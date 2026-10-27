import { describe, it, expect } from 'vitest';
import {
  hexToRgb,
  hexToRgbString,
  getLuminance,
  getContrastForeground,
  isValidHex,
  isValidAssetUrl,
  isValidFaviconUrl,
  normalizeHex,
  buildBrandingCssVariables,
} from './colorUtils';

describe('colorUtils', () => {
  describe('isValidHex', () => {
    it('accepts valid 6-digit and 3-digit hex codes', () => {
      expect(isValidHex('#3b82f6')).toBe(true);
      expect(isValidHex('#FFFFFF')).toBe(true);
      expect(isValidHex('#000')).toBe(true);
      expect(isValidHex('#abc')).toBe(true);
    });

    it('rejects malformed hex strings and CSS injection payloads', () => {
      expect(isValidHex('3b82f6')).toBe(false);
      expect(isValidHex('#12345')).toBe(false);
      expect(isValidHex('#1234567')).toBe(false);
      expect(isValidHex('#gggggg')).toBe(false);
      expect(isValidHex('#3b82f6; background: url(javascript:alert(1))')).toBe(false);
      expect(isValidHex('rgb(0,0,0)')).toBe(false);
      expect(isValidHex('')).toBe(false);
    });
  });

  describe('normalizeHex', () => {
    it('expands 3-digit hex codes to 6-digit lowercase', () => {
      expect(normalizeHex('#ABC')).toBe('#aabbcc');
      expect(normalizeHex('#3B82F6')).toBe('#3b82f6');
      expect(normalizeHex('invalid')).toBeNull();
    });
  });

  describe('hexToRgb & hexToRgbString', () => {
    it('converts 6-digit and 3-digit hex strings to RGB', () => {
      expect(hexToRgb('#3b82f6')).toEqual({ r: 59, g: 130, b: 246 });
      expect(hexToRgb('#ffffff')).toEqual({ r: 255, g: 255, b: 255 });
      expect(hexToRgb('#000')).toEqual({ r: 0, g: 0, b: 0 });
    });

    it('returns null for invalid hex strings in hexToRgb', () => {
      expect(hexToRgb('not-a-color')).toBeNull();
    });

    it('formats RGB string for CSS variables with safe fallback', () => {
      expect(hexToRgbString('#3b82f6')).toBe('59, 130, 246');
      expect(hexToRgbString('#0052cc')).toBe('0, 82, 204');
      expect(hexToRgbString('invalid')).toBe('59, 130, 246');
    });
  });

  describe('getLuminance & getContrastForeground', () => {
    it('computes luminance using 0.299R + 0.587G + 0.114B', () => {
      expect(getLuminance('#000000')).toBeCloseTo(0, 2);
      expect(getLuminance('#ffffff')).toBeCloseTo(255, 2);
      expect(getLuminance('#3b82f6')).toBeCloseTo(0.299 * 59 + 0.587 * 130 + 0.114 * 246, 2);
    });

    it('returns dark foreground (#0f172a) for light colors (#ffffff, #fef08a)', () => {
      expect(getContrastForeground('#ffffff')).toBe('#0f172a');
      expect(getContrastForeground('#fef08a')).toBe('#0f172a');
      expect(getContrastForeground('#f1f5f9')).toBe('#0f172a');
    });

    it('returns white foreground (#ffffff) for dark and medium colors (#000000, #3b82f6, #7c3aed)', () => {
      expect(getContrastForeground('#000000')).toBe('#ffffff');
      expect(getContrastForeground('#3b82f6')).toBe('#ffffff');
      expect(getContrastForeground('#7c3aed')).toBe('#ffffff');
    });
  });

  describe('URL validators', () => {
    it('accepts https, http, and data:image/ URLs and rejects javascript: URLs', () => {
      expect(isValidAssetUrl('https://cdn.example.com/logo.svg')).toBe(true);
      expect(isValidAssetUrl('data:image/svg+xml;base64,PHN2Zz4=')).toBe(true);
      expect(isValidAssetUrl('javascript:alert(1)')).toBe(false);
      expect(isValidAssetUrl('data:text/html,<script>alert(1)</script>')).toBe(false);
      expect(isValidFaviconUrl('/favicon.ico')).toBe(true);
      expect(isValidFaviconUrl('//evil.example.com/icon.ico')).toBe(false);
    });
  });

  describe('buildBrandingCssVariables', () => {
    it('builds CSS custom properties for :root and .dark', () => {
      const css = buildBrandingCssVariables('#7c3aed', '#f1f5f9');
      expect(css).toContain('--primary: #7c3aed;');
      expect(css).toContain('--primary-rgb: 124, 58, 237;');
      expect(css).toContain('--primary-foreground: #ffffff;');
      expect(css).toContain('--ring: #7c3aed;');
      expect(css).toContain('--secondary: #f1f5f9;');
    });
  });
});
