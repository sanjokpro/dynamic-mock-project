import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BankingSandboxModal } from './BankingSandboxModal';

describe('BankingSandboxModal', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('does not render when open is false', () => {
    const { container } = render(
      <BankingSandboxModal open={false} onClose={() => {}} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal dialog and transaction steps when open is true', () => {
    render(
      <BankingSandboxModal open={true} onClose={() => {}} />
    );

    expect(screen.getByRole('dialog')).toBeDefined();
    expect(screen.getByText('Reference Banking Sandbox')).toBeDefined();
    expect(screen.getByText('0100 Authorization')).toBeDefined();
    expect(screen.getByText('0200 Financial Presentment')).toBeDefined();
    expect(screen.getByText('0400 Acquirer Reversal')).toBeDefined();
    expect(screen.getByText('State Machine Inspector')).toBeDefined();
    expect(screen.getByRole('button', { name: /Load \/ Reset Banking Sandbox/i })).toBeDefined();
  });

  it('triggers POST /api/sandbox/load on button click and invokes callbacks', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      text: vi.fn().mockResolvedValue('ok')
    });
    global.fetch = fetchMock;

    const onClose = vi.fn();
    const onLoadSuccess = vi.fn();

    render(
      <BankingSandboxModal
        open={true}
        onClose={onClose}
        onLoadSuccess={onLoadSuccess}
      />
    );

    const loadBtn = screen.getByRole('button', { name: /Load \/ Reset Banking Sandbox/i });
    fireEvent.click(loadBtn);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/api/sandbox/load', { method: 'POST' });
      expect(onLoadSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('closes on Escape key press', () => {
    const onClose = vi.fn();
    render(<BankingSandboxModal open={true} onClose={onClose} />);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});
