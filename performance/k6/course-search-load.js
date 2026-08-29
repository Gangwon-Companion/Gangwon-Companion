import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const runId = __ENV.TEST_RUN_ID || 'course-search-manual';
const peakVus = Number(__ENV.PEAK_VUS || 20);

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    ocean_course_search: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.WARMUP || '5s', target: Math.max(1, Math.round(peakVus * 0.25)) },
        { duration: __ENV.RAMP || '10s', target: peakVus },
        { duration: __ENV.HOLD || '30s', target: peakVus },
        { duration: __ENV.COOLDOWN || '5s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
  tags: { test_run_id: runId },
};

// The same candidate searches used by the representative 2-night/3-day ocean trip.
const searchBodies = [
  {
    domain: 'DESTINATION', slot: 'D1_DESTINATION', query_text: '바다',
    soft_preferences: { oceanView: 1.0 }, limit: 24,
  },
  {
    domain: 'RESTAURANT', slot: 'D1_LUNCH', query_text: '', limit: 24,
  },
  {
    domain: 'LODGING', slot: 'D1_LODGING', query_text: '바다',
    soft_preferences: { oceanView: 1.0 }, limit: 10,
  },
];

export default function () {
  const body = searchBodies[Math.floor(Math.random() * searchBodies.length)];
  const response = http.post(`${baseUrl}/internal/search/places`, JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'course-place-search', domain: body.domain },
  });

  check(response, {
    'search returned 200': (r) => r.status === 200,
    'search response is JSON': (r) => {
      try { return Array.isArray(r.json('results')); } catch (_) { return false; }
    },
  });
  sleep(Number(__ENV.THINK_TIME || 0.1));
}

function metricValue(data, name, key) {
  return data.metrics[name] && data.metrics[name].values[key] !== undefined
    ? data.metrics[name].values[key]
    : null;
}

export function handleSummary(data) {
  const result = {
    runId,
    generatedAt: new Date().toISOString(),
    workload: '2-night/3-day ocean course candidate search only',
    peakVus,
    p95Ms: metricValue(data, 'http_req_duration', 'p(95)'),
    p99Ms: metricValue(data, 'http_req_duration', 'p(99)'),
    requests: metricValue(data, 'http_reqs', 'count'),
    throughputRps: metricValue(data, 'http_reqs', 'rate'),
    errorRate: metricValue(data, 'http_req_failed', 'rate'),
    checkRate: metricValue(data, 'checks', 'rate'),
    durationMs: data.state.testRunDurationMs,
  };
  return {
    stdout: `run=${runId} p95=${result.p95Ms}ms throughput=${result.throughputRps}req/s errors=${result.errorRate}\n`,
    [`/results/${runId}.json`]: JSON.stringify(result, null, 2),
  };
}
