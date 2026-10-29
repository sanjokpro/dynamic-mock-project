import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ResponsePanel, highlightJson, isHtmlResponse } from './ResponsePanel';
import { RedirectChainBanner } from './RedirectChainBanner';

const mockUseWorkspace = vi.fn();

vi.mock('@/context/WorkspaceContext', () => ({
  useWorkspace: () => mockUseWorkspace(),
}));

vi.mock('@/context/ThemeContext', () => ({
  useTheme: () => ({ theme: 'dark' }),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('ResponsePanel - highlightJson', () => {
  it('correctly preserves string values and does not swallow them (BUG 1 fix)', () => {
    const input = '{"reached":"head2"}';
    const highlighted = highlightJson(input, 'dark');

    expect(highlighted).toContain('head2');
    expect(highlighted).not.toContain('&quot;&quot;');
    expect(highlighted).toContain('<span style="color:#7dd3fc">&quot;reached&quot;</span>');
    expect(highlighted).toContain('<span style="color:#a5d6a7">&quot;head2&quot;</span>');
  });

  it('correctly tokenizes complex JSON structures', () => {
    const json = JSON.stringify({
      message: 'success',
      count: 42,
      active: true,
      data: null,
      items: ['one', 'two'],
    });

    const highlighted = highlightJson(json, 'dark');
    expect(highlighted).toContain('success');
    expect(highlighted).toContain('42');
    expect(highlighted).toContain('true');
    expect(highlighted).toContain('null');
    expect(highlighted).toContain('one');
    expect(highlighted).toContain('two');
  });

  it('handles empty string value correctly without dropping keys', () => {
    const input = '{"key":""}';
    const highlighted = highlightJson(input, 'dark');
    expect(highlighted).toContain('key');
    expect(highlighted).toContain('&quot;&quot;');
  });

  it('applies search query highlighting with mark tags without altering JSON tokens', () => {
    const input = '{"name":"Alice","role":"admin"}';
    const highlighted = highlightJson(input, 'dark', 'alice');

    expect(highlighted).toContain(
      '<mark class="bg-amber-400/40 text-amber-200 rounded-xs px-0.5 font-bold">Alice</mark>'
    );
    expect(highlighted).toContain('role');
  });

  it('handles search queries that match property keys', () => {
    const input = '{"status":"active"}';
    const highlighted = highlightJson(input, 'dark', 'status');

    expect(highlighted).toContain(
      '<mark class="bg-amber-400/40 text-amber-200 rounded-xs px-0.5 font-bold">status</mark>'
    );
  });
});

describe('ResponsePanel - Phase 4 Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('1. detects HTML responses via Content-Type header, doctype, and <html> tag', () => {
    expect(isHtmlResponse({ 'Content-Type': 'text/html; charset=utf-8' }, 'plain text')).toBe(true);
    expect(isHtmlResponse({ 'content-type': 'application/xhtml+xml' }, '<div/>')).toBe(true);
    expect(isHtmlResponse({ 'Content-Type': 'application/json' }, '<!DOCTYPE html><html></html>')).toBe(true);
    expect(isHtmlResponse({}, '<html lang="en"><body>Hi</body></html>')).toBe(true);
    expect(isHtmlResponse({ 'Content-Type': 'application/json' }, '{"ok":true}')).toBe(false);
  });

  it('2 & 3. automatically selects Preview tab and renders HtmlPreviewFrame when response is HTML', () => {
    mockUseWorkspace.mockReturnValue({
      activeRoute: { id: 'route-1', method: 'GET', path: '/mobile-banking-login' },
      activeEnvironment: { id: 'env-1', name: 'DEV', variables: {} },
      lastResponse: {
        status: 200,
        statusText: '200 OK',
        headers: { 'Content-Type': 'text/html; charset=utf-8' },
        body: '<!DOCTYPE html><html><body><h1>Mobile Banking Login</h1></body></html>',
        durationMs: 14.28,
      },
    });

    render(<ResponsePanel />);

    const previewTab = screen.getByTestId('response-tab-preview');
    const bodyTab = screen.getByTestId('response-tab-body');
    const headersTab = screen.getByTestId('response-tab-headers');

    expect(previewTab).toBeTruthy();
    expect(bodyTab).toBeTruthy();
    expect(headersTab).toBeTruthy();

    // Preview tab is active and renders the sandboxed iframe via srcdoc
    const iframe = screen.getByTestId('html-preview-iframe') as HTMLIFrameElement;
    expect(iframe).toBeTruthy();
    expect(iframe.getAttribute('sandbox')).toBe('allow-scripts allow-forms allow-popups');
    expect(iframe.getAttribute('srcdoc')).toContain('Mobile Banking Login');

    // Can switch to Body tab to inspect raw HTML source
    fireEvent.click(bodyTab);
    expect(screen.queryByTestId('html-preview-iframe')).toBeNull();
  });

  it('4. preserves JSON backward compatibility (only Body and Headers tabs, defaults to Body)', () => {
    mockUseWorkspace.mockReturnValue({
      activeRoute: { id: 'route-json', method: 'GET', path: '/hello' },
      activeEnvironment: null,
      lastResponse: {
        status: 200,
        statusText: '200 OK',
        headers: { 'Content-Type': 'application/json' },
        body: '{"greeting":"hello world"}',
        durationMs: 8.5,
      },
    });

    render(<ResponsePanel />);

    expect(screen.queryByTestId('response-tab-preview')).toBeNull();
    expect(screen.getByTestId('response-tab-body')).toBeTruthy();
    expect(screen.getByTestId('response-tab-headers')).toBeTruthy();
    expect(screen.queryByTestId('redirect-chain-banner')).toBeNull();
  });

  it('5. renders RedirectChainBanner when redirectChain is present and hides when empty', () => {
    const { unmount } = render(<RedirectChainBanner redirectChain={[]} />);
    expect(screen.queryByTestId('redirect-chain-banner')).toBeNull();
    unmount();

    mockUseWorkspace.mockReturnValue({
      activeRoute: { id: 'route-redir', method: 'GET', path: '/return-url-redirector' },
      activeEnvironment: null,
      lastResponse: {
        status: 200,
        statusText: '200 OK',
        headers: { 'Content-Type': 'application/json' },
        body: '{"finalDestination":true}',
        durationMs: 18.42,
        redirectChain: [
          {
            step: 1,
            method: 'GET',
            url: 'http://localhost:8080/mock/return-url-redirector',
            status: 302,
            location: 'http://localhost:8080/mock/return-url-final-destination',
            durationMs: 6.12,
          },
          {
            step: 2,
            method: 'GET',
            url: 'http://localhost:8080/mock/return-url-final-destination',
            status: 200,
            durationMs: 12.3,
          },
        ],
      },
    });

    render(<ResponsePanel />);

    expect(screen.getByTestId('redirect-chain-banner')).toBeTruthy();
    expect(screen.getByTestId('redirect-chain-hop-count').textContent).toBe('2 Hops');
    expect(screen.getByTestId('redirect-hop-status-1').textContent).toBe('302');
    expect(screen.getByTestId('redirect-hop-location-1').textContent).toContain(
      'Location: http://localhost:8080/mock/return-url-final-destination'
    );
    expect(screen.getByTestId('redirect-hop-status-2').textContent).toBe('200');
  });

  it('6. uses response.html for HTML downloads and response.json for JSON downloads', () => {
    const createObjectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url');
    const revokeObjectUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    let downloadedFilename = '';
    const originalCreateElement = document.createElement.bind(document);
    const createElementSpy = vi.spyOn(document, 'createElement').mockImplementation((tagName: any) => {
      const el = originalCreateElement(tagName);
      if (tagName === 'a') {
        Object.defineProperty(el, 'download', {
          set(val: string) {
            downloadedFilename = val;
          },
          get() {
            return downloadedFilename;
          },
        });
      }
      return el;
    });

    // Test HTML download -> response.html
    mockUseWorkspace.mockReturnValue({
      activeRoute: { id: 'route-1', method: 'GET', path: '/login' },
      activeEnvironment: null,
      lastResponse: {
        status: 200,
        statusText: '200 OK',
        headers: { 'Content-Type': 'text/html' },
        body: '<html><body>Login</body></html>',
        durationMs: 10,
      },
    });

    const { rerender } = render(<ResponsePanel />);
    fireEvent.click(screen.getByTestId('response-download-btn'));
    expect(downloadedFilename).toBe('response.html');

    // Test JSON download -> response.json
    mockUseWorkspace.mockReturnValue({
      activeRoute: { id: 'route-2', method: 'GET', path: '/api' },
      activeEnvironment: null,
      lastResponse: {
        status: 200,
        statusText: '200 OK',
        headers: { 'Content-Type': 'application/json' },
        body: '{"ok":true}',
        durationMs: 5,
      },
    });

    rerender(<ResponsePanel />);
    fireEvent.click(screen.getByTestId('response-download-btn'));
    expect(downloadedFilename).toBe('response.json');

    createElementSpy.mockRestore();
    clickSpy.mockRestore();
    createObjectUrlSpy.mockRestore();
    revokeObjectUrlSpy.mockRestore();
  });
});
