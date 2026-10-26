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

  it('renders modal dialog with guidance sections, steps, and distinction banner', () => {
    render(
      <BankingSandboxModal open={true} onClose={() => {}} />
    );

    expect(screen.getByRole('dialog')).toBeDefined();
    expect(screen.getAllByText('Banking Demo Sandbox').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Optional Demo')).toBeDefined();
    expect(screen.getByText('Regular REST Mocking')).toBeDefined();
    expect(screen.getByText('What is this?')).toBeDefined();
    expect(screen.getByText('Who should use this?')).toBeDefined();
    expect(screen.getByText('0100 Authorization')).toBeDefined();
    expect(screen.getByText('0200 Financial Presentment')).toBeDefined();
    expect(screen.getByText('0400 Acquirer Reversal')).toBeDefined();
    expect(screen.getByText('State Machine Inspector')).toBeDefined();
    expect(screen.getByRole('button', { name: /Continue with REST Mocking/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Load \/ Reset Banking Demo Sandbox/i })).toBeDefined();
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

    const loadBtn = screen.getByRole('button', { name: /Load \/ Reset Banking Demo Sandbox/i });
    fireEvent.click(loadBtn);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/api/sandbox/load', { method: 'POST' });
      expect(onLoadSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('invokes onClose when "Continue with REST Mocking" is clicked', () => {
    const onClose = vi.fn();
    render(<BankingSandboxModal open={true} onClose={onClose} />);

    const continueBtn = screen.getByRole('button', { name: /Continue with REST Mocking/i });
    fireEvent.click(continueBtn);
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes on Escape key press', () => {
    const onClose = vi.fn();
    render(<BankingSandboxModal open={true} onClose={onClose} />);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});
