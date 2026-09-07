import unittest

from evaluate_suite import cases, run_suite


class GroundingEvaluationSuiteTest(unittest.TestCase):
    def test_has_sixty_cases_and_no_question_group_leakage(self):
        dataset = cases()
        self.assertEqual(len(dataset), 60)
        groups = {}
        for case in dataset:
            group = case["question_group"]
            self.assertNotIn(group, groups)
            groups[group] = case["category"]

    def test_guarded_configuration_removes_all_contract_violations(self):
        result = run_suite()
        baseline = result["summary"]["baseline"]
        guarded = result["summary"]["guarded"]
        self.assertGreater(baseline["violating_claim_rate"], 0.0)
        self.assertEqual(guarded["unsupported_value_rate"], 0.0)
        self.assertEqual(guarded["unsupported_document_id_rate"], 0.0)
        self.assertEqual(guarded["missing_citation_rate"], 0.0)
        self.assertEqual(guarded["policy_violation_rate"], 0.0)
        self.assertEqual(guarded["violating_claim_rate"], 0.0)

    def test_each_adversarial_category_is_detected(self):
        result = run_suite()
        for category in ("numeric", "document_id", "missing_citation", "policy", "cross_document"):
            self.assertGreater(result["by_category"][category]["baseline"]["violating_claim_rate"], 0.0)
            self.assertEqual(result["by_category"][category]["guarded"]["violating_claim_rate"], 0.0)


if __name__ == "__main__":
    unittest.main()
