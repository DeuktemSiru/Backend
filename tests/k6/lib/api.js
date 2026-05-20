import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

const JSON_HEADERS = {
  'Content-Type': 'application/json',
};

export function jsonHeaders(token) {
  return token
    ? { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } }
    : { headers: JSON_HEADERS };
}

export function authHeaders(token) {
  return { headers: { Authorization: `Bearer ${token}` } };
}

export function parseJson(res, label) {
  try {
    return res.json();
  } catch (error) {
    fail(`${label} did not return JSON: ${error}`);
  }
}

export function expectStatus(res, expected, label) {
  check(res, {
    [`${label} status is ${expected}`]: (r) => r.status === expected,
    [`${label} returns ApiResponse envelope`]: (r) => {
      const contentType = r.headers['Content-Type'] || '';
      if (!contentType.includes('application/json')) return true;
      const body = parseJson(r, label);
      return body && Object.prototype.hasOwnProperty.call(body, 'code') && Object.prototype.hasOwnProperty.call(body, 'message');
    },
  });
}

export function debugLogin(role, email) {
  const payload = email ? { role, email } : { role };
  const res = http.post(`${BASE_URL}/api/v1/auth/debug/login`, JSON.stringify(payload), jsonHeaders());
  expectStatus(res, 200, `${role.toLowerCase()} debug login`);

  const body = parseJson(res, `${role.toLowerCase()} debug login`);
  const token = body && body.data && body.data.accessToken;
  if (!token) {
    fail(`${role.toLowerCase()} debug login did not return data.accessToken`);
  }
  return token;
}

export function getFirstProductId(token) {
  const res = http.get(
    `${BASE_URL}/api/v1/products?latitude=37.3452&longitude=126.6878&radius=5000&page=0&size=20`,
    authHeaders(token),
  );
  expectStatus(res, 200, 'list products');

  const body = parseJson(res, 'list products');
  const products = body && body.data && body.data.products;
  if (!Array.isArray(products) || products.length === 0) {
    fail('list products did not return any seeded products');
  }
  return products[0].productId;
}
