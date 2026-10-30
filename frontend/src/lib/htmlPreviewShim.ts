export interface HtmlPreviewContext {
  targetUrl?: string;
  mockPath?: string;
  origin?: string;
}

export interface ResolvedPreviewContext {
  targetUrl: string;
  mockPath: string;
  origin: string;
  baseHref: string;
}

export function resolvePreviewContext(options?: HtmlPreviewContext): ResolvedPreviewContext {
  const defaultOrigin =
    typeof window !== 'undefined' && window.location?.origin && window.location.origin !== 'null'
      ? window.location.origin
      : 'http://localhost:8080';

  const origin = (options?.origin || defaultOrigin).replace(/\/+$/, '');

  let mockPath = options?.mockPath || '/';
  if (mockPath.startsWith('/mock/')) {
    mockPath = mockPath.slice('/mock'.length);
  } else if (mockPath === '/mock') {
    mockPath = '/';
  }
  if (!mockPath.startsWith('/')) {
    mockPath = '/' + mockPath;
  }

  const baseHref = `${origin}/mock/`;
  const defaultTargetUrl = `${origin}/mock${mockPath === '/' ? '' : mockPath}`;
  const targetUrl = options?.targetUrl?.trim() ? options.targetUrl.trim() : defaultTargetUrl;

  return {
    targetUrl,
    mockPath,
    origin,
    baseHref,
  };
}

/**
 * Pure URL normalizer used both in unit tests and serialized into the surgical srcdoc URL shim.
 * Resolves srcdoc pathname artifacts, query-only relative URLs, /mock/... paths, and relative paths
 * without mutating unrelated URLs.
 */
export function normalizeMockPreviewUrl(
  rawUrl: string,
  context: { targetUrl?: string; mockPath?: string; origin?: string }
): string {
  if (!rawUrl || typeof rawUrl !== 'string') {
    return rawUrl;
  }

  const trimmed = rawUrl.trim();
  if (
    trimmed.startsWith('http://') ||
    trimmed.startsWith('https://') ||
    trimmed.startsWith('data:') ||
    trimmed.startsWith('blob:') ||
    trimmed.startsWith('mailto:')
  ) {
    return trimmed;
  }

  const resolved = resolvePreviewContext(context);
  const endpointBase = `${resolved.origin}/mock${resolved.mockPath === '/' ? '' : resolved.mockPath}`;

  // Handle window.location.pathname in srcdoc ("srcdoc?..." / "/srcdoc?..." / "about:srcdoc?...")
  const srcdocPrefixMatch = trimmed.match(/^(?:about:srcdoc|\/?srcdoc)(\?.*)?$/i);
  if (srcdocPrefixMatch) {
    const querySuffix = srcdocPrefixMatch[1] || '';
    return `${endpointBase}${querySuffix}`;
  }

  // Handle query-only URLs ("?action=login&returnUrl=...")
  if (trimmed.startsWith('?')) {
    return `${endpointBase}${trimmed}`;
  }

  // Handle paths already starting with /mock/
  if (trimmed.startsWith('/mock/') || trimmed === '/mock') {
    return `${resolved.origin}${trimmed}`;
  }

  // Handle root-relative paths ("/mobile-banking-login?action=login")
  if (trimmed.startsWith('/')) {
    return `${resolved.origin}/mock${trimmed}`;
  }

  // Handle relative paths ("submit" or "./submit")
  const cleanRelative = trimmed.replace(/^\.\//, '');
  return `${resolved.baseHref}${cleanRelative}`;
}

/**
 * Builds a sandboxed srcdoc HTML payload using the approved 3-tier strategy:
 * 1. <base href="{origin}/mock/" /> for declarative relative links/assets
 * 2. window.__DYNAMIC_MOCK_CONTEXT__ metadata object
 * 3. Surgical URL normalization shim for relative / srcdoc fetch & XHR URLs
 *
 * Preserves zero double execution by rendering existing response content directly via srcdoc.
 */
export function buildSandboxedSrcDoc(rawHtml: string, options?: HtmlPreviewContext): string {
  const content = rawHtml ?? '';
  const ctx = resolvePreviewContext(options);

  const serializedContext = JSON.stringify({
    targetUrl: ctx.targetUrl,
    mockPath: ctx.mockPath,
    origin: ctx.origin,
    baseHref: ctx.baseHref,
  });

  const escapeBaseHref = ctx.baseHref.replace(/"/g, '&quot;');

  const injectionBlock = `<base href="${escapeBaseHref}" />
<script data-dynamic-mock-preview-shim="true">
(function() {
  var ctx = ${serializedContext};
  window.__DYNAMIC_MOCK_CONTEXT__ = Object.freeze(ctx);
  var endpointBase = ctx.origin + '/mock' + (ctx.mockPath === '/' ? '' : ctx.mockPath);

  function resolveMockUrl(input) {
    if (!input || typeof input !== 'string') return input;
    var trimmed = input.trim();
    if (/^(https?:|data:|blob:|mailto:)/i.test(trimmed)) return trimmed;
    var srcdocMatch = trimmed.match(/^(?:about:srcdoc|\\/?srcdoc)(\\?.*)?$/i);
    if (srcdocMatch) {
      return endpointBase + (srcdocMatch[1] || '');
    }
    if (trimmed.charAt(0) === '?') {
      return endpointBase + trimmed;
    }
    if (trimmed.indexOf('/mock/') === 0 || trimmed === '/mock') {
      return ctx.origin + trimmed;
    }
    if (trimmed.charAt(0) === '/') {
      return ctx.origin + '/mock' + trimmed;
    }
    return ctx.baseHref + trimmed.replace(/^\\.\\//, '');
  }

  window.resolveMockUrl = resolveMockUrl;

  if (typeof window.fetch === 'function') {
    var nativeFetch = window.fetch.bind(window);
    window.fetch = function(input, init) {
      if (typeof input === 'string') {
        return nativeFetch(resolveMockUrl(input), init);
      }
      return nativeFetch(input, init);
    };
  }

  if (typeof XMLHttpRequest !== 'undefined' && XMLHttpRequest.prototype && XMLHttpRequest.prototype.open) {
    var nativeOpen = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function(method, url) {
      var args = Array.prototype.slice.call(arguments);
      if (typeof url === 'string') {
        args[1] = resolveMockUrl(url);
      }
      return nativeOpen.apply(this, args);
    };
  }
})();
</script>`;

  // Inject right after <head> if present
  if (/<head(\s[^>]*)?>/i.test(content)) {
    return content.replace(/<head(\s[^>]*)?>/i, (match) => `${match}\n${injectionBlock}`);
  }

  // Inject inside <html> if present without <head>
  if (/<html(\s[^>]*)?>/i.test(content)) {
    return content.replace(/<html(\s[^>]*)?>/i, (match) => `${match}\n<head>\n${injectionBlock}\n</head>`);
  }

  // Fallback for HTML fragments or plain text served as text/html
  return `<!DOCTYPE html>\n<html>\n<head>\n<meta charset="UTF-8" />\n${injectionBlock}\n</head>\n<body>\n${content}\n</body>\n</html>`;
}

