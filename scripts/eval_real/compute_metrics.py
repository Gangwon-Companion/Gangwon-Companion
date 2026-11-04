"""Compute response-level grounding metrics from independent claim labels."""
from __future__ import annotations

import argparse
import csv
import json
import math
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


VALID_LABELS = {"supported", "unsupported", "contradicted"}
FORMAT_REASONS = {
    "claim_place_id_not_string",
    "claim_confidence_not_enum",
    "claim_evidence_shape_invalid",
}
GROUNDING_REASONS = {
    "claim_unknown_place_id",
    "claim_unsupported_evidence",
    "claim_evidence_value_mismatch",
}


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    with path.open(encoding="utf-8") as handle:
        return [json.loads(line) for line in handle if line.strip()]


def _read_labels(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def _wilson(successes: int, total: int) -> dict[str, Any]:
    if total == 0:
        return {"successes": 0, "total": 0, "rate": None, "wilson_95": None, "three_rule_upper_bound": None}
    p = successes / total
    z = 1.959963984540054
    denominator = 1 + z * z / total
    centre = p + z * z / (2 * total)
    margin = z * math.sqrt((p * (1 - p) + z * z / (4 * total)) / total)
    return {
        "successes": successes,
        "total": total,
        "rate": p,
        "wilson_95": [max(0.0, (centre - margin) / denominator), min(1.0, (centre + margin) / denominator)],
        "three_rule_upper_bound": 3 / total if successes == 0 else None,
    }


def _label_state(labels: list[dict[str, str]]) -> dict[tuple[str, str, str], dict[str, Any]]:
    result: dict[tuple[str, str, str], dict[str, Any]] = defaultdict(lambda: {"labels": [], "unlabeled": 0})
    for row in labels:
        key = (row.get("req_id", ""), str(row.get("run", "")), row.get("output_kind", "llm_raw"))
        label = row.get("label", "").strip().lower()
        if label in VALID_LABELS:
            result[key]["labels"].append(label)
        else:
            result[key]["unlabeled"] += 1
    return result


def _metrics(records: list[dict[str, Any]], labels: list[dict[str, str]]) -> dict[str, Any]:
    by_response = _label_state(labels)
    raw_rows: list[dict[str, Any]] = []
    final_rows: list[dict[str, Any]] = []
    for record in records:
        base_key = (str(record.get("req_id", "")), str(record.get("run", "")))
        raw_state = by_response.get((*base_key, "llm_raw"), {"labels": [], "unlabeled": 0})
        final_state = by_response.get((*base_key, "final_output"), {"labels": [], "unlabeled": 0})
        assessed = raw_state["unlabeled"] == 0 and bool(raw_state["labels"])
        violating = any(label in {"unsupported", "contradicted"} for label in raw_state["labels"])
        validator_blocked = bool(record.get("fallback_used"))
        raw_rows.append(
            {
                "key": base_key,
                "split": record.get("split"),
                "assessed": assessed,
                "violating": violating,
                "validator_blocked": validator_blocked,
                "fallback_used": validator_blocked,
                "unlabeled_claims": raw_state["unlabeled"],
                "record": record,
                "quota_excluded": record.get("llm_status") == "quota_exhausted"
                or record.get("llm_failure_reason") == "response_api_error"
                and "insufficient_quota" in str(record.get("validator", {}).get("reasons", [])),
            }
        )
        final_assessed = final_state["unlabeled"] == 0 and bool(final_state["labels"])
        final_rows.append(
            {
                "key": base_key,
                "assessed": final_assessed,
                "violating": any(label in {"unsupported", "contradicted"} for label in final_state["labels"]),
                "unlabeled_claims": final_state["unlabeled"],
            }
        )

    assessed_rows = [row for row in raw_rows if row["assessed"]]
    violation_rows = [row for row in assessed_rows if row["violating"]]
    clean_rows = [row for row in assessed_rows if not row["violating"]]
    final_assessed_rows = [row for row in final_rows if row["assessed"]]

    metrics = {
        "responses": len(records),
        "assessed_responses": len(assessed_rows),
        "final_assessed_responses": len(final_assessed_rows),
        "unassessed_responses": len(records) - len(assessed_rows),
        "unlabeled_claims": sum(row["unlabeled_claims"] for row in raw_rows) + sum(row["unlabeled_claims"] for row in final_rows),
        "raw_violation_rate": _wilson(len(violation_rows), len(assessed_rows)),
        "validator_block_rate": _wilson(sum(row["validator_blocked"] for row in violation_rows), len(violation_rows)),
        "false_block_rate": _wilson(sum(row["validator_blocked"] for row in clean_rows), len(clean_rows)),
        "final_violation_rate": _wilson(sum(row["violating"] for row in final_assessed_rows), len(final_assessed_rows)),
        "fallback_rate": _wilson(sum(row["fallback_used"] for row in assessed_rows), len(assessed_rows)),
        "format_rejection_rate": _wilson(
            sum(_has_reason(row["record"], FORMAT_REASONS) for row in raw_rows if not row["quota_excluded"]),
            sum(bool(row["record"].get("llm_called")) and not row["quota_excluded"] for row in raw_rows),
        ),
        "grounding_rejection_rate": _wilson(
            sum(_has_reason(row["record"], GROUNDING_REASONS) for row in raw_rows if not row["quota_excluded"]),
            sum(bool(row["record"].get("llm_called")) and not row["quota_excluded"] for row in raw_rows),
        ),
        "api_failure_rate": _wilson(
            sum(bool(row["record"].get("llm_failure_reason")) for row in raw_rows),
            len(raw_rows),
        ),
    }
    return {"metrics": metrics, "response_rows": raw_rows, "final_rows": final_rows}


def _has_reason(record: dict[str, Any], reasons: set[str]) -> bool:
    return bool(set(record.get("validator", {}).get("reasons", [])) & reasons)


def _format_rate(value: dict[str, Any]) -> str:
    if value["total"] == 0:
        return "자료 없음 (분모 0)"
    text = f"{value['successes']}/{value['total']} = {value['rate']:.1%}"
    low, high = value["wilson_95"]
    text += f" (Wilson 95% CI {low:.1%}–{high:.1%})"
    if value["three_rule_upper_bound"] is not None:
        text += f"; 3의 법칙 상한 {value['three_rule_upper_bound']:.1%}"
    return text


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs", type=Path, required=True)
    parser.add_argument("--labels", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, default=Path(__file__).resolve().parent / "results")
    args = parser.parse_args()
    records = _read_jsonl(args.runs)
    labels = _read_labels(args.labels)
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for record in records:
        grouped[str(record.get("split", "unknown"))].append(record)

    result: dict[str, Any] = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "runs": str(args.runs),
        "labels": str(args.labels),
        "splits": {},
    }
    sections: list[str] = ["# 실제 LLM 근거 위반율 평가", "", f"생성 시각: {result['generated_at']}", ""]
    for split, split_records in sorted(grouped.items()):
        computed = _metrics(split_records, labels)
        result["splits"][split] = computed["metrics"]
        m = computed["metrics"]
        sections.extend(
            [
                f"## {split}",
                "",
                f"- 응답: {m['responses']}건; 원문 라벨 완료: {m['assessed_responses']}건; 최종 라벨 완료: {m['final_assessed_responses']}건; 미평가: {m['unassessed_responses']}건",
                f"- 원문 위반율: {_format_rate(m['raw_violation_rate'])}",
                f"- 차단율: {_format_rate(m['validator_block_rate'])}",
                f"- 오차단율: {_format_rate(m['false_block_rate'])}",
                f"- 최종 위반율: {_format_rate(m['final_violation_rate'])}",
                f"- fallback 비율: {_format_rate(m['fallback_rate'])}",
                "",
            ]
        )
    args.output_dir.mkdir(parents=True, exist_ok=True)
    (args.output_dir / "metrics.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.output_dir / "summary.md").write_text("\n".join(sections) + "\n", encoding="utf-8")
    print(json.dumps({"metrics": str(args.output_dir / 'metrics.json'), "summary": str(args.output_dir / 'summary.md')}, ensure_ascii=False))


if __name__ == "__main__":
    main()
