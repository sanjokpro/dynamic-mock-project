import { test, expect } from '@playwright/test';
import net from 'net';
// @ts-ignore
import Iso8583 from 'iso_8583';

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

function buildIsoMessage() {
  const msg = new Iso8583({
    '0': '0100',
    '2': '4111111111111111',
    '3': '000000',
    '4': '000000010000',
    '7': '0709163030',
    '11': '123456',
    '12': '163030',
    '13': '0709',
    '37': '123456789012',
    '41': '12345678',
    '49': '840',
  });
  return msg;
}

function encodeWithLength(buf: Buffer, header: '2BYTE' | '4BYTE' | 'NONE' = '2BYTE') {
  if (header === 'NONE') return buf;
  if (header === '4BYTE') {
    const out = Buffer.alloc(4 + buf.length);
    out.writeInt32BE(buf.length, 0);
    buf.copy(out, 4);
    return out;
  }
  const out = Buffer.alloc(2 + buf.length);
  out.writeUInt16BE(buf.length, 0);
  buf.copy(out, 2);
  return out;
}

function decodeWithLength(buf: Buffer, header: '2BYTE' | '4BYTE' | 'NONE' = '2BYTE') {
  if (header === 'NONE') return buf;
  const len = header === '4BYTE' ? buf.readInt32BE(0) : buf.readUInt16BE(0);
  return buf.slice(header === '4BYTE' ? 4 : 2, (header === '4BYTE' ? 4 : 2) + len);
}

test('ISO8583 live round-trip (standalone/Q2)', async () => {
  const unique = `iso-live-${Date.now()}`;
  const port = await getFreePort();

  // Cleanup existing ISO endpoints to avoid port conflicts/reloads
  try {
    const list = await fetch(`${process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'}/api/iso8583/endpoints`);
    if (list.ok) {
      const endpoints = await list.json();
      for (const ep of endpoints) {
        await fetch(`${process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080'}/api/iso8583/endpoints/${ep.id}`, { method: 'DELETE' });
      }
    }
  } catch (e) {
    // ignore cleanup errors
  }

  // Create ISO8583 endpoint
  const base = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080';
  const createRes = await fetch(`${base}/api/iso8583/endpoints`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: unique,
      description: 'Playwright ISO8583 live',
      port,
      isolatedPort: true,
      mocks: [
        {
          mti: '0100',
          responseMti: '0110',
          matchers: { 'field.2': '^4111' },
          responseFields: { 39: '00' },
          enabled: true,
        },
      ],
      headerLengthType: '2BYTE',
      encoding: 'ASCII',
      active: true,
    }),
  });
  if (createRes.status !== 201) {
    const body = await createRes.text();
    throw new Error(`ISO create failed: ${createRes.status} ${body}`);
  }
  const endpoint = await createRes.json();

  // Activate (idempotent)
  await fetch(`${base}/api/iso8583/endpoints/${endpoint.id}/activate`, { method: 'POST' });

  // Give server a moment to start (Q2/standalone)
  await new Promise((res) => setTimeout(res, 1500));

  // Build message
  const isoMsg = buildIsoMessage();
  const payload = isoMsg.getBufferMessage();
  const framed = encodeWithLength(payload, '2BYTE');

  const responseBuf: Buffer = await new Promise((resolve, reject) => {
    const socket = new net.Socket();
    const chunks: Buffer[] = [];
    socket.setTimeout(10000);
    socket.connect(port, '127.0.0.1', () => {
      socket.write(framed);
    });
    socket.on('data', (d) => chunks.push(d));
    socket.on('timeout', () => {
      socket.destroy();
      reject(new Error('ISO8583 socket timeout'));
    });
    socket.on('error', (err) => reject(err));
    socket.on('close', () => {
      if (chunks.length === 0) {
        return reject(new Error('No response received'));
      }
      resolve(Buffer.concat(chunks));
    });
  });

  const isoRespBuf = decodeWithLength(responseBuf, '2BYTE');
  const respMsg = new Iso8583(isoRespBuf);
  expect(respMsg.getField(0)).toBe('0110');
  expect(respMsg.getField(39)).toBe('00');

  // Cleanup
  await fetch(`http://localhost:8080/api/iso8583/endpoints/${endpoint.id}`, { method: 'DELETE' });
});

