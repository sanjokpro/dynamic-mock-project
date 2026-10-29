'use client';

import React, { useState } from 'react';
import { 
  Copy, Check, Code2, Eye, GitCommitHorizontal, 
  Send, Server, AlertCircle 
} from 'lucide-react';
import { ExecutionEvent } from '@/types';
import { copyToClipboard } from '@/lib/curl';
import { HtmlPreviewFrame } from './HtmlPreviewFrame';
import { RedirectChainBanner } from './RedirectChainBanner';
import { isHtmlResponse } from './ResponsePanel';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

export interface TrafficEventDetailsProps {
  event: ExecutionEvent;
}

type DetailTab = 'overview' | 'params' | 'reqHeaders' | 'reqBody' | 'resHeaders' | 'resBody' | 'redirects' | 'webhook';

export function TrafficEventDetails({ event }: TrafficEventDetailsProps) {
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<DetailTab>('overview');
  const [showHtmlPreview, setShowHtmlPreview] = useState(false);

  const queryParamsList = React.useMemo(() => {
    if (event.queryParams && Object.keys(event.queryParams).length > 0) {
      return Object.entries(event.queryParams);
    }
    if (event.queryString && event.queryString.trim()) {
      try {
        const parsed = new URLSearchParams(event.queryString);
        return Array.from(parsed.entries());
      } catch {
        return [];
      }
    }
    return [];
  }, [event.queryParams, event.queryString]);

  const hasRedirects = Boolean(event.redirectChain && event.redirectChain.length > 0);
  const hasWebhook = Boolean(event.webhookDetail || event.protocol === 'WEBHOOK');
  const isHtml = isHtmlResponse(event.responseHeaders, event.responseBody);

  const handleCopy = async (text: string, label: string) => {
    const success = await copyToClipboard(text);
    if (success) {
      setCopiedId(label);
      toast.success(`Copied ${label}`);
      setTimeout(() => setCopiedId(null), 2000);
    }
  };

  return (
    <div
      className="border-t border-border/80 bg-background/95 text-xs flex flex-col overflow-hidden"
      data-testid={`traffic-event-details-${event.id}`}
    >
      {/* Top ID & Correlation Bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-3 py-2 border-b border-border bg-muted/20">
        <div className="flex flex-wrap items-center gap-2">
          {/* Full Request ID */}
          <div className="flex items-center gap-1.5 bg-background border border-border/80 rounded px-2 py-0.5 font-mono text-[11px]">
            <span className="text-muted-foreground text-[10px] font-sans uppercase">Req ID:</span>
            <span className="text-foreground select-all font-semibold" data-testid="traffic-detail-req-id">
              {event.id}
            </span>
            <button
              type="button"
              onClick={() => handleCopy(event.id, 'Request ID')}
              className="p-0.5 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
              title="Copy Request ID"
              data-testid="traffic-copy-req-id"
            >
              {copiedId === 'Request ID' ? <Check className="h-3 w-3 text-emerald-500" /> : <Copy className="h-3 w-3" />}
            </button>
          </div>

          {/* Full Route ID */}
          {event.resourceId && (
            <div className="flex items-center gap-1.5 bg-background border border-border/80 rounded px-2 py-0.5 font-mono text-[11px]">
              <span className="text-muted-foreground text-[10px] font-sans uppercase">Route ID:</span>
              <span className="text-foreground select-all" data-testid="traffic-detail-route-id">
                {event.resourceId}
              </span>
              <button
                type="button"
                onClick={() => handleCopy(event.resourceId!, 'Route ID')}
                className="p-0.5 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                title="Copy Route ID"
                data-testid="traffic-copy-route-id"
              >
                {copiedId === 'Route ID' ? <Check className="h-3 w-3 text-emerald-500" /> : <Copy className="h-3 w-3" />}
              </button>
            </div>
          )}

          {/* Parent Event ID (for Webhook correlation) */}
          {event.parentEventId && (
            <div className="flex items-center gap-1.5 bg-amber-500/10 border border-amber-500/30 text-amber-500 rounded px-2 py-0.5 font-mono text-[11px]">
              <span className="text-[10px] font-sans uppercase">Triggered By Req:</span>
              <span className="select-all font-semibold" data-testid="traffic-detail-parent-id">
                {event.parentEventId}
              </span>
              <button
                type="button"
                onClick={() => handleCopy(event.parentEventId!, 'Parent Request ID')}
                className="p-0.5 text-amber-500 hover:text-amber-400 transition-colors cursor-pointer"
                title="Copy Parent Request ID"
                data-testid="traffic-copy-parent-id"
              >
                {copiedId === 'Parent Request ID' ? <Check className="h-3 w-3 text-emerald-500" /> : <Copy className="h-3 w-3" />}
              </button>
            </div>
          )}
        </div>

        {/* Timestamp and Match info */}
        <div className="flex items-center gap-3 text-muted-foreground text-[11px]">
          {event.matchName && (
            <span>
              Match: <strong className="text-foreground">{event.matchName}</strong>
            </span>
          )}
          <span>{new Date(event.timestamp).toLocaleString()}</span>
        </div>
      </div>

      {/* Tabs Toolbar */}
      <div className="flex items-center justify-between border-b border-border bg-muted/10 px-2 text-[11px] shrink-0 overflow-x-auto">
        <div className="flex items-center">
          {[
            { id: 'overview', label: 'Overview' },
            { id: 'params', label: `Query Params (${queryParamsList.length})` },
            { id: 'reqHeaders', label: `Req Headers (${Object.keys(event.requestHeaders || {}).length})` },
            { id: 'reqBody', label: `Req Body ${event.requestBody ? '•' : ''}` },
            { id: 'resHeaders', label: `Res Headers (${Object.keys(event.responseHeaders || {}).length})` },
            { id: 'resBody', label: `Res Body ${event.responseBody ? '•' : ''}` },
            ...(hasRedirects ? [{ id: 'redirects', label: `Redirects (${event.redirectChain!.length})` }] : []),
            ...(hasWebhook ? [{ id: 'webhook', label: 'Webhook Details' }] : []),
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as DetailTab)}
              data-testid={`traffic-tab-${tab.id}`}
              className={cn(
                'px-2.5 py-1.5 transition-colors cursor-pointer font-medium border-b-2 whitespace-nowrap',
                activeTab === tab.id
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Quick action: HTML preview toggle when response body is HTML */}
        {isHtml && activeTab === 'resBody' && (
          <button
            type="button"
            onClick={() => setShowHtmlPreview((prev) => !prev)}
            className={cn(
              'inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-medium transition-colors cursor-pointer border',
              showHtmlPreview
                ? 'bg-primary text-primary-foreground border-primary'
                : 'bg-background border-border text-muted-foreground hover:text-foreground'
            )}
            data-testid="traffic-toggle-html-preview"
          >
            {showHtmlPreview ? <Code2 className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
            <span>{showHtmlPreview ? 'View Source' : 'Preview HTML'}</span>
          </button>
        )}
      </div>

      {/* Tab Panels */}
      <div className="p-3 max-h-72 overflow-y-auto custom-scrollbar">
        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
            <div className="space-y-1.5 rounded border border-border/70 p-2.5 bg-card">
              <span className="font-semibold text-muted-foreground text-[10px] uppercase tracking-wider block">
                Request Context
              </span>
              <div className="grid grid-cols-[80px_1fr] gap-1 text-[11px]">
                <span className="text-muted-foreground">Protocol:</span>
                <span className="font-semibold">{event.protocol}</span>
                <span className="text-muted-foreground">Method:</span>
                <span className="font-semibold">{event.method || '---'}</span>
                <span className="text-muted-foreground">Path:</span>
                <span className="font-mono break-all">{event.path}</span>
                {event.queryString && (
                  <>
                    <span className="text-muted-foreground">Query:</span>
                    <span className="font-mono text-cyan-500 break-all">{event.queryString}</span>
                  </>
                )}
              </div>
            </div>

            <div className="space-y-1.5 rounded border border-border/70 p-2.5 bg-card">
              <span className="font-semibold text-muted-foreground text-[10px] uppercase tracking-wider block">
                Execution Outcome
              </span>
              <div className="grid grid-cols-[80px_1fr] gap-1 text-[11px]">
                <span className="text-muted-foreground">Status:</span>
                <span className={cn('font-semibold font-mono', event.status >= 400 ? 'text-red-500' : 'text-emerald-500')}>
                  {event.status}
                </span>
                <span className="text-muted-foreground">Duration:</span>
                <span className="font-mono">{event.durationMs}ms</span>
                <span className="text-muted-foreground">Payload Size:</span>
                <span className="font-mono">{event.payloadSizeBytes ? `${event.payloadSizeBytes} B` : '<empty>'}</span>
                {event.bodyTruncated && (
                  <>
                    <span className="text-muted-foreground">Truncated:</span>
                    <span className="text-amber-500 font-medium">Yes (&gt;32KB limit)</span>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Decoded Query Parameters Tab */}
        {activeTab === 'params' && (
          <div>
            {queryParamsList.length === 0 ? (
              <div className="text-muted-foreground italic py-3 text-center">No query parameters on this request.</div>
            ) : (
              <div className="border border-border/80 rounded overflow-hidden">
                <table className="w-full text-left text-xs" data-testid="traffic-query-params-table">
                  <thead className="bg-muted/40 border-b border-border text-[10px] uppercase text-muted-foreground">
                    <tr>
                      <th className="px-3 py-1.5 font-semibold w-1/3">Parameter</th>
                      <th className="px-3 py-1.5 font-semibold">Decoded Value</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {queryParamsList.map(([k, v], idx) => (
                      <tr key={`${idx}-${k}`} className="hover:bg-muted/10 font-mono text-[11px]">
                        <td className="px-3 py-1.5 text-foreground font-semibold break-all">{k}</td>
                        <td className="px-3 py-1.5 text-muted-foreground break-all">{decodeURIComponent(v || '')}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Request Headers Tab */}
        {activeTab === 'reqHeaders' && (
          <HeadersTable headers={event.requestHeaders} emptyLabel="No request headers captured" />
        )}

        {/* Request Body Tab */}
        {activeTab === 'reqBody' && (
          <div>
            {event.requestBody ? (
              <pre className="bg-code-bg text-code-foreground rounded p-2.5 text-xs font-mono overflow-auto max-h-56 whitespace-pre-wrap break-all custom-scrollbar">
                {event.requestBody}
              </pre>
            ) : (
              <div className="text-muted-foreground italic py-3 text-center">No request body.</div>
            )}
          </div>
        )}

        {/* Response Headers Tab */}
        {activeTab === 'resHeaders' && (
          <HeadersTable headers={event.responseHeaders} emptyLabel="No response headers captured" />
        )}

        {/* Response Body Tab */}
        {activeTab === 'resBody' && (
          <div>
            {showHtmlPreview && isHtml ? (
              <div className="h-64 border border-border rounded overflow-hidden">
                <HtmlPreviewFrame
                  html={event.responseBody || ''}
                  mockPath={event.path}
                  interactive={false}
                  compact={true}
                />
              </div>
            ) : event.responseBody ? (
              <pre className="bg-code-bg text-code-foreground rounded p-2.5 text-xs font-mono overflow-auto max-h-56 whitespace-pre-wrap break-all custom-scrollbar">
                {event.responseBody}
              </pre>
            ) : (
              <div className="text-muted-foreground italic py-3 text-center">No response body.</div>
            )}
          </div>
        )}

        {/* Redirect Chain Tab */}
        {activeTab === 'redirects' && (
          <div className="space-y-2">
            <RedirectChainBanner redirectChain={event.redirectChain} />
          </div>
        )}

        {/* Webhook Detail Tab */}
        {activeTab === 'webhook' && event.webhookDetail && (
          <div className="space-y-3">
            <div className="rounded border border-amber-500/30 bg-amber-500/5 p-3 space-y-2">
              <div className="flex items-center gap-2">
                <Send className="h-4 w-4 text-amber-500" />
                <span className="font-bold text-foreground">Asynchronous Webhook Dispatch</span>
              </div>

              <div className="grid grid-cols-[100px_1fr] gap-1.5 text-xs font-mono">
                <span className="text-muted-foreground">Target URL:</span>
                <span className="text-foreground break-all">{event.webhookDetail.targetUrl}</span>
                {event.webhookDetail.finalUrl && event.webhookDetail.finalUrl !== event.webhookDetail.targetUrl && (
                  <>
                    <span className="text-muted-foreground">Final URL:</span>
                    <span className="text-foreground break-all">{event.webhookDetail.finalUrl}</span>
                  </>
                )}
                <span className="text-muted-foreground">Method:</span>
                <span className="font-bold">{event.webhookDetail.method}</span>
                <span className="text-muted-foreground">Response Status:</span>
                <span className={cn('font-bold', (event.webhookDetail.responseStatus ?? 0) >= 400 ? 'text-red-500' : 'text-emerald-500')}>
                  {event.webhookDetail.responseStatus ?? '---'}
                </span>
                {event.webhookDetail.delayMs !== undefined && event.webhookDetail.delayMs > 0 && (
                  <>
                    <span className="text-muted-foreground">Delay:</span>
                    <span>{event.webhookDetail.delayMs}ms</span>
                  </>
                )}
                {event.webhookDetail.errorMessage && (
                  <>
                    <span className="text-red-500 flex items-center gap-1">
                      <AlertCircle className="h-3 w-3" /> Error:
                    </span>
                    <span className="text-red-400 break-all">{event.webhookDetail.errorMessage}</span>
                  </>
                )}
              </div>
            </div>

            {event.webhookDetail.requestBody && (
              <div className="space-y-1">
                <span className="text-[10px] font-semibold uppercase text-muted-foreground">Webhook Request Payload:</span>
                <pre className="bg-code-bg text-code-foreground rounded p-2 text-xs font-mono overflow-auto max-h-36 whitespace-pre-wrap break-all custom-scrollbar">
                  {event.webhookDetail.requestBody}
                </pre>
              </div>
            )}

            {event.webhookDetail.responseBody && (
              <div className="space-y-1">
                <span className="text-[10px] font-semibold uppercase text-muted-foreground">Webhook Response Body:</span>
                <pre className="bg-code-bg text-code-foreground rounded p-2 text-xs font-mono overflow-auto max-h-36 whitespace-pre-wrap break-all custom-scrollbar">
                  {event.webhookDetail.responseBody}
                </pre>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function HeadersTable({ headers, emptyLabel }: { headers?: Record<string, string>; emptyLabel: string }) {
  if (!headers || Object.keys(headers).length === 0) {
    return <div className="text-muted-foreground italic py-3 text-center">{emptyLabel}</div>;
  }
  return (
    <div className="border border-border/80 rounded overflow-hidden">
      <table className="w-full text-left text-xs">
        <thead className="bg-muted/40 border-b border-border text-[10px] uppercase text-muted-foreground">
          <tr>
            <th className="px-3 py-1.5 font-semibold w-1/3">Header</th>
            <th className="px-3 py-1.5 font-semibold">Value</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border/60">
          {Object.entries(headers).map(([k, v]) => (
            <tr key={k} className="hover:bg-muted/10 font-mono text-[11px]">
              <td className="px-3 py-1.5 text-foreground font-semibold break-all">{k}</td>
              <td className="px-3 py-1.5 text-muted-foreground break-all">{v}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
