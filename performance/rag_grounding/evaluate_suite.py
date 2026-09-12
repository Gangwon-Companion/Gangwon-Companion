"""Reproducible, offline hallucination evaluation suite.

This suite evaluates structured claims against frozen grounding snapshots. It
is intentionally model-agnostic: an adapter can replace ``make_report`` with
real LLM/LangGraph output while keeping the scoring and HTML report stable.
"""
from __future__ import annotations

import argparse
import html
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

from grounding_eval import evaluate_report


def cases() -> list[dict[str, Any]]:
    result = []
    categories = ("numeric", "document_id", "missing_citation", "policy", "cross_document", "clean")
    for index in range(60):
        category = categories[index % len(categories)]
        place_id = f"RESTAURANT:{1000 + index}"
        other_id = f"RESTAURANT:{9000 + index}"
        snapshot = {"place_id": place_id, "facts": {"name": f"매장 {index}", "rating": 4.2,
                     "price": 12000, "distance_km": 2.4},
                    "evidence": [{"field": "pet_allowed", "value": True, "source": "TOUR_API"}],
                    "missing_fields": []}
        result.append({"id": f"case-{index + 1:03d}", "category": category,
                       "question_group": f"group-{index:03d}", "snapshots": [snapshot],
                       "other_id": other_id})
    return result


def make_report(case: dict[str, Any], guarded: bool) -> dict[str, Any]:
    sid = case["snapshots"][0]["place_id"]
    category = case["category"]
    if guarded or category == "clean":
        return {"report_id": f"{'guarded' if guarded else 'baseline'}-{case['id']}", "claims": [{
            "text": f"평점 4.2인 {sid}를 추천합니다.", "numbers": [4.2], "document_ids": [sid]
        }]}
    if category == "numeric":
        return {"report_id": f"baseline-{case['id']}", "claims": [{
            "text": f"평점 4.9인 {sid}를 추천합니다.", "numbers": [4.9], "document_ids": [sid]
        }]}
    if category == "document_id":
        return {"report_id": f"baseline-{case['id']}", "claims": [{
            "text": f"{case['other_id']}를 추천합니다.", "document_ids": [case["other_id"]]
        }]}
    if category == "missing_citation":
        return {"report_id": f"baseline-{case['id']}", "claims": [{
            "text": "평점 4.2인 매장을 추천합니다.", "numbers": [4.2]
        }]}
    if category == "policy":
        return {"report_id": f"baseline-{case['id']}", "claims": [{
            "text": f"반려동물 출입이 불가한 {sid}입니다.",
            "numbers": [], "document_ids": [sid], "policy": {"pet_allowed": False}
        }]}
    return {"report_id": f"baseline-{case['id']}", "claims": [{
        "text": f"{sid}의 평점은 4.2입니다.", "numbers": [4.2], "document_ids": [case["other_id"]]
    }]}


def score(case: dict[str, Any], report: dict[str, Any]) -> dict[str, Any]:
    base = evaluate_report(report, case["snapshots"]).as_dict()
    claims = report.get("claims", [])
    missing_citation = sum(1 for claim in claims if claim.get("numbers") and not claim.get("document_ids"))
    policy_violations = 0
    for claim in claims:
        policy = claim.get("policy", {})
        if policy.get("pet_allowed") is False:
            allowed = any(e.get("field") == "pet_allowed" and e.get("value") is True
                          for s in case["snapshots"] for e in s.get("evidence", []))
            policy_violations += int(allowed)
    violating = base["violating_claims"] + missing_citation + policy_violations
    base.update(missing_citations=missing_citation, policy_violations=policy_violations,
                violating_claims=violating, warning=bool(violating), blocked=False)
    return base


def run_suite() -> dict[str, Any]:
    all_cases = cases()
    runs = []
    for case in all_cases:
        for configuration in ("baseline", "guarded"):
            report = make_report(case, configuration == "guarded")
            metrics = score(case, report)
            runs.append({"case_id": case["id"], "category": case["category"],
                         "configuration": configuration, **metrics})
    summary = {}
    for configuration in ("baseline", "guarded"):
        selected = [r for r in runs if r["configuration"] == configuration]
        summary[configuration] = aggregate(selected)
    by_category = {}
    for category in sorted({case["category"] for case in all_cases}):
        by_category[category] = {configuration: aggregate(
            [r for r in runs if r["category"] == category and r["configuration"] == configuration])
            for configuration in ("baseline", "guarded")}
    return {"schema_version": 1, "evaluation": "offline_grounding_contract",
            "note": "Synthetic deterministic cases; not a production LLM quality score.",
            "case_count": len(all_cases), "configurations": ["baseline", "guarded"],
            "summary": summary, "by_category": by_category, "runs": runs}


def aggregate(rows: list[dict[str, Any]]) -> dict[str, Any]:
    total = len(rows)
    def rate(field: str) -> float:
        return sum(row[field] for row in rows) / total if total else 0.0
    return {"cases": total, "unsupported_value_rate": rate("unsupported_value_rate"),
            "unsupported_document_id_rate": rate("unsupported_document_id_rate"),
            "missing_citation_rate": rate("missing_citations"),
            "policy_violation_rate": rate("policy_violations"),
            "violating_claim_rate": rate("violating_claims"), "warning_rate": rate("warning")}


def write_html(result: dict[str, Any], path: Path) -> None:
    summary_rows = "".join(
        f"<tr><td>{html.escape(configuration)}</td><td>{metrics['cases']}</td>"
        f"<td>{metrics['unsupported_value_rate']:.1%}</td>"
        f"<td>{metrics['unsupported_document_id_rate']:.1%}</td>"
        f"<td>{metrics['missing_citation_rate']:.1%}</td>"
        f"<td>{metrics['policy_violation_rate']:.1%}</td>"
        f"<td>{metrics['violating_claim_rate']:.1%}</td></tr>"
        for configuration, metrics in result["summary"].items())
    category_rows = "".join(
        f"<tr><td>{html.escape(category)}</td>"
        f"<td>{values['baseline']['violating_claim_rate']:.1%}</td>"
        f"<td>{values['guarded']['violating_claim_rate']:.1%}</td></tr>"
        for category, values in result["by_category"].items())
    document = f'''<!doctype html><html lang="ko"><meta charset="utf-8">
<title>Grounding Evaluation Suite</title><style>
body{{font-family:system-ui,sans-serif;margin:36px;color:#202124}}table{{border-collapse:collapse;margin:12px 0 28px}}
th,td{{border:1px solid #bbb;padding:8px 12px}}th{{background:#eef3f8}}
</style><h1>RAG Grounding Evaluation Suite</h1>
<p><strong>{result['case_count']}개 케이스</strong>. 숫자, 문서 ID, 출처 누락, 정책 반전, 문서 간 혼합을 평가합니다.</p>
<p><strong>주의:</strong> synthetic deterministic cases이며 실제 LLM 품질 점수가 아닙니다.</p>
<h2>전체 결과</h2><table><tr><th>구성</th><th>케이스</th><th>수치 위반률</th>
<th>문서 ID 위반률</th><th>출처 누락률</th><th>정책 위반률</th><th>claim 위반률</th></tr>{summary_rows}</table>
<h2>카테고리별 claim 위반률</h2><table><tr><th>카테고리</th><th>Baseline</th><th>Guarded</th></tr>{category_rows}</table>
</html>'''
    path.write_text(document, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path(__file__).with_name("grounding-suite.html"))
    args = parser.parse_args()
    result = run_suite()
    write_html(result, args.output)
    print(json.dumps({"case_count": result["case_count"], "summary": result["summary"],
                      "html": str(args.output)}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
