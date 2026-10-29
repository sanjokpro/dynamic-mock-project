import { describe, it, expect } from 'vitest';
import {
  buildSandboxedSrcDoc,
  normalizeMockPreviewUrl,
  resolvePreviewContext,
} from './htmlPreviewShim';

describe('htmlPreviewShim', () => {
  describe('resolvePreviewContext', () => {
    it('normalizes mockPath and builds baseHref and targetUrl', () => {
      const ctx = resolvePreviewContext({
        mockPath: '/mobile-banking-login',
        origin: 'http://localhost:8080/',
      });

      expect(ctx.origin).toBe('http://localhost:8080');
      expect(ctx.mockPath).toBe('/mobile-banking-login');
      expect(ctx.baseHref).toBe('http://localhost:8080/mock/');
      expect(ctx.targetUrl).toBe('http://localhost:8080/mock/mobile-banking-login');
    });

    it('strips leading /mock prefix from mockPath if provided', () => {
      const ctx = resolvePreviewContext({
        mockPath: '/mock/oauth/authorize',
        origin: 'http://localhost',
      });

      expect(ctx.mockPath).toBe('/oauth/authorize');
      expect(ctx.targetUrl).toBe('http://localhost/mock/oauth/authorize');
    });
  });

  describe('normalizeMockPreviewUrl', () => {
    const context = {
      mockPath: '/mobile-banking-login',
      origin: 'http://localhost:8080',
      targetUrl: 'http://localhost:8080/mock/mobile-banking-login?returnUrl=http://localhost/mock/dest',
    };

    it('resolves srcdoc?... patterns produced by window.location.pathname in srcdoc', () => {
      expect(
        normalizeMockPreviewUrl('srcdoc?action=login&returnUrl=http%3A%2F%2Flocalhost%2Fmock%2Fdest', context)
      ).toBe(
        'http://localhost:8080/mock/mobile-banking-login?action=login&returnUrl=http%3A%2F%2Flocalhost%2Fmock%2Fdest'
      );

      expect(normalizeMockPreviewUrl('/srcdoc?action=login', context)).toBe(
        'http://localhost:8080/mock/mobile-banking-login?action=login'
      );

      expect(normalizeMockPreviewUrl('about:srcdoc?action=login', context)).toBe(
        'http://localhost:8080/mock/mobile-banking-login?action=login'
      );
    });

    it('resolves ?query=value patterns against the active mock endpoint', () => {
      expect(normalizeMockPreviewUrl('?step=verify&otp=123456', context)).toBe(
        'http://localhost:8080/mock/mobile-banking-login?step=verify&otp=123456'
      );
    });

    it('resolves /mock/... paths against the origin', () => {
      expect(normalizeMockPreviewUrl('/mock/return-url-redirector', context)).toBe(
        'http://localhost:8080/mock/return-url-redirector'
      );
    });

    it('resolves root-relative paths (/...) under /mock', () => {
      expect(normalizeMockPreviewUrl('/oauth/token', context)).toBe(
        'http://localhost:8080/mock/oauth/token'
      );
    });

    it('resolves relative paths (submit, ./submit) under baseHref', () => {
      expect(normalizeMockPreviewUrl('submit-login', context)).toBe(
        'http://localhost:8080/mock/submit-login'
      );
      expect(normalizeMockPreviewUrl('./submit-login', context)).toBe(
        'http://localhost:8080/mock/submit-login'
      );
    });

    it('leaves absolute http/https/data URLs untouched', () => {
      expect(normalizeMockPreviewUrl('https://example.com/callback', context)).toBe(
        'https://example.com/callback'
      );
      expect(normalizeMockPreviewUrl('data:image/png;base64,AAAA', context)).toBe(
        'data:image/png;base64,AAAA'
      );
    });
  });

  describe('buildSandboxedSrcDoc', () => {
    it('injects <base>, window.__DYNAMIC_MOCK_CONTEXT__, and surgical URL shim into <head>', () => {
      const html = `<!DOCTYPE html><html><head><title>Login</title></head><body><h1>Hello</h1></body></html>`;
      const result = buildSandboxedSrcDoc(html, {
        mockPath: '/mobile-banking-login',
        origin: 'http://localhost:8080',
        targetUrl: 'http://localhost:8080/mock/mobile-banking-login?returnUrl=http://localhost/mock/dest',
      });

      expect(result).toContain('<base href="http://localhost:8080/mock/" />');
      expect(result).toContain('window.__DYNAMIC_MOCK_CONTEXT__');
      expect(result).toContain('"mockPath":"/mobile-banking-login"');
      expect(result).toContain('window.resolveMockUrl = resolveMockUrl');
      expect(result).toContain('<title>Login</title>');
      expect(result).toContain('<h1>Hello</h1>');
    });

    it('wraps HTML fragments or plain text responses when <head> and <html> are absent', () => {
      const plain = 'no return url were provided';
      const result = buildSandboxedSrcDoc(plain, {
        mockPath: '/mobile-banking-login',
        origin: 'http://localhost:8080',
      });

      expect(result).toContain('<!DOCTYPE html>');
      expect(result).toContain('<base href="http://localhost:8080/mock/" />');
      expect(result).toContain('no return url were provided');
    });
  });
});
