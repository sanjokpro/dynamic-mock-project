export interface CurlOptions {
  method: string;
  url: string;
  headers?: Record<string, string>;
  body?: string;
  variables?: Record<string, string>;
  baseOrigin?: string;
}

/**
 * Internal headers that should never appear in exported cURL commands.
 * These are used for internal mock-server routing and are meaningless
 * when pasted into a terminal.
 */
const INTERNAL_HEADER_PREFIXES = ['x-dynamic-mock-'];

/**
 * Checks if a header value looks like a regex pattern rather than
 * a concrete test value. Patterns like `.*`, `^foo$`, `\d+`, etc.
 * are matcher patterns that won't work as real header values.
 */
function isRegexPattern(value: string): boolean {
  if (!value) return false;
  const regexIndicators = /^(\.\*|\^.*\$|.*[+*?{}()|\\[\]].*)$/;
  return regexIndicators.test(value.trim());
}

/**
 * Generates a sensible default value for a regex-pattern header.
 * Maps common patterns to meaningful test values.
 */
function sanitizeHeaderValue(key: string, value: string): string {
  if (!isRegexPattern(value)) return value;
  // `.*` means "anything" — use a sensible default based on the header name
  if (value.trim() === '.*') return 'test';
  // For other regex patterns, strip anchors and special chars to extract a hint
  const stripped = value.replace(/[\\^$.*+?(){}[\]|]/g, '').trim();
  return stripped || 'test';
}

/**
 * Resolves placeholders like {{BASE_URL}} or environment variables in URLs.
 * Mimics Postman's automatic variable resolution so the cURL command
 * is immediately terminal-executable without modification.
 *
 * KEY DESIGN DECISION: The mock server is always mounted at the `/mock`
 * path prefix behind nginx. If an environment variable resolves BASE_URL
 * to a bare origin (e.g. `http://localhost`), we ensure `/mock` is appended
 * so the resulting cURL hits the correct endpoint.
 */
export function resolveCurlUrl(
  url: string,
  variables?: Record<string, string>,
  baseOrigin?: string
): string {
  let resolved = url || '';

  // Handle {{BASE_URL}} with special treatment:
  // 1. Check environment variables for BASE_URL first
  // 2. Fall back to baseOrigin or window.location.origin + '/mock'
  // 3. Always ensure the resolved base includes /mock
  if (resolved.includes('{{BASE_URL}}')) {
    let base: string | undefined;

    // Check if environment has a BASE_URL variable
    if (variables && typeof variables === 'object' && variables['BASE_URL']) {
      base = variables['BASE_URL'];
    }

    // Fall back to explicit baseOrigin or auto-detected origin
    if (!base) {
      base =
        baseOrigin ||
        (typeof window !== 'undefined'
          ? `${window.location.origin}/mock`
          : 'http://localhost/mock');
    }

    // Ensure the base ends with /mock (or contains it)
    // If someone set BASE_URL to 'http://localhost' we need to append /mock
    const baseUrl = new URL(base.startsWith('http') ? base : `http://${base}`);
    if (!baseUrl.pathname.includes('/mock')) {
      base = `${base.replace(/\/+$/, '')}/mock`;
    }

    resolved = resolved.replace(/\{\{BASE_URL\}\}/g, base.replace(/\/+$/, ''));
  }

  // Resolve remaining custom environment variables (e.g. {{HOST}}, {{PORT}})
  // but NOT BASE_URL since we handled it above
  if (variables && typeof variables === 'object') {
    for (const [key, val] of Object.entries(variables)) {
      if (key && key !== 'BASE_URL' && val !== undefined) {
        resolved = resolved.replace(new RegExp(`\\{\\{${key}\\}\\}`, 'g'), val);
      }
    }
  }

  // If URL is a relative path (e.g. "/mock/foo" or "/foo"), prepend origin
  if (resolved.startsWith('/')) {
    let origin =
      baseOrigin ||
      (typeof window !== 'undefined'
        ? window.location.origin
        : 'http://localhost');
    origin = origin.replace(/\/+$/, '');
    if (origin.endsWith('/mock')) {
      origin = origin.slice(0, -5);
    }
    if (resolved.startsWith('/mock/')) {
      resolved = `${origin}${resolved}`;
    } else {
      resolved = `${origin}/mock${resolved}`;
    }
  }

  // Final safety: if URL is absolute but path doesn't contain /mock, inject it
  if (resolved.startsWith('http')) {
    try {
      const parsed = new URL(resolved);
      if (!parsed.pathname.startsWith('/mock')) {
        parsed.pathname = `/mock${parsed.pathname}`;
        resolved = parsed.toString().replace(/\/$/, '');
      }
    } catch {
      // URL parsing failed, leave as-is
    }
  }

  return resolved;
}

