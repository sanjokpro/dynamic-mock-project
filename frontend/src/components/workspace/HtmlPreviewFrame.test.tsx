import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { HtmlPreviewFrame } from './HtmlPreviewFrame';

describe('HtmlPreviewFrame', () => {
  const sampleHtml = '<!DOCTYPE html><html><head></head><body><h1 id="title">Secure Sign In</h1></body></html>';

  it('enforces interactive sandbox invariants and referrerPolicy="no-referrer" by default', () => {
    render(
      <HtmlPreviewFrame
        html={sampleHtml}
        mockPath="/mobile-banking-login"
        targetUrl="http://localhost:8080/mock/mobile-banking-login?returnUrl=http://localhost/mock/dest"
      />
    );

    const iframe = screen.getByTestId('html-preview-iframe') as HTMLIFrameElement;
    expect(iframe).toBeTruthy();

    const sandboxAttr = iframe.getAttribute('sandbox') || '';
    expect(sandboxAttr).toBe('allow-scripts allow-forms allow-popups');
    expect(sandboxAttr).not.toContain('allow-same-origin');
    expect(sandboxAttr).not.toContain('allow-top-navigation');

    expect(iframe.getAttribute('referrerpolicy') || iframe.referrerPolicy).toBe('no-referrer');
    expect(iframe.getAttribute('src')).toBeNull();
    expect(iframe.getAttribute('srcdoc')).toContain('Secure Sign In');
    expect(iframe.getAttribute('srcdoc')).toContain('window.__DYNAMIC_MOCK_CONTEXT__');

    expect(screen.getByTestId('html-preview-mode-badge').textContent).toContain('Interactive');
    expect(screen.getByTestId('html-preview-path').textContent).toBe(
      '/mock/mobile-banking-login?returnUrl=http://localhost/mock/dest'
    );
  });

  it('enforces empty static sandbox="" when interactive={false}', () => {
    render(
      <HtmlPreviewFrame
        html={sampleHtml}
        mockPath="/mobile-banking-login"
        interactive={false}
        compact={true}
      />
    );

    const iframe = screen.getByTestId('html-preview-iframe') as HTMLIFrameElement;
    expect(iframe.getAttribute('sandbox')).toBe('');
    expect(iframe.getAttribute('sandbox')).not.toContain('allow-scripts');
    expect(iframe.getAttribute('sandbox')).not.toContain('allow-same-origin');
    expect(screen.getByTestId('html-preview-mode-badge').textContent).toContain('Static');
  });

  it('remounts the iframe when Reload Preview is clicked', () => {
    render(<HtmlPreviewFrame html={sampleHtml} mockPath="/mobile-banking-login" />);

    const initialIframe = screen.getByTestId('html-preview-iframe');
    expect(initialIframe.getAttribute('data-reload-key')).toBe('0');

    const reloadBtn = screen.getByTestId('html-preview-reload');
    fireEvent.click(reloadBtn);

    const reloadedIframe = screen.getByTestId('html-preview-iframe');
    expect(reloadedIframe.getAttribute('data-reload-key')).toBe('1');
  });

  it('opens the target mock URL in a new tab with noopener,noreferrer when Open in New Tab is clicked', () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    render(
      <HtmlPreviewFrame
        html={sampleHtml}
        mockPath="/mobile-banking-login"
        targetUrl="http://localhost:8080/mock/mobile-banking-login?returnUrl=http://localhost/mock/dest"
      />
    );

    const openTabBtn = screen.getByTestId('html-preview-open-tab');
    fireEvent.click(openTabBtn);

    expect(openSpy).toHaveBeenCalledWith(
      'http://localhost:8080/mock/mobile-banking-login?returnUrl=http://localhost/mock/dest',
      '_blank',
      'noopener,noreferrer'
    );

    openSpy.mockRestore();
  });
});
