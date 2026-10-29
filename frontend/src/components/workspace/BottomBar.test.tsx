import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BottomBar } from './BottomBar';
import { ExecutionEvent } from '@/types';

const mockUseTraffic = vi.fn();
const mockUseWorkspace = vi.fn();

vi.mock('@/hooks/useTraffic', () => ({
  useTraffic: () => mockUseTraffic(),
}));

vi.mock('@/context/WorkspaceContext', () => ({
  useWorkspace: () => mockUseWorkspace(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

const mockEvent1: ExecutionEvent = {
  id: 'req-uuid-1111-2222',
  timestamp: '2026-10-29T12:00:00.000Z',
  protocol: 'HTTP',
  resourceId: 'route-mobile-banking-login-1234',
  method: 'GET',
  path: '/mobile-banking-login',
  queryString: 'action=login&returnUrl=http%3A%2F%2Flocalhost%2Fmock%2Fredirector',
  queryParams: {
    action: 'login',
    returnUrl: 'http://localhost/mock/redirector',
  },
  requestHeaders: {
    Authorization: 'Bearer test-token',
    Accept: 'text/html',
  },
  requestBody: '{"test":true}',
  status: 200,
  responseHeaders: {
    'Content-Type': 'text/html; charset=utf-8',
  },
  responseBody: '<html><body>Login Success</body></html>',
  durationMs: 126.43,
  payloadSizeBytes: 3276,
  bodyTruncated: false,
};

const mockWebhookEvent: ExecutionEvent = {
  id: 'webhook-uuid-3333-4444',
  parentEventId: 'req-uuid-1111-2222',
  timestamp: '2026-10-29T12:00:01.000Z',
  protocol: 'WEBHOOK',
  resourceId: 'route-mobile-banking-login-1234',
  method: 'POST',
  path: 'http://localhost:8080/mock/return-url-redirector',
  status: 200,
  durationMs: 18.25,
  payloadSizeBytes: 120,
  webhookDetail: {
    sourceRouteId: 'route-mobile-banking-login-1234',
    parentEventId: 'req-uuid-1111-2222',
    targetUrl: 'http://localhost:8080/mock/return-url-redirector',
    finalUrl: 'http://localhost:8080/mock/return-url-final-destination',
    method: 'POST',
    delayMs: 0,
    requestBody: '{"action":"login_callback"}',
    responseStatus: 200,
    responseBody: '{"result":"acknowledged"}',
  },
  redirectChain: [
    {
      step: 1,
      method: 'GET',
      url: 'http://localhost:8080/mock/return-url-redirector',
      status: 302,
      location: 'http://localhost:8080/mock/return-url-final-destination',
      durationMs: 6.1,
    },
    {
      step: 2,
      method: 'GET',
      url: 'http://localhost:8080/mock/return-url-final-destination',
      status: 200,
      durationMs: 12.15,
    },
  ],
};

describe('BottomBar & Console Explorer (Phase 6)', () => {
  const clearEventsMock = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockUseTraffic.mockReturnValue({
      events: [mockEvent1, mockWebhookEvent],
      clearEvents: clearEventsMock,
    });
    mockUseWorkspace.mockReturnValue({
      lastResponse: null,
      activeRoute: null,
    });
  });

  it('1. renders collapsed event rows with method, path, query string, status, duration, and size', () => {
    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    // HTTP Row
    const row1 = screen.getByTestId(`traffic-event-row-${mockEvent1.id}`);
    expect(row1).toBeTruthy();
    expect(row1.textContent).toContain('HTTP');
    expect(row1.textContent).toContain('GET');
    expect(row1.textContent).toContain('/mobile-banking-login');
    expect(row1.textContent).toContain('?action=login&returnUrl=');
    expect(row1.textContent).toContain('200');
    expect(row1.textContent).toContain('126.43ms');
    expect(row1.textContent).toContain('3.2 KB');

    // WEBHOOK Row
    const rowWebhook = screen.getByTestId(`traffic-event-row-${mockWebhookEvent.id}`);
    expect(rowWebhook).toBeTruthy();
    expect(rowWebhook.textContent).toContain('WEBHOOK');
    expect(rowWebhook.textContent).toContain('POST');
    expect(rowWebhook.textContent).toContain('http://localhost:8080/mock/return-url-redirector');
  });

  it('2. expands row upon click and displays full Request ID, Route ID, Parent Event ID, and copy buttons', () => {
    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    // Initially details are collapsed
    expect(screen.queryByTestId(`traffic-event-details-${mockEvent1.id}`)).toBeNull();

    // Click row 1 header to expand
    fireEvent.click(screen.getByTestId(`traffic-row-header-${mockEvent1.id}`));

    const details = screen.getByTestId(`traffic-event-details-${mockEvent1.id}`);
    expect(details).toBeTruthy();

    // Verify full untruncated IDs
    expect(screen.getByTestId('traffic-detail-req-id').textContent).toBe(mockEvent1.id);
    expect(screen.getByTestId('traffic-detail-route-id').textContent).toBe(mockEvent1.resourceId);
    expect(screen.getByTestId('traffic-copy-req-id')).toBeTruthy();
    expect(screen.getByTestId('traffic-copy-route-id')).toBeTruthy();
  });

  it('3. displays decoded query parameters table in Query Params tab', () => {
    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    fireEvent.click(screen.getByTestId(`traffic-row-header-${mockEvent1.id}`));

    const paramsTab = screen.getByTestId('traffic-tab-params');
    fireEvent.click(paramsTab);

    const table = screen.getByTestId('traffic-query-params-table');
    expect(table).toBeTruthy();
    expect(table.textContent).toContain('action');
    expect(table.textContent).toContain('login');
    expect(table.textContent).toContain('returnUrl');
    expect(table.textContent).toContain('http://localhost/mock/redirector');
  });

  it('4. displays parentEventId correlation, webhook details, and redirect hops for Webhook events', () => {
    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    // Click webhook row to expand
    fireEvent.click(screen.getByTestId(`traffic-row-header-${mockWebhookEvent.id}`));

    const details = screen.getByTestId(`traffic-event-details-${mockWebhookEvent.id}`);
    expect(details).toBeTruthy();

    // Parent correlation ID
    expect(screen.getByTestId('traffic-detail-parent-id').textContent).toBe(mockEvent1.id);

    // Webhook tab
    const webhookTab = screen.getByTestId('traffic-tab-webhook');
    fireEvent.click(webhookTab);
    expect(details.textContent).toContain('Asynchronous Webhook Dispatch');
    expect(details.textContent).toContain('http://localhost:8080/mock/return-url-redirector');
    expect(details.textContent).toContain('http://localhost:8080/mock/return-url-final-destination');

    // Redirects tab
    const redirectsTab = screen.getByTestId('traffic-tab-redirects');
    fireEvent.click(redirectsTab);
    expect(details.textContent).toContain('Redirect Chain');
    expect(details.textContent).toContain('302');
  });

  it('5. filters events by protocol pill buttons', () => {
    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    // Click WEBHOOK filter pill
    const webhookPill = screen.getByTestId('traffic-filter-webhook');
    fireEvent.click(webhookPill);

    expect(screen.queryByTestId(`traffic-event-row-${mockEvent1.id}`)).toBeNull();
    expect(screen.getByTestId(`traffic-event-row-${mockWebhookEvent.id}`)).toBeTruthy();

    // Click ALL pill
    const allPill = screen.getByTestId('traffic-filter-all');
    fireEvent.click(allPill);

    expect(screen.getByTestId(`traffic-event-row-${mockEvent1.id}`)).toBeTruthy();
    expect(screen.getByTestId(`traffic-event-row-${mockWebhookEvent.id}`)).toBeTruthy();
  });

  it('6. filters events by search query input', () => {
    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    const searchInput = screen.getByTestId('traffic-search-input');
    fireEvent.change(searchInput, { target: { value: 'webhook-uuid' } });

    // Only webhook row contains 'webhook-uuid'
    expect(screen.queryByTestId(`traffic-event-row-${mockEvent1.id}`)).toBeNull();
    expect(screen.getByTestId(`traffic-event-row-${mockWebhookEvent.id}`)).toBeTruthy();

    // Clear search
    fireEvent.click(screen.getByTestId('traffic-search-clear'));
    expect(screen.getByTestId(`traffic-event-row-${mockEvent1.id}`)).toBeTruthy();
  });

  it('7. captures workspace test client execution history in History tab', () => {
    mockUseWorkspace.mockReturnValue({
      lastResponse: {
        status: 200,
        statusText: '200 OK',
        durationMs: 14.5,
      },
      activeRoute: {
        method: 'POST',
        path: '/mobile-banking-login',
      },
    });

    render(<BottomBar isOpen={true} onToggle={vi.fn()} />);

    const historyTabBtn = screen.getByTestId('bottom-bar-tab-history');
    fireEvent.click(historyTabBtn);

    const historyView = screen.getByTestId('bottom-bar-history-view');
    expect(historyView).toBeTruthy();
    expect(historyView.textContent).toContain('POST');
    expect(historyView.textContent).toContain('/mobile-banking-login');
    expect(historyView.textContent).toContain('200');
    expect(historyView.textContent).toContain('14.5ms');
  });
});
