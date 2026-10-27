'use client';

import { createContext, useContext } from 'react';
import {
  BrandingConfig,
  BrandingUpdateRequest,
  DEFAULT_BRANDING,
} from '@/types/branding';

export interface BrandingContextValue {
  branding: BrandingConfig;
  isLoading: boolean;
  isUpdating: boolean;
  isResetting: boolean;
  updateBranding: (request: BrandingUpdateRequest) => Promise<BrandingConfig>;
  resetBranding: () => Promise<BrandingConfig>;
}

export const BrandingContext = createContext<BrandingContextValue>({
  branding: DEFAULT_BRANDING,
  isLoading: false,
  isUpdating: false,
  isResetting: false,
  updateBranding: async () => DEFAULT_BRANDING,
  resetBranding: async () => DEFAULT_BRANDING,
});

export function useBrandingContext(): BrandingContextValue {
  return useContext(BrandingContext);
}
