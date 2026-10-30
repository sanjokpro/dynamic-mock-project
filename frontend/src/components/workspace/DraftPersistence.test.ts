import { describe, it, expect, beforeEach } from 'vitest';
import { parseQueryParamsFromUrl, buildUrlWithQueryParams } from './RequestPanel';

describe('Test Request Draft Persistence (BUG 2 fix)', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('persists and retrieves test request draft from localStorage for an active route', () => {
    const routeId = 'test-route-123';
    const draftKey = `dynamicmock:test_draft:${routeId}`;

    const testDraft = {
      requestHeaders: { head1: 'test', 'X-Custom': 'value' },
      queryParams: { page: '1', size: '20' },
      requestBodyType: 'json',
      requestBodyContent: JSON.stringify({ head2: 'hello' }),
      updatedAt: Date.now()
    };

    localStorage.setItem(draftKey, JSON.stringify(testDraft));

    const retrievedRaw = localStorage.getItem(draftKey);
    expect(retrievedRaw).not.toBeNull();
    const retrieved = JSON.parse(retrievedRaw!);

    expect(retrieved.requestHeaders).toEqual({ head1: 'test', 'X-Custom': 'value' });
    expect(retrieved.queryParams).toEqual({ page: '1', size: '20' });
    expect(retrieved.requestBodyType).toBe('json');
    expect(retrieved.requestBodyContent).toBe(JSON.stringify({ head2: 'hello' }));
  });

  it('persists activeRouteId to survive browser page refresh', () => {
    const routeId = 'route-abc-456';
    localStorage.setItem('dynamicmock:active_route_id', routeId);

    expect(localStorage.getItem('dynamicmock:active_route_id')).toBe('route-abc-456');

    // Simulate switching routes
    const newRouteId = 'route-xyz-789';
    localStorage.setItem('dynamicmock:active_route_id', newRouteId);
    expect(localStorage.getItem('dynamicmock:active_route_id')).toBe('route-xyz-789');

    // Simulate clearing active route
    localStorage.removeItem('dynamicmock:active_route_id');
    expect(localStorage.getItem('dynamicmock:active_route_id')).toBeNull();
  });

  it('isolates drafts between different routes', () => {
    const route1Key = 'dynamicmock:test_draft:route-1';
    const route2Key = 'dynamicmock:test_draft:route-2';

    localStorage.setItem(route1Key, JSON.stringify({
      requestHeaders: { 'Route-1-Header': 'val1' },
      queryParams: { q1: 'test1' },
      requestBodyType: 'json',
      requestBodyContent: '{"r1": true}'
    }));

    localStorage.setItem(route2Key, JSON.stringify({
      requestHeaders: { 'Route-2-Header': 'val2' },
      queryParams: { q2: 'test2' },
      requestBodyType: 'raw',
      requestBodyContent: 'plain text'
    }));

    const r1 = JSON.parse(localStorage.getItem(route1Key)!);
    const r2 = JSON.parse(localStorage.getItem(route2Key)!);

    expect(r1.requestHeaders['Route-1-Header']).toBe('val1');
    expect(r2.requestHeaders['Route-2-Header']).toBe('val2');
    expect(r1.requestBodyType).toBe('json');
    expect(r2.requestBodyType).toBe('raw');
  });

  it('extracts and reconstructs URL query parameters (such as returnUrl) across save and relogin', () => {
    const rawUrl = '{{BASE_URL}}/mobile-banking/login?returnUrl=http://localhost/mock/return-redirect';
    const parsed = parseQueryParamsFromUrl(rawUrl);
    expect(parsed).toEqual({
      returnUrl: 'http://localhost/mock/return-redirect',
    });

    const reconstructed = buildUrlWithQueryParams('{{BASE_URL}}/mobile-banking/login', parsed);
    expect(reconstructed).toBe(rawUrl);
  });
});
