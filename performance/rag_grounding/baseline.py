"""Capture baseline search responses or replay saved artifacts without a network."""
import argparse
import hashlib
import json
from pathlib import Path
import random
import time
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True,
                                     separators=(",", ":")).encode()).hexdigest()


def validate_cases(cases):
    seen = set()
    groups = {}
    for case in cases:
        if case["id"] in seen:
            raise ValueError("Duplicate case id")
        seen.add(case["id"])
        if case["split"] not in ("tuning", "test"):
            raise ValueError("split must be tuning or test")
        group = case["question_group"]
        if group in groups and groups[group] != case["split"]:
            raise ValueError("Question group leaks across tuning/test")
        groups[group] = case["split"]
        if not isinstance(case["request"], dict):
            raise ValueError("request must be an object")
    if not cases:
        raise ValueError("Empty dataset")


def capture(cases, endpoint, snapshot_id, revision, repeats, seed, timeout):
    validate_cases(cases)
    jobs = [(case, repeat) for case in cases for repeat in range(repeats)]
    random.Random(seed).shuffle(jobs)
    records = []
    for case, repeat in jobs:
        record = {"case_id": case["id"], "repeat": repeat, "request": case["request"]}
        started = time.perf_counter()
        try:
            request = Request(endpoint, data=json.dumps(case["request"]).encode(),
                              headers={"Content-Type": "application/json"}, method="POST")
            with urlopen(request, timeout=timeout) as result:
                response = json.load(result)
            if not response.get("trace") or "snapshots" not in response["trace"]:
                raise ValueError("Server response lacks grounding trace")
            record.update(status="ok", response=response, response_hash=digest(response))
        except (HTTPError, URLError, TimeoutError, ValueError, OSError) as error:
            # Do not persist URLs, headers, credentials or arbitrary server error bodies.
            record.update(status="failed", error_type=type(error).__name__)
        record["latency_ms"] = round((time.perf_counter() - started) * 1000, 3)
        records.append(record)
    return {"schema_version": 1, "configuration": "A", "scope": "search_baseline",
            "created_at": datetime.now(timezone.utc).isoformat(),
            "source_snapshot_id": snapshot_id, "code_revision": revision,
            "dataset_hash": digest(cases), "cases": cases, "seed": seed,
            "repeats": repeats, "cache_policy": "uncontrolled; no warmup",
            "records": records}


def replay(artifact):
    if artifact["schema_version"] != 1 or artifact["configuration"] != "A":
        raise ValueError("Unsupported artifact")
    cases = artifact["cases"]
    validate_cases(cases)
    if digest(cases) != artifact["dataset_hash"]:
        raise ValueError("Dataset hash mismatch")
    expected = {(case["id"], repeat): case["request"]
                for case in cases for repeat in range(artifact["repeats"])}
    paths = {}
    failed = 0
    for record in artifact["records"]:
        key = (record["case_id"], record["repeat"])
        if key not in expected or expected.pop(key) != record["request"]:
            raise ValueError("Unexpected, duplicate or changed request")
        if record["status"] == "failed":
            failed += 1
            continue
        if record["status"] != "ok" or digest(record["response"]) != record["response_hash"]:
            raise ValueError("Response hash mismatch")
        trace = record["response"]["trace"]
        if [x["place_id"] for x in trace["snapshots"]] != [x["place_id"] for x in record["response"]["results"]]:
            raise ValueError("Candidate/snapshot mismatch")
        path = trace["engine"] + ":" + trace["path"]
        paths[path] = paths.get(path, 0) + 1
    if expected:
        raise ValueError("Missing requests")
    return {"requests": len(artifact["records"]), "failed": failed, "actual_paths": paths,
            "scope": "artifact integrity only; not a hallucination or relevance evaluation"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    record = commands.add_parser("capture")
    record.add_argument("--cases", type=Path, required=True)
    record.add_argument("--output", type=Path, required=True)
    record.add_argument("--endpoint", default="http://localhost:8080/internal/search/places")
    record.add_argument("--source-snapshot-id", required=True)
    record.add_argument("--code-revision", required=True)
    record.add_argument("--repeats", type=int, default=1)
    record.add_argument("--seed", type=int, default=42)
    record.add_argument("--timeout", type=float, default=30)
    check = commands.add_parser("replay")
    check.add_argument("artifact", type=Path)
    args = parser.parse_args()
    if args.command == "capture":
        if args.repeats < 1 or args.timeout <= 0:
            parser.error("repeats and timeout must be positive")
        cases = json.loads(args.cases.read_text(encoding="utf-8"))
        # Refuse to overwrite an earlier baseline.
        with args.output.open("x", encoding="utf-8") as output:
            artifact = capture(cases, args.endpoint, args.source_snapshot_id, args.code_revision,
                               args.repeats, args.seed, args.timeout)
            json.dump(artifact, output, ensure_ascii=False, indent=2)
    else:
        artifact = json.loads(args.artifact.read_text(encoding="utf-8"))
    print(json.dumps(replay(artifact), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
