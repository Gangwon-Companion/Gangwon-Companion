"""Create an independent, human-editable claim labeling CSV."""
from __future__ import annotations

import argparse
import csv
import json
import random
import re
from pathlib import Path
from typing import Any


CLAIM_COLUMNS = [
    "req_id", "split", "run", "claim_id", "claim_text", "related_places",
    "output_kind", "label", "evidence_sentence", "notes",
]
PLACE_ID = re.compile(r"\b[A-Za-z][A-Za-z0-9_]*:\d+\b")
SENTENCE = re.compile(r"(?<=[.!?。！？])\s+|(?<=다)\s+(?=[가-힣A-Za-z0-9])")


def _split_claims(text: str) -> list[str]:
    cleaned = " ".join(str(text or "").split())
    if not cleaned:
        return []
    return [part.strip() for part in SENTENCE.split(cleaned) if part.strip()]


def _records(path: Path) -> list[dict[str, Any]]:
    with path.open(encoding="utf-8") as handle:
        return [json.loads(line) for line in handle if line.strip()]


def _write_claims(records: list[dict[str, Any]], path: Path) -> int:
    count = 0
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CLAIM_COLUMNS)
        writer.writeheader()
        for record in records:
            for output_kind, field in (("llm_raw", "llm_raw"), ("final_output", "final_output")):
                claims = _split_claims(record.get(field) or "")
                for index, claim in enumerate(claims, start=1):
                    writer.writerow(
                        {
                            "req_id": record.get("req_id", ""),
                            "split": record.get("split", ""),
                            "run": record.get("run", ""),
                            "claim_id": f"{record.get('req_id', 'unknown')}-r{record.get('run', '')}-{output_kind}-c{index:03d}",
                            "claim_text": claim,
                            "related_places": ";".join(PLACE_ID.findall(claim)),
                            "output_kind": output_kind,
                            "label": "",
                            "evidence_sentence": "",
                            "notes": "",
                        }
                    )
                    count += 1
    return count


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--manual-sample", type=Path)
    parser.add_argument("--sample-size", type=int, default=30)
    parser.add_argument("--seed", type=int, default=20261102)
    args = parser.parse_args()
    records = _records(args.input)
    output = args.output or args.input.with_name(args.input.stem.replace("runs-", "labels-") + ".csv")
    output.parent.mkdir(parents=True, exist_ok=True)
    count = _write_claims(records, output)

    sample_path = args.manual_sample or output.with_name(output.stem + "-manual-sample.csv")
    unique_records: dict[str, dict[str, Any]] = {}
    for record in records:
        unique_records.setdefault(str(record.get("req_id", "")), record)
    sample_records = list(unique_records.values())
    random.Random(args.seed).shuffle(sample_records)
    sample_records = sample_records[: min(args.sample_size, len(sample_records))]
    _write_claims(sample_records, sample_path)
    print(json.dumps({"claims": count, "labels": str(output), "manual_sample": str(sample_path)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
