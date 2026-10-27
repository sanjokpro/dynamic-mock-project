import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrandingProvider } from './BrandingProvider';
import { useBrandingContext } from './BrandingContext';
import apiClient from '@/lib/apiClient';

vi.mock('@/lib/apiClient', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  },
}));

function ConsumerProbe() {
  const { branding } = useBrandingContext();
  return <div data-testid="brand-name">{branding.productName}</div>;
}

describe('BrandingProvider', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
    document.documentElement.removeAttribute('style');
    const existing = document.getElementById('dynamic-branding-style');
    existing?.remove();
  });

  it('injects CSS custom properties, style element, document title, and favicon on mount', async () => {
    (apiClient.get as any).mockResolvedValue({
      data: {
        productName: 'FinTech Sandbox',
        organizationName: 'FinTech Corp',
        logoUrl: null,
        faviconUrl: '/custom-favicon.ico',
        primaryColor: '#7c3aed',
        secondaryColor: '#f1f5f9',
        footerText: 'FinTech Footer',
      },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <BrandingProvider
          initialBranding={{
            productName: 'FinTech Sandbox',
            organizationName: 'FinTech Corp',
            logoUrl: null,
            faviconUrl: '/custom-favicon.ico',
            primaryColor: '#7c3aed',
            secondaryColor: '#f1f5f9',
            footerText: 'FinTech Footer',
          }}
        >
          <ConsumerProbe />
        </BrandingProvider>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('brand-name').textContent).toBe('FinTech Sandbox');
    });

    const root = document.documentElement;
    expect(root.style.getPropertyValue('--primary')).toBe('#7c3aed');
    expect(root.style.getPropertyValue('--primary-rgb')).toBe('124, 58, 237');
    expect(root.style.getPropertyValue('--primary-foreground')).toBe('#ffffff');
    expect(root.style.getPropertyValue('--ring')).toBe('#7c3aed');
    expect(root.style.getPropertyValue('--secondary')).toBe('#f1f5f9');

    const dynamicStyle = document.getElementById('dynamic-branding-style');
    expect(dynamicStyle).not.toBeNull();
    expect(dynamicStyle?.textContent).toContain('--primary: #7c3aed;');
    expect(document.title).toBe('FinTech Sandbox - API Workspace');
  });
});
