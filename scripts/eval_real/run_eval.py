"""Run the real travel graph and capture GPT response grounding observations."""
from __future__ import annotations

import argparse
import json
import os
import sys
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from time import perf_counter
from typing import Any, Iterator

from dotenv import load_dotenv


HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parents[1]
AI_ROOT = REPO_ROOT.parent / "Gangwon-AI"
DEFAULT_RESULTS = HERE / "results"


def _read_requests(split: str) -> list[dict[str, Any]]:
    paths = {
        "dev": [HERE / "requests" / "dev.jsonl"],
        "holdout": [HERE / "requests" / "holdout.jsonl"],
        "all": [HERE / "requests" / "dev.jsonl", HERE / "requests" / "holdout.jsonl"],
    }
    rows: list[dict[str, Any]] = []
    for path in paths[split]:
        with path.open(encoding="utf-8") as handle:
            for line in handle:
                if line.strip():
                    row = json.loads(line)
                    if row.get("split") != split and split != "all":
                        raise ValueError(f"split mismatch in {path}: {row.get('split')}")
                    rows.append(row)
    return rows


def _json_default(value: Any) -> Any:
    if hasattr(value, "model_dump"):
        return value.model_dump()
    if hasattr(value, "isoformat"):
        return value.isoformat()
    raise TypeError(f"not JSON serializable: {type(value)!r}")


class Capture:
    def __init__(self) -> None:
        self.model: str | None = None
        self.llm_raw: str | None = None
        self.llm_input: str | None = None
        self.instructions: str | None = None
        self.validator_events: list[dict[str, Any]] = []
        self.llm_error: str | None = None
        self.llm_failure_reason: str | None = None
        self.llm_latency_ms: float | None = None
        self.schema_place_ids: list[str] = []
        self.fallback_answer: str | None = None
        self.context_docs: list[dict[str, Any]] = []
        self.retrieved_context: dict[str, Any] = {}
        self.llm_called = False


class CapturingClient:
    def __init__(self, delegate: Any, capture: Capture) -> None:
        self.delegate = delegate
        self.capture = capture

    def create_answer(
        self, *, model: str, instructions: str, input_text: str,
        response_schema: dict[str, Any] | None = None,
    ) -> str:
        self.capture.model = model
        self.capture.instructions = instructions
        self.capture.llm_input = input_text
        try:
            parsed_input = json.loads(input_text)
            retrieved_context = parsed_input.get("retrieved_context", {})
            if isinstance(retrieved_context, dict):
                self.capture.retrieved_context = retrieved_context
        except (TypeError, json.JSONDecodeError):
            self.capture.retrieved_context = {}
        self.capture.llm_called = True
        started = perf_counter()
        try:
            raw = self.delegate.create_answer(
                model=model,
                instructions=instructions,
                input_text=input_text,
                response_schema=response_schema,
            )
        except Exception as exc:
            self.capture.llm_error = f"{exc.__class__.__name__}: {exc}"
            self.capture.llm_failure_reason = getattr(exc, "reason", None)
            self.capture.llm_latency_ms = round((perf_counter() - started) * 1000, 2)
            raise
        self.capture.llm_latency_ms = round((perf_counter() - started) * 1000, 2)
        self.capture.llm_raw = raw
        return raw


@contextmanager
def _capture_response(capture: Capture) -> Iterator[None]:
    if str(AI_ROOT) not in sys.path:
        sys.path.insert(0, str(AI_ROOT))
    from app.agents import response as response_module
    from app.agents import response_llm

    original_render = response_module.render_answer_with_llm
    original_claims = response_llm.validate_claims
    original_final = response_llm.validate_final_answer

    def wrapped_claims(claims: Any, *, days: Any, accommodations: Any = None):
        result = original_claims(claims, days=days, accommodations=accommodations)
        capture.validator_events.append({"validator": "claims", "pass": result[0], "reason": result[1]})
        return result

    def wrapped_final(answer: str, *, days: Any, notices: Any, accommodations: Any = None):
        result = original_final(answer, days=days, notices=notices, accommodations=accommodations)
        capture.validator_events.append({"validator": "final_answer", "pass": result[0], "reason": result[1]})
        return result

    def wrapped_render(**kwargs: Any) -> str:
        capture.fallback_answer = kwargs.get("fallback_answer")
        capture.context_docs = [
            *kwargs.get("days", []),
            *kwargs.get("accommodations", []),
        ]
        delegate = response_llm.OpenAIResponsesClient()
        return original_render(**kwargs, client=CapturingClient(delegate, capture))

    response_module.render_answer_with_llm = wrapped_render
    response_llm.validate_claims = wrapped_claims
    response_llm.validate_final_answer = wrapped_final
    try:
        yield
    finally:
        response_module.render_answer_with_llm = original_render
        response_llm.validate_claims = original_claims
        response_llm.validate_final_answer = original_final


