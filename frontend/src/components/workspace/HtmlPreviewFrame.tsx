'use client';

import React, { useMemo, useState, useCallback } from 'react';
import { Globe, RotateCw, ExternalLink, ShieldCheck, Lock } from 'lucide-react';
import { buildSandboxedSrcDoc, resolvePreviewContext } from '@/lib/htmlPreviewShim';
import { cn } from '@/lib/utils';

export interface HtmlPreviewFrameProps {
  html: string;
  targetUrl?: string;
  mockPath?: string;
  interactive?: boolean;
  compact?: boolean;
}

const INTERACTIVE_SANDBOX = 'allow-scripts allow-forms allow-popups';
const STATIC_SANDBOX = '';

export function HtmlPreviewFrame({
  html,
  targetUrl,
  mockPath,
  interactive = true,
  compact = false,
}: HtmlPreviewFrameProps) {
  const [reloadKey, setReloadKey] = useState(0);

  const resolvedContext = useMemo(
    () => resolvePreviewContext({ targetUrl, mockPath }),
    [targetUrl, mockPath]
  );

  const srcDoc = useMemo(
    () => buildSandboxedSrcDoc(html, { targetUrl, mockPath, origin: resolvedContext.origin }),
    [html, targetUrl, mockPath, resolvedContext.origin]
  );

  const displayPath = useMemo(() => {
    if (targetUrl && targetUrl.trim()) {
      try {
        const parsed = new URL(targetUrl, resolvedContext.origin);
        return `${parsed.pathname}${parsed.search}`;
      } catch {
        return targetUrl.replace(/^https?:\/\/[^/]+/i, '') || `/mock${resolvedContext.mockPath}`;
      }
    }
    return `/mock${resolvedContext.mockPath === '/' ? '' : resolvedContext.mockPath}`;
  }, [targetUrl, resolvedContext]);

  const handleReload = useCallback(() => {
    setReloadKey((prev) => prev + 1);
  }, []);

  const handleOpenInNewTab = useCallback(() => {
    if (typeof window === 'undefined') return;
    const openUrl = resolvedContext.targetUrl;
    window.open(openUrl, '_blank', 'noopener,noreferrer');
  }, [resolvedContext.targetUrl]);

  const sandboxValue = interactive ? INTERACTIVE_SANDBOX : STATIC_SANDBOX;

  return (
    <div
      className={cn(
        'flex flex-col w-full h-full border border-border rounded-md overflow-hidden bg-background',
        compact ? 'min-h-[220px]' : 'min-h-[320px]'
      )}
      data-testid="html-preview-container"
    >
      {/* Mini Browser Preview Toolbar */}
      <div
        className={cn(
          'flex items-center justify-between gap-2 border-b border-border bg-muted/30 shrink-0',
          compact ? 'px-2.5 py-1 text-[11px]' : 'px-3 py-1.5 text-xs'
        )}
      >
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <Globe className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
          <span
            className="font-mono text-foreground/90 truncate bg-background/80 border border-border/70 rounded px-2 py-0.5 text-[11px]"
            title={resolvedContext.targetUrl}
            data-testid="html-preview-path"
          >
            {displayPath}
          </span>

          <span
            className={cn(
              'inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium shrink-0 border',
              interactive
                ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20'
                : 'bg-amber-500/10 text-amber-500 border-amber-500/20'
            )}
            data-testid="html-preview-mode-badge"
          >
            {interactive ? (
              <>
                <ShieldCheck className="h-3 w-3" />
                <span>Interactive</span>
              </>
            ) : (
              <>
                <Lock className="h-3 w-3" />
                <span>Static</span>
              </>
            )}
          </span>
        </div>

        <div className="flex items-center gap-1 shrink-0">
          <button
            type="button"
            onClick={handleReload}
            className="inline-flex items-center gap-1 px-2 py-1 rounded text-muted-foreground hover:text-foreground hover:bg-accent transition-colors cursor-pointer"
            title="Reload Preview"
            aria-label="Reload Preview"
            data-testid="html-preview-reload"
          >
            <RotateCw className="h-3 w-3" />
            {!compact && <span>Reload Preview</span>}
          </button>

          <button
            type="button"
            onClick={handleOpenInNewTab}
            className="inline-flex items-center gap-1 px-2 py-1 rounded text-muted-foreground hover:text-foreground hover:bg-accent transition-colors cursor-pointer"
            title="Open in New Tab"
            aria-label="Open in New Tab"
            data-testid="html-preview-open-tab"
          >
            <ExternalLink className="h-3 w-3" />
            {!compact && <span>Open in New Tab</span>}
          </button>
        </div>
      </div>

      {/* Isolated Opaque-Origin Sandboxed Iframe Viewport */}
      <div className="flex-1 w-full h-full bg-white relative overflow-hidden">
        <iframe
          key={reloadKey}
          title="HTML Stub Preview"
          srcDoc={srcDoc}
          sandbox={sandboxValue}
          referrerPolicy="no-referrer"
          className="w-full h-full border-0 block"
          data-testid="html-preview-iframe"
          data-reload-key={reloadKey}
        />
      </div>
    </div>
  );
}
