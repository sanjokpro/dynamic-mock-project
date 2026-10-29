'use client';

import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { Clock, Database, Ghost, Copy, Download, Check, Search, X, Terminal, Eye } from 'lucide-react';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useTheme } from '@/context/ThemeContext';
import { formatHttpStatus, formatPayloadSize } from '@/lib/httpStatus';
import { generateCurlCommand, copyToClipboard } from '@/lib/curl';
import { HtmlPreviewFrame } from './HtmlPreviewFrame';
import { RedirectChainBanner } from './RedirectChainBanner';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

type ResponseTab = 'preview' | 'body' | 'headers';

export function isHtmlResponse(headers?: Record<string, string>, body?: unknown): boolean {
  if (headers) {
    for (const [key, val] of Object.entries(headers)) {
      if (key.toLowerCase().trim() === 'content-type' && typeof val === 'string') {
        const lowerVal = val.toLowerCase();
        if (lowerVal.includes('text/html') || lowerVal.includes('application/xhtml+xml')) {
          return true;
        }
      }
    }
  }

  if (typeof body === 'string') {
    const trimmed = body.trim().toLowerCase();
    if (trimmed.startsWith('<!doctype html') || trimmed.startsWith('<html')) {
      return true;
    }
  }

  return false;
}

export function ResponsePanel() {
  const { lastResponse, activeRoute, activeEnvironment } = useWorkspace();
  const { theme } = useTheme();
  const [activeTab, setActiveTab] = useState<ResponseTab>('body');
  const [copied, setCopied] = useState(false);
  const [copiedCurl, setCopiedCurl] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const isHtml = useMemo(
    () => isHtmlResponse(lastResponse?.headers, lastResponse?.body),
    [lastResponse]
  );

  // Automatically select 'preview' when an HTML response arrives, or 'body' for non-HTML responses
  useEffect(() => {
    if (!lastResponse) return;
    if (isHtml) {
      setActiveTab('preview');
    } else {
      setActiveTab('body');
    }
  }, [lastResponse, isHtml]);

  const handleCopy = useCallback(() => {
    if (!lastResponse) return;
    const text = typeof lastResponse.body === 'string' 
      ? lastResponse.body 
      : JSON.stringify(lastResponse.body, null, 2);
    
    copyToClipboard(text).then((success) => {
      if (success) {
        setCopied(true);
        toast.success('Response copied to clipboard');
        setTimeout(() => setCopied(false), 2000);
      }
    });
  }, [lastResponse]);

  const handleCopyCurl = useCallback(async () => {
    if (!lastResponse) return;
    const method = activeRoute?.method || 'GET';
    const path = activeRoute?.path || '/api';
    let headers: Record<string, string> = {};
    let body: string | undefined;

    if (typeof window !== 'undefined' && activeRoute?.id) {
      try {
        const saved = localStorage.getItem(`dynamicmock:test_draft:${activeRoute.id}`);
        if (saved) {
          const draft = JSON.parse(saved);
          if (draft) {
            for (const [k, v] of Object.entries(draft.requestHeaders || {})) {
              if (k.toLowerCase() !== 'content-type') {
                headers[k] = v as string;
              }
            }
            if (draft.requestBodyType !== 'none') {
              body = draft.requestBodyContent;
            }
            if (draft.requestBodyType === 'json') {
              headers['Content-Type'] = 'application/json';
            } else if (draft.requestBodyType === 'form') {
              headers['Content-Type'] = 'application/x-www-form-urlencoded';
            } else if (draft.requestBodyType === 'raw') {
              const existingCt = Object.entries(draft.requestHeaders || {}).find(([k]) => k.toLowerCase() === 'content-type');
              headers['Content-Type'] = existingCt ? (existingCt[1] as string) : 'text/plain';
            }
          }
        }
      } catch (e) {
        console.warn('Failed reading draft for cURL export', e);
      }
    }

    // Note: X-Dynamic-Mock-Env excluded from cURL - it's an internal routing header

    const targetUrl = `{{BASE_URL}}${path.startsWith('/') ? path : '/' + path}`;
    const curl = generateCurlCommand({
      method,
      url: targetUrl,
      headers,
      body,
      variables: activeEnvironment?.variables,
      baseOrigin: typeof window !== 'undefined' ? `${window.location.origin}/mock` : undefined
    });

    const success = await copyToClipboard(curl);
    if (success) {
      setCopiedCurl(true);
      toast.success('cURL command copied to clipboard');
      setTimeout(() => setCopiedCurl(false), 2000);
    } else {
      toast.error('Failed to copy cURL command');
    }
  }, [lastResponse, activeRoute, activeEnvironment]);

  const handleDownload = useCallback(() => {
    if (!lastResponse) return;
    const text = typeof lastResponse.body === 'string'
      ? lastResponse.body
      : JSON.stringify(lastResponse.body, null, 2);
    const filename = isHtml ? 'response.html' : 'response.json';
    const mimeType = isHtml ? 'text/html' : 'application/json';
    const blob = new Blob([text], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    toast.success(`Downloaded ${filename}`);
  }, [lastResponse, isHtml]);

  const rawBody = lastResponse?.body;
  let formattedBody = '';
  if (typeof rawBody === 'string') {
    const trimmed = rawBody.trim();
    if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
      try {
        formattedBody = JSON.stringify(JSON.parse(trimmed), null, 2);
      } catch {
        formattedBody = rawBody;
      }
    } else {
      formattedBody = rawBody;
    }
  } else if (rawBody !== undefined && rawBody !== null) {
    try {
      formattedBody = JSON.stringify(rawBody, null, 2);
    } catch {
      formattedBody = String(rawBody);
    }
  }
  const bodyText = formattedBody;

  const mockPath = useMemo(() => {
    const rawPath = activeRoute?.path || '/';
    return rawPath.startsWith('/') ? rawPath : `/${rawPath}`;
  }, [activeRoute?.path]);

  const resolvedTargetUrl = useMemo(() => {
    const origin =
      typeof window !== 'undefined' && window.location?.origin && window.location.origin !== 'null'
        ? window.location.origin
        : 'http://localhost:8080';

    let rawUrl = `{{BASE_URL}}${mockPath}`;
    if (typeof window !== 'undefined' && activeRoute?.id) {
      try {
        const saved = localStorage.getItem(`dynamicmock:test_draft:${activeRoute.id}`);
        if (saved) {
          const draft = JSON.parse(saved);
          if (draft?.url && typeof draft.url === 'string') {
            rawUrl = draft.url;
          }
        }
      } catch {
        // Ignore draft read failure
      }
    }

    const vars = activeEnvironment?.variables || {};
    let resolved = rawUrl;
    for (const [k, v] of Object.entries(vars)) {
      resolved = resolved.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), v);
    }
    resolved = resolved.replace(/\{\{BASE_URL\}\}/g, `${origin}/mock`);
    return resolved;
  }, [activeRoute?.id, mockPath, activeEnvironment?.variables]);

  const matchCount = useMemo(() => {
    if (!searchQuery.trim() || !bodyText) return 0;
    try {
      const escaped = searchQuery.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const matches = bodyText.match(new RegExp(escaped, 'gi'));
      return matches ? matches.length : 0;
    } catch {
      return 0;
    }
  }, [searchQuery, bodyText]);

  if (!lastResponse) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-3 bg-panel-bg">
        <Ghost className="h-14 w-14 opacity-[0.08]" strokeWidth={1} />
        <p className="text-sm">Send a request to see the response</p>
      </div>
    );
  }

  const isError = lastResponse.status >= 400;

  return (
    <div className="flex flex-col h-full bg-panel-bg">
      {/* Header bar */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b bg-panel-header-bg">
        <div className="flex items-center gap-3 text-xs">
          {/* Status badge with Reason Phrase */}
          <span className={cn(
            "inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md font-semibold text-[11px]",
            isError 
              ? "bg-red-500/10 text-red-500 border border-red-500/20" 
              : "bg-green-500/10 text-green-500 border border-green-500/20"
          )}>
            <span className={cn("w-1.5 h-1.5 rounded-full", isError ? "bg-red-500" : "bg-green-500")} />
            {formatHttpStatus(lastResponse.status, lastResponse.statusText)}
          </span>

          {/* Duration */}
          <span className="flex items-center gap-1 text-muted-foreground">
            <Clock className="h-3 w-3" />
            <span className="tabular-nums">{lastResponse.durationMs}ms</span>
          </span>

          {/* Size */}
          {bodyText && (
            <span className="flex items-center gap-1 text-muted-foreground">
              <Database className="h-3 w-3" />
              <span className="tabular-nums">{formatPayloadSize(new Blob([bodyText]).size)}</span>
            </span>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={handleCopyCurl}
            className={cn(
              "flex items-center gap-1 px-2 py-1 text-xs rounded transition-colors cursor-pointer",
              copiedCurl
                ? "bg-green-500/10 text-green-500"
                : "text-muted-foreground hover:text-foreground hover:bg-accent"
            )}
            title="Copy as cURL"
          >
            {copiedCurl ? <Check className="h-3 w-3" /> : <Terminal className="h-3 w-3" />}
            <span className="hidden sm:inline">{copiedCurl ? 'Copied' : 'cURL'}</span>
          </button>

          <button
            type="button"
            onClick={handleCopy}
            className={cn(
              "flex items-center gap-1 px-2 py-1 text-xs rounded transition-colors cursor-pointer",
              copied 
                ? "bg-green-500/10 text-green-500" 
                : "text-muted-foreground hover:text-foreground hover:bg-accent"
            )}
            title="Copy response body"
          >
            {copied ? <Check className="h-3 w-3" /> : <Copy className="h-3 w-3" />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>

          <button
            type="button"
            onClick={handleDownload}
            className="flex items-center gap-1 px-2 py-1 text-xs rounded text-muted-foreground hover:text-foreground hover:bg-accent transition-colors cursor-pointer"
            title="Download response"
            data-testid="response-download-btn"
          >
            <Download className="h-3 w-3" />
            <span className="hidden sm:inline">Download</span>
          </button>
        </div>
      </div>

      {/* Redirect Chain Banner (hidden automatically when no redirects occurred) */}
      <RedirectChainBanner redirectChain={lastResponse.redirectChain} />

      {/* Tabs & Search Toolbar */}
      <div className="flex items-center justify-between border-b text-xs px-2 bg-muted/10 shrink-0">
        <div className="flex">
          {isHtml && (
            <button
              onClick={() => setActiveTab('preview')}
              data-testid="response-tab-preview"
              className={cn(
                "inline-flex items-center gap-1.5 px-3 py-1.5 transition-colors cursor-pointer font-medium border-b-2",
                activeTab === 'preview'
                  ? "border-primary text-foreground"
                  : "border-transparent text-muted-foreground hover:text-foreground"
              )}
            >
              <Eye className="h-3 w-3" />
              <span>Preview</span>
            </button>
          )}
          <button
            onClick={() => setActiveTab('body')}
            data-testid="response-tab-body"
            className={cn(
              "px-3 py-1.5 transition-colors cursor-pointer font-medium border-b-2",
              activeTab === 'body' 
                ? "border-primary text-foreground" 
                : "border-transparent text-muted-foreground hover:text-foreground"
            )}
          >
            Body
          </button>
          <button
            onClick={() => setActiveTab('headers')}
            data-testid="response-tab-headers"
            className={cn(
              "px-3 py-1.5 transition-colors cursor-pointer font-medium border-b-2",
              activeTab === 'headers' 
                ? "border-primary text-foreground" 
                : "border-transparent text-muted-foreground hover:text-foreground"
            )}
          >
            Headers ({Object.keys(lastResponse.headers || {}).length})
          </button>
        </div>

        {/* Payload Search Bar */}
        {activeTab === 'body' && (
          <div className="flex items-center gap-2 py-1">
            <div className="relative">
              <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-3 w-3 text-muted-foreground pointer-events-none" />
              <input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search..."
                className="h-6 w-36 sm:w-48 pl-6 pr-6 text-[11px] bg-background border border-border rounded focus:outline-none focus:ring-1 focus:ring-primary outline-none"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-1.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  <X className="h-2.5 w-2.5" />
                </button>
              )}
            </div>
            {searchQuery && (
              <span className="text-[10px] text-muted-foreground font-mono shrink-0">
                {matchCount} {matchCount === 1 ? 'match' : 'matches'}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'preview' && isHtml ? (
          <div className="h-full w-full p-2 overflow-hidden">
            <HtmlPreviewFrame
              html={bodyText}
              targetUrl={resolvedTargetUrl}
              mockPath={mockPath}
              interactive={true}
            />
          </div>
        ) : activeTab === 'body' ? (
          <div className="h-full overflow-auto p-3 custom-scrollbar">
            <JsonHighlighter json={bodyText} theme={theme} searchQuery={searchQuery} />
          </div>
        ) : (
          <div className="h-full overflow-auto p-3 custom-scrollbar">
            <div className="space-y-0.5">
              {Object.entries(lastResponse.headers || {}).map(([key, value]) => (
                <div key={key} className="flex gap-3 text-xs py-1 px-2 rounded hover:bg-accent/30">
                  <span className="font-medium text-foreground shrink-0 min-w-[160px]">{key}</span>
                  <span className="text-muted-foreground break-all">{value as string}</span>
                </div>
              ))}
              {(!lastResponse.headers || Object.keys(lastResponse.headers).length === 0) && (
                <div className="text-xs text-muted-foreground italic py-4 text-center">No headers</div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Highlighting helper applying search query mark highlight to safe token content
 */
function applySearchHighlight(escapedText: string, searchQuery?: string): string {
  if (!searchQuery || !searchQuery.trim()) return escapedText;
  try {
    const rawQuery = searchQuery.trim();
    // Escape HTML of the query so it matches escaped characters in text
    const escapedQuery = rawQuery
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
    const pattern = new RegExp(`(${escapedQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return escapedText.replace(pattern, '<mark class="bg-amber-400/40 text-amber-200 rounded-xs px-0.5 font-bold">$1</mark>');
  } catch {
    return escapedText;
  }
}

/**
 * Simple JSON syntax highlighter that renders color-coded tokens.
 * Uses dangerouslySetInnerHTML for the highlighting markup.
 */
function JsonHighlighter({ json, theme, searchQuery }: { json: string; theme: 'light' | 'dark'; searchQuery?: string }) {
  const highlighted = highlightJson(json, theme, searchQuery);

  return (
    <pre className={cn(
      "text-xs leading-relaxed font-mono whitespace-pre-wrap break-all m-0",
      theme === 'dark' ? 'text-slate-200' : 'text-slate-800'
    )}>
      <code dangerouslySetInnerHTML={{ __html: highlighted }} />
    </pre>
  );
}

export function highlightJson(json: string, theme: 'light' | 'dark', searchQuery?: string): string {
  // Colors for dark theme
  const colors = theme === 'dark'
    ? {
        key: '#7dd3fc',      // sky-300
        string: '#a5d6a7',   // green-300
        number: '#ffb74d',   // orange-300
        boolean: '#ce93d8',  // purple-300
        null: '#ef5350',     // red-400
        bracket: '#94a3b8',  // slate-400
        punctuation: '#64748b', // slate-500
      }
    : {
        key: '#0284c7',      // sky-600
        string: '#16a34a',   // green-600
        number: '#ea580c',   // orange-600
        boolean: '#9333ea',  // purple-600
        null: '#dc2626',     // red-600
        bracket: '#475569',  // slate-600
        punctuation: '#94a3b8', // slate-400
      };

  const escapeHtml = (str: string) =>
    str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');

  // Tokenize and highlight
  const tokens: string[] = [];
  const regex = /("(?:\\.|[^"\\])*")(?:\s*(:))?|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(\btrue\b|\bfalse\b)|(\bnull\b)|([{}[\]])|([,:])/g;
  let lastIndex = 0;

  let match: RegExpExecArray | null;

  while ((match = regex.exec(json)) !== null) {
    // Append whitespace before this token
    if (match.index > lastIndex) {
      tokens.push(escapeHtml(json.slice(lastIndex, match.index)));
    }

    if (match[1]) {
      // String (key or value)
      const isKey = !!match[2];
      const highlightedText = applySearchHighlight(escapeHtml(match[1]), searchQuery);
      tokens.push(`<span style="color:${isKey ? colors.key : colors.string}">${highlightedText}</span>`);
      if (match[2]) {
        tokens.push(`<span style="color:${colors.punctuation}">:</span>`);
      }
    } else if (match[3]) {
      // Number
      const highlightedNum = applySearchHighlight(escapeHtml(match[3]), searchQuery);
      tokens.push(`<span style="color:${colors.number}">${highlightedNum}</span>`);
    } else if (match[4]) {
      // Boolean
      const highlightedBool = applySearchHighlight(escapeHtml(match[4]), searchQuery);
      tokens.push(`<span style="color:${colors.boolean}">${highlightedBool}</span>`);
    } else if (match[5]) {
      // Null
      const highlightedNull = applySearchHighlight(escapeHtml(match[5]), searchQuery);
      tokens.push(`<span style="color:${colors.null}">${highlightedNull}</span>`);
    } else if (match[6]) {
      // Brackets
      tokens.push(`<span style="color:${colors.bracket}">${escapeHtml(match[6])}</span>`);
    } else if (match[7]) {
      // Punctuation
      tokens.push(`<span style="color:${colors.punctuation}">${escapeHtml(match[7])}</span>`);
    }

    lastIndex = regex.lastIndex;
  }

  // Append remaining text
  if (lastIndex < json.length) {
    tokens.push(escapeHtml(json.slice(lastIndex)));
  }

  return tokens.join('');
}
