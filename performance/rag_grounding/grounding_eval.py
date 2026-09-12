"""Offline grounding evaluator for generated reports.

The evaluator deliberately accepts structured claims when available. For plain
text reports it extracts numbers and place IDs, then compares them with the
facts/evidence in the search trace. It does not judge prose quality or whether
the recommendation itself is commercially good.
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

NUMBER = re.compile(r"(?<![A-Za-z])[-+]?\d+(?:[,.]\d+)?%?(?![A-Za-z])")
PLACE_ID = re.compile(r"(?<![A-Z0-9_])[A-Z][A-Z_]+:\d+(?![A-Z0-9_])")


def _number(value: Any) -> str | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, (int, float)):
        return str(value).rstrip("0").rstrip(".") if isinstance(value, float) else str(value)
    if isinstance(value, str):
        match = NUMBER.fullmatch(value.strip())
        if match:
            return match.group().replace(",", "")
    return None


def _walk_values(value: Any):
    if isinstance(value, dict):
        for child in value.values():
            yield from _walk_values(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk_values(child)
    else:
        yield value


def allowed_values(snapshots: list[dict[str, Any]]) -> set[str]:
    values: set[str] = set()
    for snapshot in snapshots:
        for value in _walk_values(snapshot.get("facts", {})):
            normalized = _number(value)
            if normalized is not None:
                values.add(normalized)
        for evidence in snapshot.get("evidence", []):
            normalized = _number(evidence.get("value"))
            if normalized is not None:
                values.add(normalized)
    return values


def allowed_document_ids(snapshots: list[dict[str, Any]]) -> set[str]:
    return {snapshot["place_id"] for snapshot in snapshots if snapshot.get("place_id")}


def _claims(report: dict[str, Any]) -> list[dict[str, Any]]:
    claims = report.get("claims")
    if isinstance(claims, list):
        return [claim for claim in claims if isinstance(claim, dict)]
    return [{"text": report.get("text", "")}]


@dataclass(frozen=True)
class Evaluation:
    report_id: str
    claimed_values: int
    unsupported_values: int
    claimed_document_ids: int
    unsupported_document_ids: int
    violating_claims: int
    warning: bool

    def as_dict(self) -> dict[str, Any]:
        return {
            "report_id": self.report_id,
            "claimed_values": self.claimed_values,
            "unsupported_values": self.unsupported_values,
            "unsupported_value_rate": self.unsupported_values / self.claimed_values if self.claimed_values else 0.0,
            "claimed_document_ids": self.claimed_document_ids,
            "unsupported_document_ids": self.unsupported_document_ids,
            "unsupported_document_id_rate": self.unsupported_document_ids / self.claimed_document_ids
            if self.claimed_document_ids else 0.0,
            "violating_claims": self.violating_claims,
            "warning": self.warning,
        }


def evaluate_report(report: dict[str, Any], snapshots: list[dict[str, Any]]) -> Evaluation:
    values = allowed_values(snapshots)
    document_ids = allowed_document_ids(snapshots)
    claimed_values = unsupported_values = claimed_ids = unsupported_ids = violating = 0
    for claim in _claims(report):
        text = str(claim.get("text", ""))
        ids = claim.get("document_ids")
        ids = [str(value) for value in ids] if isinstance(ids, list) else PLACE_ID.findall(text)
        raw_values = claim.get("numbers")
        # Do not count the numeric suffix of an identifier such as RESTAURANT:101.
        text_without_ids = PLACE_ID.sub("", text)
        numbers = [str(value).replace(",", "") for value in raw_values] \
            if isinstance(raw_values, list) else NUMBER.findall(text_without_ids)
        bad = False
        for value in numbers:
            claimed_values += 1
            if value not in values:
                unsupported_values += 1
                bad = True
        for value in ids:
            claimed_ids += 1
            if value not in document_ids:
                unsupported_ids += 1
                bad = True
        violating += int(bad)
    return Evaluation(str(report.get("report_id", "unknown")), claimed_values, unsupported_values,
                       claimed_ids, unsupported_ids, violating, bool(violating))


def evaluate_pair(artifact: dict[str, Any], report: dict[str, Any]) -> dict[str, Any]:
    snapshots = artifact.get("trace", {}).get("snapshots", [])
    return evaluate_report(report, snapshots).as_dict()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, required=True, help="Search response or baseline record JSON")
    parser.add_argument("--report", type=Path, required=True, help="Generated report JSON")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = evaluate_pair(json.loads(args.artifact.read_text(encoding="utf-8")),
                           json.loads(args.report.read_text(encoding="utf-8")))
    encoded = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        args.output.write_text(encoded + "\n", encoding="utf-8")
    print(encoded)


if __name__ == "__main__":
    main()
