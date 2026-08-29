import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const runId = __ENV.TEST_RUN_ID || 'manual';
const peakVus = Number(__ENV.PEAK_VUS || 100);

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    mixed_read_search: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.WARMUP || '30s', target: Math.max(1, Math.round(peakVus * 0.2)) },
        { duration: __ENV.RAMP || '1m', target: peakVus },
        { duration: __ENV.HOLD || '3m', target: peakVus },
        { duration: __ENV.COOLDOWN || '30s', target: 0 },
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
  tags: { test_run_id: runId },
};

const searchBodies = [
  { domain: 'DESTINATION', slot: 'load-test', query_text: '강릉', region_codes: ['GANGNEUNG'], limit: 10 },
  { domain: 'RESTAURANT', slot: 'load-test', query_text: '반려견', hard_filters: { pet_allowed: true }, limit: 10 },
  { domain: 'LODGING', slot: 'load-test', query_text: '숙소', region_codes: ['SOKCHO'], limit: 10 },
];

const reads = [
  '/api/v1/activities?page=0&size=20',
  '/api/v1/lodgings?page=0&size=20',
  '/api/v1/restaurants?page=0&size=20',
  '/api/v1/promotions/hotplace',
  '/api/v1/community/posts?page=0&size=20',
];

export default function () {
  const doSearch = Math.random() < 0.4;
  let response;
  if (doSearch) {
    const body = searchBodies[Math.floor(Math.random() * searchBodies.length)];
    response = http.post(`${baseUrl}/internal/search/places`, JSON.stringify(body), {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'place-search' },
    });
  } else {
    const path = reads[Math.floor(Math.random() * reads.length)];
    response = http.get(`${baseUrl}${path}`, { tags: { endpoint: path.split('?')[0] } });
  }

  check(response, {
    'status is successful': (r) => r.status >= 200 && r.status < 400,
  });
  sleep(Number(__ENV.THINK_TIME || 0.2));
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
    p95Ms: metricValue(data, 'http_req_duration', 'p(95)'),
    p99Ms: metricValue(data, 'http_req_duration', 'p(99)'),
    requests: metricValue(data, 'http_reqs', 'count'),
    throughputRps: metricValue(data, 'http_reqs', 'rate'),
    errorRate: metricValue(data, 'http_req_failed', 'rate'),
    checkRate: metricValue(data, 'checks', 'rate'),
    durationMs: data.state.testRunDurationMs,
  };
  const line = `run=${runId} p95=${result.p95Ms}ms throughput=${result.throughputRps}req/s errors=${result.errorRate}\n`;
  return {
    stdout: line,
    [`/results/${runId}.json`]: JSON.stringify(result, null, 2),
  };
}
