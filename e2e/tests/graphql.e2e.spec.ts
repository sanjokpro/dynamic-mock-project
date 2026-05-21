import { test, expect } from '@playwright/test';

test('create, execute, and delete a GraphQL mock', async ({ request }) => {
  const unique = `gql-${Date.now()}`;
  const schema = /* GraphQL */ `
    type Query {
      hello(name: String): String
    }
  `;

  const resolvers = [
    {
      operationType: 'QUERY',
      fieldName: 'hello',
      responseTemplate: '{"data":{"hello":"Hello {{args.name}}"}}',
      delayMs: 0,
    },
  ];

  // Create endpoint
  const createRes = await request.post('/api/graphql/endpoints', {
    data: {
      name: unique,
      description: 'Playwright E2E GraphQL endpoint',
      schema,
      resolvers,
      active: true,
    },
  });
  expect(createRes.status()).toBe(201);
  const endpoint = await createRes.json();
  expect(endpoint.id).toBeTruthy();

  // Execute query
  const execRes = await request.post(`/api/graphql/endpoints/${endpoint.id}/execute`, {
    data: {
      query: `{ hello(name: "World") }`,
      variables: {},
    },
  });
  expect(execRes.ok()).toBeTruthy();
  const body = await execRes.json();
  expect(body.data.hello).toBe('Hello World');

  // Cleanup
  await request.delete(`/api/graphql/endpoints/${endpoint.id}`);
});

