import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { RouteDrawer, isHtmlRoute } from './RouteDrawer';

const mockUseWorkspace = vi.fn();
const mockUpdateRoute = vi.fn();
const mockCreateRoute = vi.fn();
const mockDeleteRoute = vi.fn();
const htmlPreviewFrameSpy = vi.fn();

vi.mock('@/context/WorkspaceContext', () => ({
  useWorkspace: () => mockUseWorkspace(),
}));

vi.mock('@/hooks/useRoutes', () => ({
  useRoutes: () => ({
    updateRoute: mockUpdateRoute,
    createRoute: mockCreateRoute,
    deleteRoute: mockDeleteRoute,
  }),
}));

vi.mock('@/context/DialogContext', () => ({
  useDialogs: () => ({ confirm: vi.fn() }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

// Wrap the real HtmlPreviewFrame so we can assert on the props it receives
vi.mock('./HtmlPreviewFrame', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./HtmlPreviewFrame')>();
  return {
    HtmlPreviewFrame: (props: any) => {
      htmlPreviewFrameSpy(props);
      return <actual.HtmlPreviewFrame {...props} />;
    },
  };
});

const LONG_HTML_TEMPLATE =
  '<!DOCTYPE html><html><head><title>Mobile Banking Login</title></head><body>' +
  '<form id="mobile-login-form"><input id="mpin" type="password" /><button id="login-btn">Login</button></form>' +
  '<script>fetch(window.location.pathname + "?action=login");</script>' +
  '<p>' + 'X'.repeat(600) + '</p>' +
  '<div id="tail-marker">END_OF_TEMPLATE</div></body></html>';

function buildRoute(overrides: Record<string, unknown> = {}) {
  return {
    id: 'route-html-1',
    name: 'Mobile Banking Login',
    protocol: 'HTTP',
    method: 'GET',
    path: '/mobile-banking-login',
    responseStatus: 200,
    responseHeaders: { 'Content-Type': 'text/html; charset=utf-8' },
    responseTemplate: LONG_HTML_TEMPLATE,
    active: true,
    delayMs: 0,
    version: 1,
    createdAt: '2026-10-29T00:00:00Z',
    updatedAt: '2026-10-29T00:00:00Z',
    ...overrides,
  };
}

function mockWorkspaceWithRoute(route: unknown) {
  mockUseWorkspace.mockReturnValue({
    activeRoute: route,
    setActiveRoute: vi.fn(),
    setDrawerOpen: vi.fn(),
  });
}

describe('RouteDrawer - Phase 5 HTML Route Preview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. detects HTML routes via Content-Type header and template shape', () => {
    expect(isHtmlRoute({ 'Content-Type': 'text/html; charset=utf-8' }, 'plain')).toBe(true);
    expect(isHtmlRoute({ 'content-type': 'application/xhtml+xml' }, '')).toBe(true);
    expect(isHtmlRoute({}, '<!DOCTYPE html><html></html>')).toBe(true);
    expect(isHtmlRoute(undefined, '  <html lang="en"></html>')).toBe(true);
    expect(isHtmlRoute({ 'Content-Type': 'application/json' }, '{"ok":true}')).toBe(false);
    expect(isHtmlRoute(undefined, undefined)).toBe(false);
  });

  it('2. defaults to the Source tab (not Preview) for HTML routes and mounts no iframe', () => {
    mockWorkspaceWithRoute(buildRoute());
    render(<RouteDrawer isOpen={true} onClose={vi.fn()} />);

    const sourceTab = screen.getByTestId('route-template-tab-source');
    const previewTab = screen.getByTestId('route-template-tab-preview');
    expect(sourceTab.getAttribute('aria-selected')).toBe('true');
    expect(previewTab.getAttribute('aria-selected')).toBe('false');

    expect(screen.getByTestId('route-template-source')).toBeTruthy();
    expect(screen.queryByTestId('html-preview-iframe')).toBeNull();
    expect(htmlPreviewFrameSpy).not.toHaveBeenCalled();
  });

  it('3. renders the full template in Source view without 300-character truncation', () => {
    mockWorkspaceWithRoute(buildRoute());
    render(<RouteDrawer isOpen={true} onClose={vi.fn()} />);

    const source = screen.getByTestId('route-template-source');
    expect(LONG_HTML_TEMPLATE.length).toBeGreaterThan(300);
    expect(source.textContent).toBe(LONG_HTML_TEMPLATE);
    expect(source.textContent).toContain('END_OF_TEMPLATE');
    expect(source.textContent?.endsWith('...')).toBe(false);
  });

  it('4 & 5. renders a static sandbox="" preview when Preview tab is selected', () => {
    mockWorkspaceWithRoute(buildRoute());
    render(<RouteDrawer isOpen={true} onClose={vi.fn()} />);

    fireEvent.click(screen.getByTestId('route-template-tab-preview'));

    expect(screen.getByTestId('route-template-preview')).toBeTruthy();
    const iframe = screen.getByTestId('html-preview-iframe') as HTMLIFrameElement;
    expect(iframe.getAttribute('sandbox')).toBe('');
    expect(iframe.getAttribute('sandbox')).not.toContain('allow-scripts');
    expect(iframe.getAttribute('sandbox')).not.toContain('allow-same-origin');
    expect(iframe.getAttribute('referrerpolicy') || iframe.referrerPolicy).toBe('no-referrer');
    expect(iframe.getAttribute('src')).toBeNull();
    expect(iframe.getAttribute('srcdoc')).toContain('Mobile Banking Login');
    expect(screen.getByTestId('html-preview-mode-badge').textContent).toContain('Static');
  });

  it('6. passes interactive={false} and compact to HtmlPreviewFrame', () => {
    mockWorkspaceWithRoute(buildRoute());
    render(<RouteDrawer isOpen={true} onClose={vi.fn()} />);

    fireEvent.click(screen.getByTestId('route-template-tab-preview'));

    expect(htmlPreviewFrameSpy).toHaveBeenCalled();
    const props = htmlPreviewFrameSpy.mock.calls[htmlPreviewFrameSpy.mock.calls.length - 1][0];
    expect(props.interactive).toBe(false);
    expect(props.compact).toBe(true);
    expect(props.html).toBe(LONG_HTML_TEMPLATE);
    expect(props.mockPath).toBe('/mobile-banking-login');
  });

  it('keeps non-HTML (JSON) routes on plain full source view without Source/Preview toggle', () => {
    const jsonTemplate = JSON.stringify({ data: 'Y'.repeat(500), tail: 'JSON_TAIL' });
    mockWorkspaceWithRoute(
      buildRoute({
        responseHeaders: { 'Content-Type': 'application/json' },
        responseTemplate: jsonTemplate,
      })
    );
    render(<RouteDrawer isOpen={true} onClose={vi.fn()} />);

    expect(screen.queryByTestId('route-template-tab-source')).toBeNull();
    expect(screen.queryByTestId('route-template-tab-preview')).toBeNull();
    expect(screen.getByTestId('route-template-source').textContent).toBe(jsonTemplate);
    expect(screen.queryByTestId('html-preview-iframe')).toBeNull();
  });

  it('remains passive: opening the drawer performs no route mutations or network-backed actions', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch' as any).mockImplementation(() => Promise.resolve(new Response()));
    mockWorkspaceWithRoute(buildRoute());

    render(<RouteDrawer isOpen={true} onClose={vi.fn()} />);
    fireEvent.click(screen.getByTestId('route-template-tab-preview'));

    expect(fetchSpy).not.toHaveBeenCalled();
    expect(mockUpdateRoute).not.toHaveBeenCalled();
    expect(mockCreateRoute).not.toHaveBeenCalled();
    expect(mockDeleteRoute).not.toHaveBeenCalled();

    fetchSpy.mockRestore();
  });
});
