import { test, expect } from '@playwright/test';

test('route versioning lifecycle', async ({ request }) => {
  const unique = `ver-${Date.now()}`;
  const path = `/${unique}`;

  // Create route v1
  const createRes = await request.post('/api/routes', {
    data: {
      path,
      method: 'GET',
      responseTemplate: '{"v":"1"}',
      responseStatus: 200,
      active: true,
    },
  });
  expect(createRes.status()).toBe(201);
  const route = await createRes.json();

  // Update to v2
  const updateRes = await request.put(`/api/routes/${route.id}`, {
    data: {
      responseTemplate: '{"v":"2"}',
      responseStatus: 200,
    },
  });
  expect(updateRes.ok()).toBeTruthy();

  // Fetch versions list
  const versionsRes = await request.get(`/api/routes/${route.id}/versions`);
  expect(versionsRes.ok()).toBeTruthy();
  const versions = await versionsRes.json();
  expect(versions.length).toBeGreaterThanOrEqual(2);

  // Roll back to version 1
  const rollbackRes = await request.post(
    `/api/routes/${route.id}/versions/1/rollback`
  );
  expect(rollbackRes.ok()).toBeTruthy();

  // Verify rolled back response
  const mockRes = await request.get(`/mock${path}`);
  expect(mockRes.status()).toBe(200);
  const payload = await mockRes.json();
  expect(payload.v).toBe('1');

  // Cleanup
  await request.delete(`/api/routes/${route.id}`);
});

