export interface BrandingConfig {
  productName: string;
  organizationName: string;
  logoUrl: string | null;
  faviconUrl: string | null;
  primaryColor: string;
  secondaryColor: string;
  accentColor?: string | null;
  loginBackgroundUrl?: string | null;
  footerText: string;
  updatedAt?: string | null;
}

export interface BrandingUpdateRequest {
  productName?: string | null;
  organizationName?: string | null;
  logoUrl?: string | null;
  faviconUrl?: string | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  accentColor?: string | null;
  loginBackgroundUrl?: string | null;
  footerText?: string | null;
}

export const DEFAULT_BRANDING: BrandingConfig = {
  productName: 'Dynamic Mock',
  organizationName: 'Dynamic Mock Open Source',
  logoUrl: null,
  faviconUrl: '/favicon.ico',
  primaryColor: '#3b82f6',
  secondaryColor: '#f1f5f9',
  accentColor: '#e2e8f0',
  loginBackgroundUrl: null,
  footerText: 'Dynamic Mock — Open Source Enterprise Integration Simulation Platform',
  updatedAt: null,
};

/**
 * Merges a partial or nullable API response with platform default branding values.
 */
export function normalizeBrandingConfig(partial?: Partial<BrandingConfig> | null): BrandingConfig {
  if (!partial) return { ...DEFAULT_BRANDING };
  return {
    productName: partial.productName?.trim() || DEFAULT_BRANDING.productName,
    organizationName: partial.organizationName?.trim() || DEFAULT_BRANDING.organizationName,
    logoUrl: partial.logoUrl?.trim() ? partial.logoUrl.trim() : null,
    faviconUrl: partial.faviconUrl?.trim() || DEFAULT_BRANDING.faviconUrl,
    primaryColor: partial.primaryColor?.trim() || DEFAULT_BRANDING.primaryColor,
    secondaryColor: partial.secondaryColor?.trim() || DEFAULT_BRANDING.secondaryColor,
    accentColor: partial.accentColor?.trim() || DEFAULT_BRANDING.accentColor,
    loginBackgroundUrl: partial.loginBackgroundUrl?.trim()
      ? partial.loginBackgroundUrl.trim()
      : null,
    footerText: partial.footerText?.trim() || DEFAULT_BRANDING.footerText,
    updatedAt: partial.updatedAt ?? null,
  };
}