/**
 * Generates a terminal-ready cURL command string with multi-line formatting,
 * environment variable resolution, and shell single-quote escaping.
 *
 * Like Postman, this function:
 * - Resolves all {{variables}} to concrete values
 * - Excludes internal routing headers (X-Dynamic-Mock-*)
 * - Replaces regex-pattern header values with sensible defaults
 * - Properly escapes single quotes for shell execution
 */
export function generateCurlCommand(options: CurlOptions): string {
  const { method = 'GET', url = '', headers = {}, body, variables, baseOrigin } = options;
  const upperMethod = (method || 'GET').toUpperCase();

  const resolvedUrl = resolveCurlUrl(url, variables, baseOrigin);

  const lines: string[] = [];
  const escapedUrl = resolvedUrl.replace(/'/g, "'\\''");
  lines.push(`curl -X ${upperMethod} '${escapedUrl}'`);

  // Add headers in deterministic order with variable substitution
  // Skip internal headers and sanitize regex patterns
  for (const [key, value] of Object.entries(headers)) {
    if (!key.trim()) continue;

    // Skip internal headers that are meaningless outside the app
    if (INTERNAL_HEADER_PREFIXES.some(prefix => key.toLowerCase().startsWith(prefix))) {
      continue;
    }

    let resolvedValue = sanitizeHeaderValue(key, value);

    // Resolve environment variables in header values
    if (variables && typeof variables === 'object') {
      for (const [vKey, vVal] of Object.entries(variables)) {
        if (vKey && vVal !== undefined) {
          resolvedValue = resolvedValue.replace(new RegExp(`\\{\\{${vKey}\\}\\}`, 'g'), vVal);
        }
      }
    }
    const escapedHeader = `${key}: ${resolvedValue}`.replace(/'/g, "'\\''");
    lines.push(`  -H '${escapedHeader}'`);
  }

  // Omit body for GET and HEAD requests
  if (body !== undefined && body !== null && body !== '' && upperMethod !== 'GET' && upperMethod !== 'HEAD') {
    let resolvedBody = body;
    if (variables && typeof variables === 'object') {
      for (const [vKey, vVal] of Object.entries(variables)) {
        if (vKey && vVal !== undefined) {
          resolvedBody = resolvedBody.replace(new RegExp(`\\{\\{${vKey}\\}\\}`, 'g'), vVal);
        }
      }
    }
    const escapedBody = resolvedBody.replace(/'/g, "'\\''");
    lines.push(`  --data '${escapedBody}'`);
  }

  return lines.join(' \\\n');
}

/**
 * Copies text to clipboard with fallback for non-HTTPS development environments.
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  if (typeof window === 'undefined') return false;

  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // Fall back to document.execCommand
  }

  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    textarea.style.top = '-9999px';
    textarea.setAttribute('readonly', '');
    document.body.appendChild(textarea);
    textarea.select();
    const successful = document.execCommand('copy');
    document.body.removeChild(textarea);
    return successful;
  } catch {
    return false;
  }
}
