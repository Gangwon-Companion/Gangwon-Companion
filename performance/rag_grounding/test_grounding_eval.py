import unittest
from html import escape
from pathlib import Path

from grounding_eval import evaluate_report


SNAPSHOTS = [{
    "place_id": "RESTAURANT:101",
    "facts": {"name": "바다 카페", "rating": 4.5},
    "evidence": [{"field": "pet_allowed", "value": True, "source": "TOUR_API"}],
}]


class GroundingEvaluationTest(unittest.TestCase):
    def test_measures_unsupported_values_and_document_ids(self):
        report = {"report_id": "baseline-1", "claims": [{
            "text": "평점 4.9인 RESTAURANT:999를 추천합니다.",
        }]}

        result = evaluate_report(report, SNAPSHOTS).as_dict()

        self.assertEqual(result["claimed_values"], 1)
        self.assertEqual(result["unsupported_values"], 1)
        self.assertEqual(result["unsupported_document_ids"], 1)
        self.assertEqual(result["violating_claims"], 1)
        self.assertTrue(result["warning"])

    def test_grounded_report_has_zero_hallucination_rate(self):
        report = {"report_id": "guarded-1", "claims": [{
            "text": "평점 4.5인 RESTAURANT:101을 추천합니다.",
        }]}

        result = evaluate_report(report, SNAPSHOTS).as_dict()

        self.assertEqual(result["unsupported_values"], 0)
        self.assertEqual(result["unsupported_document_ids"], 0)
        self.assertEqual(result["unsupported_value_rate"], 0.0)
        self.assertEqual(result["unsupported_document_id_rate"], 0.0)
        self.assertFalse(result["warning"])

    def test_structured_numbers_are_preferred_for_localized_text(self):
        report = {"claims": [{
            "text": "영업시간을 근거와 함께 설명합니다.",
            "numbers": [4.5],
            "document_ids": ["RESTAURANT:101"],
        }]}

        result = evaluate_report(report, SNAPSHOTS).as_dict()

        self.assertEqual(result["violating_claims"], 0)


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(GroundingEvaluationTest)
    result = unittest.TextTestRunner(verbosity=2).run(suite)

    baseline = evaluate_report({"report_id": "baseline-example", "claims": [{
        "text": "평점 4.9인 RESTAURANT:999를 추천합니다.",
    }]}, SNAPSHOTS).as_dict()
    guarded = evaluate_report({"report_id": "guarded-example", "claims": [{
        "text": "평점 4.5인 RESTAURANT:101을 추천합니다.",
    }]}, SNAPSHOTS).as_dict()
    rows = []
    for label, metrics in (("Baseline", baseline), ("Guarded", guarded)):
        rows.append("<tr><td>{}</td><td>{}</td><td>{}</td><td>{:.1%}</td>"
                    "<td>{}</td><td>{}</td><td>{:.1%}</td><td>{}</td></tr>".format(
                        label, metrics["claimed_values"], metrics["unsupported_values"],
                        metrics["unsupported_value_rate"], metrics["claimed_document_ids"],
                        metrics["unsupported_document_ids"], metrics["unsupported_document_id_rate"],
                        "경고" if metrics["warning"] else "통과"))
    html = """<!doctype html>
<html lang="ko"><meta charset="utf-8"><title>RAG Grounding Evaluation</title>
<style>body{{font-family:system-ui,sans-serif;margin:40px;color:#202124}}table{{border-collapse:collapse}}th,td{{border:1px solid #bbb;padding:8px 12px}}th{{background:#eef3f8}}.ok{{color:green}}.warn{{color:#a15c00}}</style>
<h1>RAG Grounding Evaluation</h1>
<p>허용된 grounding snapshot과 생성 리포트의 수치·문서 ID를 비교한 오프라인 평가 결과입니다.</p>
<table><thead><tr><th>구성</th><th>수치 주장</th><th>허용되지 않은 수치</th><th>수치 위반률</th>
<th>문서 ID 주장</th><th>허용되지 않은 문서 ID</th><th>문서 ID 위반률</th><th>처리</th></tr></thead>
<tbody>{}</tbody></table>
<h2>해석</h2><ul><li>Baseline: 4.9와 RESTAURANT:999가 whitelist에 없어 경고가 발생합니다.</li>
<li>Guarded: grounding snapshot에 존재하는 4.5와 RESTAURANT:101만 포함해 위반률이 0%입니다.</li></ul>
</html>""".format("".join(rows))
    output = Path(__file__).with_name("grounding-evaluation.html")
    output.write_text(html, encoding="utf-8")
    print(f"HTML report: {output}")
    raise SystemExit(0 if result.wasSuccessful() else 1)
