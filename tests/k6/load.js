import http from 'k6/http';
import { sleep } from 'k6';
import { BASE_URL, authHeaders, debugLogin, expectStatus, getFirstProductId } from './lib/api.js';

export const options = {
  scenarios: {
    read_api_load: {
      executor: 'ramping-vus',
      stages: [
        { duration: __ENV.RAMP_UP || '30s', target: Number(__ENV.VUS || 10) },
        { duration: __ENV.HOLD || '1m', target: Number(__ENV.VUS || 10) },
        { duration: __ENV.RAMP_DOWN || '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

export function setup() {
  const buyerToken = debugLogin('CONSUMER');
  const sellerToken = debugLogin('SELLER', 'bakery@test.com');
  const productId = getFirstProductId(buyerToken);
  return { buyerToken, sellerToken, productId };
}

export default function (data) {
  const buyerHeaders = authHeaders(data.buyerToken);
  const sellerHeaders = authHeaders(data.sellerToken);

  expectStatus(
    http.get(`${BASE_URL}/api/v1/stores?latitude=37.3452&longitude=126.6878&radius=5000&page=0&size=20`, buyerHeaders),
    200,
    'list stores',
  );

  expectStatus(
    http.get(`${BASE_URL}/api/v1/products?latitude=37.3452&longitude=126.6878&radius=5000&page=0&size=20`, buyerHeaders),
    200,
    'list products',
  );

  expectStatus(
    http.get(`${BASE_URL}/api/v1/products/${data.productId}`, buyerHeaders),
    200,
    'get product detail',
  );

  expectStatus(http.get(`${BASE_URL}/api/v1/orders/my`, buyerHeaders), 200, 'get my orders');
  expectStatus(http.get(`${BASE_URL}/api/v1/sellers/orders`, sellerHeaders), 200, 'get seller orders');

  sleep(Number(__ENV.SLEEP_SECONDS || 1));
}
