'use client';

import React, { useEffect } from 'react';
import { BrandingContext } from './BrandingContext';
import { useBranding } from '@/hooks/useBranding';
import { BrandingConfig, normalizeBrandingConfig } from '@/types/branding';
import {
  buildBrandingCssVariables,
  getContrastForeground,
  hexToRgbString,
  isValidHex,
  normalizeHex,
} from '@/lib/colorUtils';

export interface BrandingProviderProps {
  children: React.ReactNode;
  initialBranding?: Partial<BrandingConfig> | null;
}

const DYNAMIC_STYLE_ID = 'dynamic-branding-style';

/**
 * Applies instance branding CSS custom properties, document title, and favicon
 * reactively to the browser DOM whenever the effective branding changes.
 */
export function applyBrandingToDocument(branding: BrandingConfig): void {
  if (typeof document === 'undefined') return;

  const primary =
    branding.primaryColor && isValidHex(branding.primaryColor)
      ? normalizeHex(branding.primaryColor)!
      : '#3b82f6';
  const secondary =
    branding.secondaryColor && isValidHex(branding.secondaryColor)
      ? normalizeHex(branding.secondaryColor)!
      : '#f1f5f9';
  const primaryRgb = hexToRgbString(primary);
  const primaryForeground = getContrastForeground(primary);

  const root = document.documentElement;
  root.style.setProperty('--primary', primary);
  root.style.setProperty('--primary-rgb', primaryRgb);
  root.style.setProperty('--primary-foreground', primaryForeground);
  root.style.setProperty('--ring', primary);
  root.style.setProperty('--secondary', secondary);

  let styleEl = document.getElementById(DYNAMIC_STYLE_ID) as HTMLStyleElement | null;
  if (!styleEl) {
    styleEl = document.createElement('style');
    styleEl.id = DYNAMIC_STYLE_ID;
    document.head.appendChild(styleEl);
  }
  styleEl.textContent = buildBrandingCssVariables(primary, secondary);

  if (branding.productName) {
    document.title = `${branding.productName} - API Workspace`;
  }

  if (branding.faviconUrl) {
    let iconLink = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (!iconLink) {
      iconLink = document.createElement('link');
      iconLink.rel = 'icon';
      document.head.appendChild(iconLink);
    }
    if (iconLink.getAttribute('href') !== branding.faviconUrl) {
      iconLink.setAttribute('href', branding.faviconUrl);
    }
  }
}

export function BrandingProvider({ children, initialBranding }: BrandingProviderProps) {
  const normalizedInitial = initialBranding
    ? normalizeBrandingConfig(initialBranding)
    : undefined;

  const {
    branding,
    isLoading,
    isUpdating,
    isResetting,
    updateBranding,
    resetBranding,
  } = useBranding({ initialData: normalizedInitial });

  useEffect(() => {
    applyBrandingToDocument(branding);
  }, [branding]);

  return (
    <BrandingContext.Provider
      value={{
        branding,
        isLoading,
        isUpdating,
        isResetting,
        updateBranding,
        resetBranding,
      }}
    >
      {children}
    </BrandingContext.Provider>
  );
}
