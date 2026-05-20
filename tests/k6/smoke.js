import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authHeaders, debugLogin, expectStatus, getFirstProductId, jsonHeaders, parseJson } from './lib/api.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750'],
  },
};

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, {
    'health endpoint is up': (res) => res.status === 200 && res.body.includes('UP'),
  });

  const buyerToken = debugLogin('CONSUMER');
  const sellerToken = debugLogin('SELLER', 'bakery@test.com');

  const stores = http.get(
    `${BASE_URL}/api/v1/stores?latitude=37.3452&longitude=126.6878&radius=5000&page=0&size=20`,
    authHeaders(buyerToken),
  );
  expectStatus(stores, 200, 'list stores');

  const productId = getFirstProductId(buyerToken);

  const productDetail = http.get(`${BASE_URL}/api/v1/products/${productId}`, authHeaders(buyerToken));
  expectStatus(productDetail, 200, 'get product detail');

  const addCart = http.post(
    `${BASE_URL}/api/v1/cart`,
    JSON.stringify({ productId, quantity: 1 }),
    jsonHeaders(buyerToken),
  );
  expectStatus(addCart, 201, 'add to cart');

  const cart = http.get(`${BASE_URL}/api/v1/cart`, authHeaders(buyerToken));
  expectStatus(cart, 200, 'get cart');

  const order = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({ items: [{ productId, quantity: 1 }], paymentMethod: 'CASH' }),
    jsonHeaders(buyerToken),
  );
  expectStatus(order, 201, 'create order');

  const orderBody = parseJson(order, 'create order');
  const orderId = orderBody && orderBody.data && orderBody.data.orderId;
  check(orderBody, {
    'create order returns orderId': () => Number.isInteger(orderId),
  });

  const myOrders = http.get(`${BASE_URL}/api/v1/orders/my`, authHeaders(buyerToken));
  expectStatus(myOrders, 200, 'get my orders');

  if (orderId) {
    const orderDetail = http.get(`${BASE_URL}/api/v1/orders/${orderId}`, authHeaders(buyerToken));
    expectStatus(orderDetail, 200, 'get order detail');

    const cancel = http.patch(`${BASE_URL}/api/v1/orders/${orderId}/cancel`, null, authHeaders(buyerToken));
    expectStatus(cancel, 200, 'cancel order');
  }

  const clearCart = http.del(`${BASE_URL}/api/v1/cart`, null, authHeaders(buyerToken));
  expectStatus(clearCart, 200, 'clear cart');

  const sellerOrders = http.get(`${BASE_URL}/api/v1/sellers/orders`, authHeaders(sellerToken));
  expectStatus(sellerOrders, 200, 'get seller orders');

  sleep(1);
}
