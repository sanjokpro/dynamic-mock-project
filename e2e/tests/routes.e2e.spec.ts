import { test, expect } from '@playwright/test';

test('create and serve a REST route', async ({ request }) => {
  const unique = `e2e-${Date.now()}`;
  const path = `/${unique}`;

  // Create route
  const createRes = await request.post('/api/routes', {
    data: {
      path,
      method: 'GET',
      responseTemplate: '{"message":"ok"}',
      responseStatus: 200,
      active: true,
    },
  });
  expect(createRes.status()).toBe(201);
  const created = await createRes.json();
  expect(created.id).toBeTruthy();

  // Activate route (in case service defaults to inactive)
  await request.post(`/api/routes/${created.id}/activate`);

  // Hit the mock endpoint
  const mockRes = await request.get(`/mock${path}`);
  expect(mockRes.status()).toBe(200);
  const payload = await mockRes.json();
  expect(payload.message).toBe('ok');

  // Cleanup
  await request.delete(`/api/routes/${created.id}`);
});

