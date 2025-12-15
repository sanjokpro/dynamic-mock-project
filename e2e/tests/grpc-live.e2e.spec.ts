import { test, expect } from '@playwright/test';
import * as grpc from '@grpc/grpc-js';
import net from 'net';

async function getFreePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.unref();
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      if (address && typeof address === 'object') {
        const port = address.port;
        server.close(() => resolve(port));
      } else {
        server.close(() => reject(new Error('Failed to allocate port')));
      }
    });
  });
}

test('gRPC live call hits mock server (unary)', async () => {
  const unique = `grpc-live-${Date.now()}`;
  // Service name must match the dynamic path; keep unique to avoid conflicts
  const serviceName = `demo.Greeter${unique}`;
  const methodName = 'SayHello';
  const port = await getFreePort();

  // Create endpoint via REST
  const createRes = await fetch('http://localhost:8080/api/grpc/endpoints', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: unique,
      description: 'Playwright gRPC live',
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
          responseTemplate: '{"message":"Hello World"}',
          statusCode: 'OK',
        },
      ],
      port,
      active: true,
    }),
  });
  expect(createRes.status).toBe(201);
  const endpoint = await createRes.json();

  // Activate (idempotent)
  await fetch(`http://localhost:8080/api/grpc/endpoints/${endpoint.id}/activate`, { method: 'POST' });

  // Build dynamic client from proto
  // Generic client with JSON pass-through (server uses raw bytes)
  const Greeter = grpc.makeGenericClientConstructor(
    {
      SayHello: {
        path: `/${serviceName}/${methodName}`,
        requestStream: false,
        responseStream: false,
        requestSerialize: (obj: any) => Buffer.from(JSON.stringify(obj)),
        requestDeserialize: (buf: Buffer) => JSON.parse(buf.toString()),
        responseSerialize: (obj: any) => Buffer.from(JSON.stringify(obj)),
        responseDeserialize: (buf: Buffer) => JSON.parse(buf.toString()),
      },
    },
    'Greeter'
  );
  const client = new Greeter(`127.0.0.1:${port}`, grpc.credentials.createInsecure()) as any;

  // Invoke unary
  const reply: any = await new Promise((resolve, reject) => {
    client[methodName]({ name: 'World' }, (err: any, res: any) => {
      if (err) return reject(err);
      resolve(res);
    });
  });
  expect(reply.message).toBe('Hello World');

  // Cleanup
  await fetch(`http://localhost:8080/api/grpc/endpoints/${endpoint.id}`, { method: 'DELETE' });
});

