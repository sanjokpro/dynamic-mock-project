import { test, expect } from '@playwright/test';

test('backend health endpoint is UP', async ({ request, baseURL }) => {
  const res = await request.get('/actuator/health');
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  expect(body.status).toBe('UP');
});