def _run_one(request: dict[str, Any]) -> dict[str, Any]:
    load_dotenv(AI_ROOT / ".env")
    if str(AI_ROOT) not in sys.path:
        sys.path.insert(0, str(AI_ROOT))
    from app.graph import travel_graph

    capture = Capture()
    effective_request = dict(request["request"])
    # The existing form binder treats these optional policy flags as required
    # clarification inputs. Explicit false means "no such constraint" and
    # keeps the evaluation request complete without changing the graph.
    effective_request.setdefault("pet_allowed", False)
    effective_request.setdefault("wheelchair_accessible", False)
    started = datetime.now(timezone.utc).isoformat()
    result: dict[str, Any] = {
        "req_id": request["req_id"],
        "split": request["split"],
        "run": request.get("run"),
        "request": {**request, "request": effective_request},
        "context_docs": [],
        "llm_raw": None,
        "validator": {"pass": None, "reasons": [], "events": []},
        "final_output": None,
        "model": os.getenv("GANGWON_RESPONSE_LLM_MODEL", "gpt-4.1"),
        "timestamp": started,
        "llm_called": False,
        "fallback_used": False,
        "llm_status": "not_called",
        "llm_failure_reason": None,
        "llm_latency_ms": None,
        "pipeline_error": None,
    }
    try:
        with _capture_response(capture):
            state = travel_graph.invoke({"request": effective_request})
        final_response = state.get("final_response") or {}
        result.update(
            {
                "context_docs": capture.context_docs,
                "retrieved_context": capture.retrieved_context,
                "retrieved_document_count": len(capture.retrieved_context.get("documents", [])),
                "retrieved_source_ids": [
                    source_id
                    for document in capture.retrieved_context.get("documents", [])
                    if isinstance(document, dict)
                    for source_id in (document.get("evidence", {}).get("source_ids", [])
                                      if isinstance(document.get("evidence"), dict) else [])
                ],
                "llm_raw": capture.llm_raw,
                "final_output": final_response.get("answer"),
                "final_response_status": final_response.get("response_status"),
                "final_notices": final_response.get("notices", []),
                "final_source_ids": final_response.get("source_ids", []),
                "model": capture.model or result["model"],
                "llm_called": capture.llm_called,
                "llm_failure_reason": capture.llm_failure_reason,
                "llm_latency_ms": capture.llm_latency_ms,
                "fallback_used": bool(
                    capture.llm_called
                    and not capture.llm_failure_reason == "response_api_error"
                    and (capture.llm_error or any(not item["pass"] for item in capture.validator_events))
                ),
                "llm_status": _llm_status(capture),
                "llm_input": capture.llm_input,
                "validator": {
                    "pass": bool(capture.validator_events)
                    and all(item["pass"] for item in capture.validator_events),
                    "reasons": [item["reason"] for item in capture.validator_events if not item["pass"]],
                    "events": capture.validator_events,
                },
            }
        )
        if capture.llm_error:
            result["validator"] = {
                "pass": False,
                "reasons": ["llm_error", capture.llm_error],
                "events": capture.validator_events,
            }
    except Exception as exc:
        result["pipeline_error"] = f"{exc.__class__.__name__}: {exc}"
    return result


def _llm_status(capture: Capture) -> str:
    if not capture.llm_called:
        return "not_called"
    if capture.llm_failure_reason == "response_api_error":
        if capture.llm_error and "insufficient_quota" in capture.llm_error:
            return "quota_exhausted"
        return "api_error"
    if capture.llm_error:
        return "llm_error"
    if any(not item["pass"] for item in capture.validator_events):
        return "grounding_rejected"
    return "passed"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--split", choices=("dev", "holdout", "all"), default="dev")
    parser.add_argument("--runs", type=int, default=3)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_RESULTS)
    args = parser.parse_args()
    if args.runs < 1:
        raise SystemExit("--runs must be positive")
    requests = _read_requests(args.split)
    if args.limit is not None:
        requests = requests[: max(0, args.limit)]
    planned = len(requests) * args.runs
    if args.dry_run:
        print(json.dumps({"split": args.split, "requests": len(requests), "runs": args.runs, "llm_calls": planned}, ensure_ascii=False))
        return

    args.output_dir.mkdir(parents=True, exist_ok=True)
    output_name = f"runs-{args.split}.jsonl"
    output_path = args.output_dir / output_name
    count = 0
    with output_path.open("w", encoding="utf-8", newline="") as handle:
        for request in requests:
            for run in range(1, args.runs + 1):
                item_request = dict(request)
                item_request["run"] = run
                record = _run_one(item_request)
                handle.write(json.dumps(record, ensure_ascii=False, default=_json_default) + "\n")
                handle.flush()
                count += 1
                print(f"[{count}/{planned}] {request['req_id']} run={run}")
    print(json.dumps({"output": str(output_path), "records": count, "planned_llm_calls": planned}, ensure_ascii=False))


if __name__ == "__main__":
    main()
