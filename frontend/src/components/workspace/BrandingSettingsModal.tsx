'use client';

import React, { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  Palette,
  X,
  Globe,
  RotateCcw,
  Save,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Image as ImageIcon,
} from 'lucide-react';
import { toast } from 'sonner';
import { useBrandingContext } from '@/context/BrandingContext';
import { DEFAULT_BRANDING } from '@/types/branding';
import {
  getContrastForeground,
  isValidAssetUrl,
  isValidFaviconUrl,
  isValidHex,
  normalizeHex,
} from '@/lib/colorUtils';

export interface BrandingSettingsModalProps {
  open: boolean;
  onClose: () => void;
}

export function BrandingSettingsModal({ open, onClose }: BrandingSettingsModalProps) {
  const {
    branding,
    updateBranding,
    resetBranding,
    isUpdating,
    isResetting,
  } = useBrandingContext();

  const [productName, setProductName] = useState(branding.productName);
  const [organizationName, setOrganizationName] = useState(branding.organizationName);
  const [logoUrl, setLogoUrl] = useState(branding.logoUrl ?? '');
  const [faviconUrl, setFaviconUrl] = useState(branding.faviconUrl ?? '/favicon.ico');
  const [primaryColor, setPrimaryColor] = useState(branding.primaryColor);
  const [secondaryColor, setSecondaryColor] = useState(branding.secondaryColor);
  const [footerText, setFooterText] = useState(branding.footerText);
  const [confirmingReset, setConfirmingReset] = useState(false);
  const [logoPreviewError, setLogoPreviewError] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setProductName(branding.productName);
      setOrganizationName(branding.organizationName);
      setLogoUrl(branding.logoUrl ?? '');
      setFaviconUrl(branding.faviconUrl ?? '/favicon.ico');
      setPrimaryColor(branding.primaryColor);
      setSecondaryColor(branding.secondaryColor);
      setFooterText(branding.footerText);
      setConfirmingReset(false);
      setLogoPreviewError(false);
      setValidationError(null);
    }
  }, [open, branding]);

  useEffect(() => {
    setLogoPreviewError(false);
  }, [logoUrl]);

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  const previewPrimary = useMemo(
    () => (isValidHex(primaryColor) ? normalizeHex(primaryColor)! : DEFAULT_BRANDING.primaryColor),
    [primaryColor]
  );
  const previewSecondary = useMemo(
    () =>
      isValidHex(secondaryColor) ? normalizeHex(secondaryColor)! : DEFAULT_BRANDING.secondaryColor,
    [secondaryColor]
  );
  const previewPrimaryForeground = useMemo(
    () => getContrastForeground(previewPrimary),
    [previewPrimary]
  );
  const previewSecondaryForeground = useMemo(
    () => getContrastForeground(previewSecondary),
    [previewSecondary]
  );

  if (!open) return null;

  const validateForm = (): string | null => {
    if (productName.trim().length > 64) {
      return 'Product name must be 64 characters or fewer.';
    }
    if (organizationName.trim().length > 100) {
      return 'Organization name must be 100 characters or fewer.';
    }
    if (!isValidHex(primaryColor)) {
      return 'Primary color must be a valid hex code (e.g. #3b82f6).';
    }
    if (!isValidHex(secondaryColor)) {
      return 'Secondary color must be a valid hex code (e.g. #f1f5f9).';
    }
    if (logoUrl.trim() && !isValidAssetUrl(logoUrl)) {
      return 'Logo URL must start with http://, https://, or data:image/.';
    }
    if (faviconUrl.trim() && !isValidFaviconUrl(faviconUrl)) {
      return 'Favicon URL must be an http(s) URL, data:image/ URI, or local path starting with /.';
    }
    if (footerText.trim().length > 255) {
      return 'Footer text must be 255 characters or fewer.';
    }
    return null;
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    const error = validateForm();
    if (error) {
      setValidationError(error);
      return;
    }
    setValidationError(null);

    try {
      await updateBranding({
        productName: productName.trim() || DEFAULT_BRANDING.productName,
        organizationName: organizationName.trim() || DEFAULT_BRANDING.organizationName,
        logoUrl: logoUrl.trim() || null,
        faviconUrl: faviconUrl.trim() || DEFAULT_BRANDING.faviconUrl,
        primaryColor: normalizeHex(primaryColor) ?? DEFAULT_BRANDING.primaryColor,
        secondaryColor: normalizeHex(secondaryColor) ?? DEFAULT_BRANDING.secondaryColor,
        footerText: footerText.trim() || DEFAULT_BRANDING.footerText,
      });
      toast.success('Branding configuration updated');
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : 'Failed to update branding configuration';
      setValidationError(msg);
      toast.error(msg);
    }
  };

  const handleConfirmReset = async () => {
    setValidationError(null);
    try {
      const reset = await resetBranding();
      setProductName(reset.productName);
      setOrganizationName(reset.organizationName);
      setLogoUrl(reset.logoUrl ?? '');
      setFaviconUrl(reset.faviconUrl ?? '/favicon.ico');
      setPrimaryColor(reset.primaryColor);
      setSecondaryColor(reset.secondaryColor);
      setFooterText(reset.footerText);
      setConfirmingReset(false);
      toast.success('Branding reset to platform defaults');
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to reset branding';
      setValidationError(msg);
      toast.error(msg);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="branding-settings-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-3xl bg-background border border-border rounded-xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-start justify-between p-5 border-b border-border bg-card shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Palette className="h-5 w-5" />
            </div>
            <div>
              <h2 id="branding-settings-modal-title" className="text-base font-semibold">
                Branding &amp; Appearance
              </h2>
              <p className="text-xs text-muted-foreground mt-0.5">
                Customize instance-wide product identity, logos, and color palette
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close branding settings"
            className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSave} className="flex-1 overflow-y-auto p-5 space-y-5">
          {validationError && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2.5 text-xs text-destructive"
            >
              <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
              <span>{validationError}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Product Name */}
            <div className="space-y-1.5">
              <label htmlFor="branding-product-name" className="text-xs font-medium text-foreground">
                Product Name
              </label>
              <input
                id="branding-product-name"
                type="text"
                maxLength={64}
                value={productName}
                onChange={(e) => setProductName(e.target.value)}
                placeholder="Dynamic Mock"
                className="w-full h-9 px-3 text-xs bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
              />
              <p className="text-[10px] text-muted-foreground">
                Shown in the workspace header, login screen, and browser tab ({productName.length}/64)
              </p>
            </div>

            {/* Organization Name */}
            <div className="space-y-1.5">
              <label htmlFor="branding-org-name" className="text-xs font-medium text-foreground">
                Organization Name
              </label>
              <input
                id="branding-org-name"
                type="text"
                maxLength={100}
                value={organizationName}
                onChange={(e) => setOrganizationName(e.target.value)}
                placeholder="Dynamic Mock Open Source"
                className="w-full h-9 px-3 text-xs bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
              />
              <p className="text-[10px] text-muted-foreground">
                Subtitle displayed on the login screen ({organizationName.length}/100)
              </p>
            </div>

            {/* Logo URL */}
            <div className="space-y-1.5">
              <label htmlFor="branding-logo-url" className="text-xs font-medium text-foreground">
                Logo URL
              </label>
              <input
                id="branding-logo-url"
                type="url"
                value={logoUrl}
                onChange={(e) => setLogoUrl(e.target.value)}
                placeholder="https://example.com/logo.svg or data:image/svg+xml;base64,..."
                className="w-full h-9 px-3 text-xs bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary font-mono"
              />
              <div className="flex items-center gap-2 rounded-md border border-border bg-muted/30 px-2.5 py-1.5 text-[11px]">
                <ImageIcon className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                {logoUrl.trim() && isValidAssetUrl(logoUrl) && !logoPreviewError ? (
                  <div className="flex items-center gap-2 min-w-0">
                    <img
                      src={logoUrl.trim()}
                      alt="Logo preview"
                      onError={() => setLogoPreviewError(true)}
                      className="h-5 w-auto max-w-[100px] object-contain"
                    />
                    <span className="text-muted-foreground truncate">Logo preview active</span>
                  </div>
                ) : (
                  <span className="text-muted-foreground">
                    {logoUrl.trim()
                      ? 'Invalid or unreachable image — default icon will be used'
                      : 'No custom logo set — using default brand icon'}
                  </span>
                )}
              </div>
            </div>

            {/* Favicon URL */}
            <div className="space-y-1.5">
              <label htmlFor="branding-favicon-url" className="text-xs font-medium text-foreground">
                Favicon URL
              </label>
              <input
                id="branding-favicon-url"
                type="text"
                value={faviconUrl}
                onChange={(e) => setFaviconUrl(e.target.value)}
                placeholder="/favicon.ico or https://example.com/favicon.png"
                className="w-full h-9 px-3 text-xs bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary font-mono"
              />
              <p className="text-[10px] text-muted-foreground">
                HTTPS URL, data:image/ URI, or local path such as /favicon.ico
              </p>
            </div>

            {/* Primary Color */}
            <div className="space-y-1.5">
              <label htmlFor="branding-primary-color" className="text-xs font-medium text-foreground">
                Primary Brand Color
              </label>
              <div className="flex items-center gap-2">
                <input
                  id="branding-primary-color-picker"
                  aria-label="Primary color picker"
                  type="color"
                  value={previewPrimary}
                  onChange={(e) => setPrimaryColor(e.target.value)}
                  className="h-9 w-11 p-1 bg-accent/20 border border-border rounded-lg cursor-pointer shrink-0"
                />
                <input
                  id="branding-primary-color"
                  type="text"
                  value={primaryColor}
                  onChange={(e) => setPrimaryColor(e.target.value)}
                  placeholder="#3b82f6"
                  className="flex-1 h-9 px-3 text-xs font-mono bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
                />
              </div>
              <p className="text-[10px] text-muted-foreground">
                Auto-calculated text contrast:{' '}
                <code className="font-mono">{previewPrimaryForeground}</code>
              </p>
            </div>

            {/* Secondary Color */}
            <div className="space-y-1.5">
              <label
                htmlFor="branding-secondary-color"
                className="text-xs font-medium text-foreground"
              >
                Secondary Accent Color
              </label>
              <div className="flex items-center gap-2">
                <input
                  id="branding-secondary-color-picker"
                  aria-label="Secondary color picker"
                  type="color"
                  value={previewSecondary}
                  onChange={(e) => setSecondaryColor(e.target.value)}
                  className="h-9 w-11 p-1 bg-accent/20 border border-border rounded-lg cursor-pointer shrink-0"
                />
                <input
                  id="branding-secondary-color"
                  type="text"
                  value={secondaryColor}
                  onChange={(e) => setSecondaryColor(e.target.value)}
                  placeholder="#f1f5f9"
                  className="flex-1 h-9 px-3 text-xs font-mono bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
                />
              </div>
              <p className="text-[10px] text-muted-foreground">
                Secondary surface accent (<code className="font-mono">{previewSecondary}</code>)
              </p>
            </div>
          </div>

          {/* Footer Text */}
          <div className="space-y-1.5">
            <label htmlFor="branding-footer-text" className="text-xs font-medium text-foreground">
              Login &amp; Footer Legal Text
            </label>
            <textarea
              id="branding-footer-text"
              rows={2}
              maxLength={255}
              value={footerText}
              onChange={(e) => setFooterText(e.target.value)}
              placeholder="Dynamic Mock — Open Source Enterprise Integration Simulation Platform"
              className="w-full p-2.5 text-xs bg-accent/20 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary resize-none"
            />
            <p className="text-[10px] text-muted-foreground">
              Displayed at the bottom of the authentication screen ({footerText.length}/255)
            </p>
          </div>

          {/* Live Visual Preview Card */}
          <div
            data-testid="branding-live-preview"
            className="rounded-xl border border-border bg-card p-4 space-y-3"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                Live Visual Preview
              </span>
              <span className="flex items-center gap-1 text-[11px] text-muted-foreground">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
                Luminance-safe contrast active
              </span>
            </div>

            {/* Simulated Workspace Header + Action Buttons */}
            <div className="rounded-lg border border-border bg-background p-3 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                {logoUrl.trim() && isValidAssetUrl(logoUrl) && !logoPreviewError ? (
                  <img
                    src={logoUrl.trim()}
                    alt={productName || 'Brand'}
                    onError={() => setLogoPreviewError(true)}
                    className="h-5 w-auto object-contain"
                  />
                ) : (
                  <div
                    className="p-1.5 rounded-md"
                    style={{
                      backgroundColor: previewPrimary,
                      color: previewPrimaryForeground,
                    }}
                  >
                    <Globe className="h-3.5 w-3.5" />
                  </div>
                )}
                <div>
                  <p className="text-xs font-semibold leading-tight">
                    {productName.trim() || DEFAULT_BRANDING.productName}
                  </p>
                  <p className="text-[10px] text-muted-foreground leading-tight">
                    {organizationName.trim() || DEFAULT_BRANDING.organizationName}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <span
                  data-testid="preview-secondary-badge"
                  className="px-2.5 py-1 rounded-md text-[11px] font-medium border border-border"
                  style={{
                    backgroundColor: previewSecondary,
                    color: previewSecondaryForeground,
                  }}
                >
                  Secondary Badge
                </span>
                <button
                  type="button"
                  data-testid="preview-primary-button"
                  className="px-3 py-1 rounded-md text-xs font-medium shadow-xs"
                  style={{
                    backgroundColor: previewPrimary,
                    color: previewPrimaryForeground,
                  }}
                >
                  Primary Action
                </button>
              </div>
            </div>

            <p className="text-[10px] text-center text-muted-foreground truncate">
              {footerText.trim() || DEFAULT_BRANDING.footerText}
            </p>
          </div>

          {/* Footer Controls */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-border">
            <div>
              {!confirmingReset ? (
                <button
                  type="button"
                  onClick={() => setConfirmingReset(true)}
                  disabled={isUpdating || isResetting}
                  className="flex items-center gap-1.5 px-3 h-8 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-accent rounded-lg border border-border transition-colors disabled:opacity-50"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  <span>Reset to Defaults</span>
                </button>
              ) : (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-destructive font-medium">
                    Restore default branding?
                  </span>
                  <button
                    type="button"
                    onClick={handleConfirmReset}
                    disabled={isResetting}
                    className="px-2.5 h-7 text-xs font-semibold bg-destructive text-destructive-foreground rounded-md hover:bg-destructive/90 transition-colors"
                  >
                    {isResetting ? 'Resetting…' : 'Confirm Reset'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setConfirmingReset(false)}
                    disabled={isResetting}
                    className="px-2.5 h-7 text-xs text-muted-foreground hover:text-foreground rounded-md hover:bg-accent transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              )}
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onClose}
                disabled={isUpdating}
                className="px-3 h-8 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-accent rounded-lg border border-border transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isUpdating}
                className="flex items-center gap-1.5 px-4 h-8 bg-primary text-primary-foreground text-xs font-medium rounded-lg hover:bg-primary/90 transition-colors shadow-xs disabled:opacity-50"
              >
                {isUpdating ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    <span>Saving…</span>
                  </>
                ) : (
                  <>
                    <Save className="h-3.5 w-3.5" />
                    <span>Save Changes</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
