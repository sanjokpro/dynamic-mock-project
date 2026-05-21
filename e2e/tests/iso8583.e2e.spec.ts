import { test, expect } from '@playwright/test';

test('create, activate, and list ISO8583 mock', async ({ request }) => {
  const unique = `iso-${Date.now()}`;

  // Create ISO8583 endpoint
  const createRes = await request.post('/api/iso8583/endpoints', {
    data: {
      name: unique,
      description: 'Playwright ISO8583 mock',
      host: '0.0.0.0',
      port: 5000,
      mti: '0100',
      fieldMatchers: {
        '2': '^4111',
      },
      responseFields: {
        '39': '00',
      },
      scriptLanguage: 'js',
      active: true,
    },
  });
  expect(createRes.status()).toBe(201);
  const endpoint = await createRes.json();
  expect(endpoint.id).toBeTruthy();

  // Activate (in case)
  await request.post(`/api/iso8583/endpoints/${endpoint.id}/activate`);

  // Verify listing
  const listRes = await request.get('/api/iso8583/endpoints');
  expect(listRes.ok()).toBeTruthy();
  const endpoints = await listRes.json();
  const found = endpoints.find((e: any) => e.id === endpoint.id);
  expect(found).toBeTruthy();
  expect(found.active).toBeTruthy();

  // Cleanup
  await request.delete(`/api/iso8583/endpoints/${endpoint.id}`);
});

