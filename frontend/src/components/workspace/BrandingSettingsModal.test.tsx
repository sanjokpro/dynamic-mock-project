import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrandingSettingsModal } from './BrandingSettingsModal';
import { BrandingContext, BrandingContextValue } from '@/context/BrandingContext';
import { DEFAULT_BRANDING } from '@/types/branding';

describe('BrandingSettingsModal', () => {
  const updateBrandingMock = vi.fn();
  const resetBrandingMock = vi.fn();

  const renderModal = (
    open = true,
    onClose = vi.fn(),
    overrides: Partial<BrandingContextValue> = {}
  ) => {
    const contextValue: BrandingContextValue = {
      branding: { ...DEFAULT_BRANDING },
      isLoading: false,
      isUpdating: false,
      isResetting: false,
      updateBranding: updateBrandingMock,
      resetBranding: resetBrandingMock,
      ...overrides,
    };

    return render(
      <BrandingContext.Provider value={contextValue}>
        <BrandingSettingsModal open={open} onClose={onClose} />
      </BrandingContext.Provider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
    updateBrandingMock.mockResolvedValue({
      ...DEFAULT_BRANDING,
      productName: 'Acme Test Hub',
      primaryColor: '#8b5cf6',
    });
    resetBrandingMock.mockResolvedValue({ ...DEFAULT_BRANDING });
  });

  it('does not render when open is false', () => {
    const { container } = renderModal(false);
    expect(container.firstChild).toBeNull();
  });

  it('renders form fields and live preview card populated with current branding', () => {
    renderModal(true);

    expect(screen.getByRole('dialog')).toBeDefined();
    expect(screen.getByLabelText(/Product Name/i)).toHaveValue('Dynamic Mock');
    expect(screen.getByLabelText(/Organization Name/i)).toHaveValue(
      'Dynamic Mock Open Source'
    );
    expect(screen.getByLabelText(/Primary Brand Color/i)).toHaveValue('#3b82f6');
    expect(screen.getByTestId('branding-live-preview')).toBeDefined();
  });

  it('updates live preview contrast when primary color changes to light yellow', () => {
    renderModal(true);

    const primaryInput = screen.getByLabelText(/Primary Brand Color/i);
    fireEvent.change(primaryInput, { target: { value: '#fef08a' } });

    const previewButton = screen.getByTestId('preview-primary-button');
    // Light yellow (#fef08a) has high luminance -> dark foreground (#0f172a -> rgb(15, 23, 42))
    expect(previewButton.style.backgroundColor).toBe('rgb(254, 240, 138)');
    expect(previewButton.style.color).toBe('rgb(15, 23, 42)');
  });

  it('blocks submission and displays validation error when hex color is malformed', async () => {
    renderModal(true);

    const primaryInput = screen.getByLabelText(/Primary Brand Color/i);
    fireEvent.change(primaryInput, { target: { value: 'not-a-hex' } });

    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveButton);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /Primary color must be a valid hex code/i
    );
    expect(updateBrandingMock).not.toHaveBeenCalled();
  });

  it('submits updated branding configuration on valid form submission', async () => {
    const onClose = vi.fn();
    renderModal(true, onClose);

    fireEvent.change(screen.getByLabelText(/Product Name/i), {
      target: { value: 'Acme Test Hub' },
    });
    fireEvent.change(screen.getByLabelText(/Primary Brand Color/i), {
      target: { value: '#8b5cf6' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Save Changes/i }));

    await waitFor(() => {
      expect(updateBrandingMock).toHaveBeenCalledWith(
        expect.objectContaining({
          productName: 'Acme Test Hub',
          primaryColor: '#8b5cf6',
        })
      );
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('requires confirmation before resetting to defaults and calls resetBranding', async () => {
    const onClose = vi.fn();
    renderModal(true, onClose);

    fireEvent.click(screen.getByRole('button', { name: /Reset to Defaults/i }));
    expect(resetBrandingMock).not.toHaveBeenCalled();

    const confirmBtn = screen.getByRole('button', { name: /Confirm Reset/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(resetBrandingMock).toHaveBeenCalledTimes(1);
      expect(onClose).toHaveBeenCalled();
    });
  });
});
