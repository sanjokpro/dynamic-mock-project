'use client';

import React, { useState } from 'react';
import { GitCommitHorizontal, ArrowRight, ChevronDown, ChevronUp, CheckCircle2 } from 'lucide-react';
import { RedirectHop } from '@/types';
import { cn } from '@/lib/utils';

export interface RedirectChainBannerProps {
  redirectChain?: RedirectHop[];
  className?: string;
}

export function RedirectChainBanner({ redirectChain, className }: RedirectChainBannerProps) {
  const [expanded, setExpanded] = useState(true);

  if (!redirectChain || redirectChain.length === 0) {
    return null;
  }

  const hopCount = redirectChain.length;
  const finalHop = redirectChain[redirectChain.length - 1];
  const totalRedirectMs =
    Math.round(redirectChain.reduce((acc, hop) => acc + (hop.durationMs || 0), 0) * 100) / 100;

  return (
    <div
      className={cn(
        'border-b border-border bg-amber-500/5 text-xs shrink-0',
        className
      )}
      data-testid="redirect-chain-banner"
    >
      {/* Summary Header */}
      <div
        onClick={() => setExpanded((prev) => !prev)}
        className="flex items-center justify-between px-3 py-1.5 cursor-pointer hover:bg-amber-500/10 transition-colors"
      >
        <div className="flex items-center gap-2 min-w-0">
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-amber-500/15 text-amber-500 border border-amber-500/30 font-semibold text-[10px]">
            <GitCommitHorizontal className="h-3 w-3" />
            <span>Redirect Chain</span>
          </span>

          <span
            className="font-medium text-foreground"
            data-testid="redirect-chain-hop-count"
          >
            {hopCount} {hopCount === 1 ? 'Hop' : 'Hops'}
          </span>

          <span className="text-muted-foreground hidden sm:inline">•</span>

          <span
            className="font-mono text-[11px] text-muted-foreground truncate"
            data-testid="redirect-chain-final-destination"
          >
            Final: {finalHop.url} ({finalHop.status})
          </span>
        </div>

        <div className="flex items-center gap-2 shrink-0 text-muted-foreground">
          <span className="font-mono text-[10px] tabular-nums">{totalRedirectMs}ms</span>
          {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
        </div>
      </div>

      {/* Detailed Hop Sequence */}
      {expanded && (
        <div className="px-3 pb-2 pt-1 space-y-1.5 border-t border-border/50 bg-background/50">
          {redirectChain.map((hop, idx) => {
            const isRedirect = hop.status >= 300 && hop.status < 400;
            const isFinal = idx === redirectChain.length - 1 && !hop.location;

            return (
              <div
                key={`${hop.step}-${hop.url}`}
                className="flex flex-col gap-0.5 rounded border border-border/60 bg-card/70 px-2.5 py-1.5"
                data-testid={`redirect-hop-${hop.step}`}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="inline-flex items-center justify-center w-4 h-4 rounded-full bg-muted text-muted-foreground font-mono text-[10px] font-bold shrink-0">
                      {hop.step}
                    </span>
                    <span className="font-bold text-[10px] text-primary shrink-0">
                      {hop.method}
                    </span>
                    <span className="font-mono text-[11px] text-foreground truncate" title={hop.url}>
                      {hop.url}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <span
                      className={cn(
                        'px-1.5 py-0.5 rounded font-mono text-[10px] font-semibold border',
                        isRedirect
                          ? 'bg-amber-500/10 text-amber-500 border-amber-500/20'
                          : hop.status >= 400
                          ? 'bg-red-500/10 text-red-500 border-red-500/20'
                          : 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20'
                      )}
                      data-testid={`redirect-hop-status-${hop.step}`}
                    >
                      {hop.status}
                    </span>
                    <span className="font-mono text-[10px] text-muted-foreground tabular-nums">
                      {hop.durationMs}ms
                    </span>
                  </div>
                </div>

                {hop.location && (
                  <div
                    className="flex items-center gap-1.5 pl-6 text-[11px] text-amber-500/90 font-mono truncate"
                    data-testid={`redirect-hop-location-${hop.step}`}
                  >
                    <ArrowRight className="h-3 w-3 shrink-0" />
                    <span>Location: {hop.location}</span>
                  </div>
                )}

                {isFinal && (
                  <div className="flex items-center gap-1.5 pl-6 text-[10px] text-emerald-500 font-medium">
                    <CheckCircle2 className="h-3 w-3 shrink-0" />
                    <span>Final Destination Reached</span>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
