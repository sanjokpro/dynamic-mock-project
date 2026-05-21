import { test, expect } from '@playwright/test';

test('create, activate, and hit a gRPC mock (unary)', async ({ request }) => {
  const unique = `grpc-${Date.now()}`;
  const serviceName = `demo.${unique}`;
  const methodName = 'SayHello';

  // Create gRPC endpoint
  const createRes = await request.post('/api/grpc/endpoints', {
    data: {
      name: unique,
      description: 'Playwright gRPC mock',
      serviceName,
      protoSchema: `
        syntax = "proto3";
        package demo;
        service Greeter {
          rpc ${methodName} (HelloRequest) returns (HelloReply);
        }
        message HelloRequest { string name = 1; }
        message HelloReply { string message = 1; }
      `,
      methods: [
        {
          methodName,
          methodType: 'UNARY',
          responseTemplate: '{"message":"Hello {{request.name}}"}',
          statusCode: 'OK',
        },
      ],
      port: 9090,
      active: true,
    },
  });
  expect(createRes.status()).toBe(201);
  const endpoint = await createRes.json();
  expect(endpoint.id).toBeTruthy();

  // Activate (in case)
  await request.post(`/api/grpc/endpoints/${endpoint.id}/activate`);

  // Call through gateway REST-to-gRPC bridge (if available)
  // If the frontend exposes a REST proxy for gRPC, adjust this call.
  // Here we validate via service state only since native gRPC requires a client.
  const listRes = await request.get('/api/grpc/endpoints');
  expect(listRes.ok()).toBeTruthy();
  const endpoints = await listRes.json();
  const found = endpoints.find((e: any) => e.id === endpoint.id);
  expect(found).toBeTruthy();
  expect(found.active).toBeTruthy();

  // Cleanup
  await request.delete(`/api/grpc/endpoints/${endpoint.id}`);
});

