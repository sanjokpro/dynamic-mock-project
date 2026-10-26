import { describe, it, expect, vi, beforeEach } from 'vitest';
import { generateCurlCommand, resolveCurlUrl, copyToClipboard } from './curl';

describe('curl utility', () => {
  describe('resolveCurlUrl', () => {
    it('resolves {{BASE_URL}} with fallback or baseOrigin', () => {
      const url = resolveCurlUrl('{{BASE_URL}}/tcws/transconnectservice', undefined, 'http://localhost/mock');
      expect(url).toBe('http://localhost/mock/tcws/transconnectservice');
    });

    it('resolves custom environment variables in URL', () => {
      const url = resolveCurlUrl('{{HOST}}:{{PORT}}/api/v1', { HOST: 'https://api.example.com', PORT: '8443' });
      expect(url).toContain('api.example.com');
      expect(url).toContain('8443');
      expect(url).toContain('/mock/');
    });

    it('resolves relative paths to full origin URLs', () => {
      const url = resolveCurlUrl('/orders', undefined, 'http://localhost');
      expect(url).toBe('http://localhost/mock/orders');
    });

    it('ensures /mock prefix when BASE_URL env var lacks it', () => {
      // This is the critical bug fix: environment variable has BASE_URL without /mock
      const url = resolveCurlUrl(
        '{{BASE_URL}}/tcws/transconnectservice',
        { BASE_URL: 'http://localhost' },
        undefined
      );
      expect(url).toBe('http://localhost/mock/tcws/transconnectservice');
    });

    it('preserves /mock when BASE_URL env var already includes it', () => {
      const url = resolveCurlUrl(
        '{{BASE_URL}}/tcws/transconnectservice',
        { BASE_URL: 'http://localhost/mock' },
        undefined
      );
      expect(url).toBe('http://localhost/mock/tcws/transconnectservice');
    });

    it('handles BASE_URL with trailing slash', () => {
      const url = resolveCurlUrl(
        '{{BASE_URL}}/api/v1',
        { BASE_URL: 'http://example.com/' },
        undefined
      );
      expect(url).toContain('/mock');
      expect(url).toContain('/api/v1');
    });
  });

  describe('generateCurlCommand', () => {
    it('generates standard GET command without headers or body', () => {
      const curl = generateCurlCommand({
        method: 'GET',
        url: 'http://localhost:8080/mock/hello'
      });

      expect(curl).toBe("curl -X GET 'http://localhost:8080/mock/hello'");
    });

    it('resolves {{BASE_URL}} in generateCurlCommand', () => {
      const curl = generateCurlCommand({
        method: 'POST',
        url: '{{BASE_URL}}/tcws/transconnectservice',
        headers: {
          'Content-Type': 'application/json',
          'head1': 'test'
        },
        body: '{"head2":"hello"}',
        baseOrigin: 'http://localhost/mock'
      });

      expect(curl).toContain("curl -X POST 'http://localhost/mock/tcws/transconnectservice'");
      expect(curl).toContain("-H 'Content-Type: application/json'");
      expect(curl).toContain("-H 'head1: test'");
      expect(curl).toContain("--data '{\"head2\":\"hello\"}'");
    });

    it('generates POST command with headers and formatted data', () => {
      const curl = generateCurlCommand({
        method: 'POST',
        url: 'http://localhost:8080/mock/tcws/transconnectservice',
        headers: {
          'Content-Type': 'application/json',
          'head1': 'test-val'
        },
        body: '{"head2":"value"}'
      });

      expect(curl).toBe(
        "curl -X POST 'http://localhost:8080/mock/tcws/transconnectservice' \\\n" +
        "  -H 'Content-Type: application/json' \\\n" +
        "  -H 'head1: test-val' \\\n" +
        "  --data '{\"head2\":\"value\"}'"
      );
    });

    it('correctly escapes single quotes in URL, headers, and body', () => {
      const curl = generateCurlCommand({
        method: 'POST',
        url: "http://localhost:8080/mock/user's/path",
        headers: {
          'X-Custom': "O'Reilly"
        },
        body: "{\"name\":\"O'Connor\"}"
      });

      expect(curl).toContain("user'\\''s/path");
      expect(curl).toContain("O'\\''Reilly");
      expect(curl).toContain("O'\\''Connor");
    });

    it('omits --data flag for GET and HEAD requests even if body is supplied', () => {
      const getCurl = generateCurlCommand({
        method: 'GET',
        url: 'http://localhost:8080/mock/users',
        body: '{"ignored": true}'
      });
      expect(getCurl).not.toContain('--data');

      const headCurl = generateCurlCommand({
        method: 'HEAD',
        url: 'http://localhost:8080/mock/users',
        body: '{"ignored": true}'
      });
      expect(headCurl).not.toContain('--data');
    });

    it('ignores headers with empty keys', () => {
      const curl = generateCurlCommand({
        method: 'GET',
        url: 'http://localhost:8080/mock/test',
        headers: {
          '': 'emptyKey',
          '   ': 'spacesKey',
          'Valid-Header': '123'
        }
      });

      expect(curl).not.toContain('emptyKey');
      expect(curl).not.toContain('spacesKey');
      expect(curl).toContain("-H 'Valid-Header: 123'");
    });

    it('excludes internal X-Dynamic-Mock-* headers from cURL output', () => {
      const curl = generateCurlCommand({
        method: 'POST',
        url: 'http://localhost/mock/api/test',
        headers: {
          'Content-Type': 'application/json',
          'X-Dynamic-Mock-Env': '22f5e1f0-cfab-40db-85de-66839bac870e',
          'X-Dynamic-Mock-Trace': 'some-trace-id',
          'X-Custom-Header': 'keep-this'
        },
        body: '{"test": true}'
      });

      expect(curl).not.toContain('X-Dynamic-Mock-Env');
      expect(curl).not.toContain('X-Dynamic-Mock-Trace');
      expect(curl).toContain("-H 'X-Custom-Header: keep-this'");
      expect(curl).toContain("-H 'Content-Type: application/json'");
    });

    it('replaces regex patterns like .* in header values with sensible defaults', () => {
      const curl = generateCurlCommand({
        method: 'POST',
        url: 'http://localhost/mock/api/test',
        headers: {
          'head1': '.*',
          'head2': 'head2 ko value',
          'Content-Type': 'application/json'
        },
        body: '{"key": "val"}'
      });

      // .* should become 'test', not remain as regex
      expect(curl).not.toContain("head1: .*");
      expect(curl).toContain("-H 'head1: test'");
      // Normal value should pass through unchanged
      expect(curl).toContain("-H 'head2: head2 ko value'");
    });

    it('ensures /mock in URL when BASE_URL variable has bare origin', () => {
      const curl = generateCurlCommand({
        method: 'POST',
        url: '{{BASE_URL}}/tcws/transconnectservice',
        headers: { 'Content-Type': 'application/json' },
        body: '{"head2":"hello"}',
        variables: { BASE_URL: 'http://localhost' }
      });

      expect(curl).toContain("'http://localhost/mock/tcws/transconnectservice'");
      expect(curl).not.toContain("'http://localhost/tcws/transconnectservice'");
    });
  });

  describe('copyToClipboard', () => {
    beforeEach(() => {
      vi.restoreAllMocks();
    });

    it('uses navigator.clipboard when available', async () => {
      const writeTextMock = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: writeTextMock
        }
      });

      const res = await copyToClipboard('test text');
      expect(res).toBe(true);
      expect(writeTextMock).toHaveBeenCalledWith('test text');
    });

    it('falls back to document.execCommand when clipboard.writeText fails', async () => {
      Object.assign(navigator, {
        clipboard: {
          writeText: vi.fn().mockRejectedValue(new Error('Permission denied'))
        }
      });
      document.execCommand = vi.fn().mockReturnValue(true);

      const res = await copyToClipboard('fallback text');
      expect(res).toBe(true);
      expect(document.execCommand).toHaveBeenCalledWith('copy');
    });
  });
});
