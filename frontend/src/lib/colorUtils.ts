const HEX_REGEX = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
const ASSET_URL_REGEX = /^(https?:\/\/|data:image\/).+/i;
const FAVICON_URL_REGEX = /^(https?:\/\/|data:image\/|\/(?!\/)).+/i;

export interface RgbColor {
  r: number;
  g: number;
  b: number;
}

/**
 * Validates whether a string is a 3-digit or 6-digit hex color (e.g. "#3b82f6" or "#fff").
 * Rejects CSS injection characters such as `;`, `{`, `}`, `(`, `)`.
 */
export function isValidHex(hex: string): boolean {
  if (typeof hex !== 'string') return false;
  return HEX_REGEX.test(hex.trim());
}

/**
 * Validates whether a logo or background URL uses a safe http(s) or data:image/ scheme.
 */
export function isValidAssetUrl(url: string): boolean {
  if (typeof url !== 'string') return false;
  const trimmed = url.trim();
  if (!trimmed) return true;
  return ASSET_URL_REGEX.test(trimmed);
}

/**
 * Validates whether a favicon URL uses http(s), data:image/, or a local path like /favicon.ico.
 */
export function isValidFaviconUrl(url: string): boolean {
  if (typeof url !== 'string') return false;
  const trimmed = url.trim();
  if (!trimmed) return true;
  return FAVICON_URL_REGEX.test(trimmed);
}

/**
 * Normalizes a 3-digit or 6-digit hex string to a lowercase 6-digit `#rrggbb` string,
 * or returns null if invalid.
 */
export function normalizeHex(hex: string): string | null {
  if (!isValidHex(hex)) return null;
  const clean = hex.trim().slice(1).toLowerCase();
  if (clean.length === 3) {
    return `#${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}`;
  }
  return `#${clean}`;
}

/**
 * Parses a hex color string (#RGB or #RRGGBB) into its RGB components (0-255).
 * Returns null if the input is not a valid hex color.
 */
export function hexToRgb(hex: string): RgbColor | null {
  const normalized = normalizeHex(hex);
  if (!normalized) return null;

  const value = normalized.slice(1);
  const r = parseInt(value.slice(0, 2), 16);
  const g = parseInt(value.slice(2, 4), 16);
  const b = parseInt(value.slice(4, 6), 16);

  return { r, g, b };
}

/**
 * Formats a hex color string as `"R, G, B"` suitable for `--primary-rgb` CSS variables.
 * Falls back to `"59, 130, 246"` (default #3b82f6) if the input is invalid.
 */
export function hexToRgbString(hex: string): string {
  const rgb = hexToRgb(hex);
  if (!rgb) return '59, 130, 246';
  return `${rgb.r}, ${rgb.g}, ${rgb.b}`;
}

/**
 * Calculates perceived luminance (0-255) using the standard ITU-R BT.601 formula:
 * `0.299 * R + 0.587 * G + 0.114 * B`
 */
export function getLuminance(hex: string): number {
  const rgb = hexToRgb(hex);
  if (!rgb) return 0;
  return 0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b;
}

/**
 * Returns a high-contrast foreground hex color (`#0f172a` dark slate for light backgrounds,
 * `#ffffff` white for dark/medium backgrounds) based on the luminance threshold of 160.
 */
export function getContrastForeground(hex: string): string {
  const luminance = getLuminance(hex);
  return luminance > 160 ? '#0f172a' : '#ffffff';
}

/**
 * Synthesizes `:root` and `.dark` CSS custom property declarations for a given primary
 * and secondary brand color. Strictly validates hex inputs to prevent CSS injection.
 */
export function buildBrandingCssVariables(
  primaryColor?: string | null,
  secondaryColor?: string | null
): string {
  const safePrimary =
    primaryColor && isValidHex(primaryColor) ? normalizeHex(primaryColor)! : '#3b82f6';
  const safeSecondary =
    secondaryColor && isValidHex(secondaryColor) ? normalizeHex(secondaryColor)! : '#f1f5f9';

  const primaryRgb = hexToRgbString(safePrimary);
  const primaryForeground = getContrastForeground(safePrimary);
  const isCustomSecondary = safeSecondary.toLowerCase() !== '#f1f5f9';
  const secondaryForeground = getContrastForeground(safeSecondary);

  const darkSecondaryRules = isCustomSecondary
    ? `  --secondary: ${safeSecondary};\n  --secondary-foreground: ${secondaryForeground};\n`
    : '';

  return [
    ':root {',
    `  --primary: ${safePrimary};`,
    `  --primary-rgb: ${primaryRgb};`,
    `  --primary-foreground: ${primaryForeground};`,
    `  --ring: ${safePrimary};`,
    `  --secondary: ${safeSecondary};`,
    `  --secondary-foreground: ${secondaryForeground};`,
    '}',
    '.dark {',
    `  --primary: ${safePrimary};`,
    `  --primary-rgb: ${primaryRgb};`,
    `  --primary-foreground: ${primaryForeground};`,
    `  --ring: ${safePrimary};`,
    darkSecondaryRules ? darkSecondaryRules.trimEnd() : null,
    '}',
  ]
    .filter((line): line is string => line !== null)
    .join('\n');
}
