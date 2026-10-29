'use client';

import React from 'react';
import { ChevronRight, ChevronDown } from 'lucide-react';
import { ExecutionEvent } from '@/types';
import { formatPayloadSize } from '@/lib/httpStatus';
import { TrafficEventDetails } from './TrafficEventDetails';
import { cn } from '@/lib/utils';

export interface TrafficEventRowProps {
  event: ExecutionEvent;
  isExpanded: boolean;
  onToggle: () => void;
}

export function TrafficEventRow({ event, isExpanded, onToggle }: TrafficEventRowProps) {
  const protocolBadge = React.useMemo(() => {
    switch (event.protocol) {
      case 'WEBHOOK':
        return 'bg-amber-500/15 text-amber-400 border-amber-500/30';
      case 'GRPC':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'ISO8583':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'GRAPHQL':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      default:
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    }
  }, [event.protocol]);

  const methodColor = React.useMemo(() => {
    switch (event.method) {
      case 'GET':
        return 'text-blue-400';
      case 'POST':
        return 'text-emerald-400';
      case 'PUT':
        return 'text-orange-400';
      case 'DELETE':
        return 'text-red-400';
      case 'UNARY':
        return 'text-blue-300';
      case 'SERVER_STREAMING':
        return 'text-emerald-300';
      case 'BIDI_STREAMING':
        return 'text-purple-300';
      default:
        return 'text-foreground';
    }
  }, [event.method]);

  const formattedPayloadSize = React.useMemo(() => {
    if (event.payloadSizeBytes !== undefined && event.payloadSizeBytes !== null && event.payloadSizeBytes > 0) {
      return formatPayloadSize(event.payloadSizeBytes);
    }
    return '';
  }, [event.payloadSizeBytes]);

  const timestampStr = React.useMemo(() => {
    try {
      return new Date(event.timestamp).toLocaleTimeString([], { hour12: false });
    } catch {
      return '';
    }
  }, [event.timestamp]);

  return (
    <div
      className={cn(
        'border-b border-border/60 transition-colors',
        isExpanded ? 'bg-accent/15' : 'hover:bg-muted/15'
      )}
      data-testid={`traffic-event-row-${event.id}`}
    >
      {/* Collapsed Header Bar */}
      <div
        onClick={onToggle}
        className="flex items-center gap-2 px-2.5 py-1.5 cursor-pointer font-mono text-xs select-none"
        data-testid={`traffic-row-header-${event.id}`}
      >
        {/* Expand/Collapse Chevron */}
        <span className="text-muted-foreground shrink-0" data-testid={`traffic-expand-icon-${event.id}`}>
          {isExpanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
        </span>

        {/* Protocol Badge */}
        <span
          className={cn(
            'text-[9px] font-bold shrink-0 w-[58px] text-center px-1 py-0.5 rounded border',
            protocolBadge
          )}
          data-testid="traffic-row-protocol"
        >
          {event.protocol || 'HTTP'}
        </span>

        {/* Timestamp */}
        <span className="text-muted-foreground shrink-0 tabular-nums text-[11px]" data-testid="traffic-row-time">
          [{timestampStr}]
        </span>

        {/* HTTP Method */}
        <span className={cn('font-bold shrink-0 w-12 text-[11px]', methodColor)} data-testid="traffic-row-method">
          {event.method || '---'}
        </span>

        {/* Path with Query String highlighted */}
        <div className="flex items-center min-w-0 flex-1 truncate text-[11px]">
          <span className="font-semibold text-foreground truncate" data-testid="traffic-row-path">
            {event.path || ''}
          </span>
          {event.queryString && (
            <span
              className="text-cyan-400 font-normal truncate ml-0.5"
              title={`?${event.queryString}`}
              data-testid="traffic-row-query"
            >
              ?{event.queryString}
            </span>
          )}
        </div>

        {/* Route / Match Info */}
        <span
          className="text-muted-foreground text-[10px] truncate max-w-[140px] hidden md:inline"
          title={event.resourceId ? `Route ID: ${event.resourceId}` : event.matchName}
          data-testid="traffic-row-match"
        >
          {event.resourceId ? `ID: ${event.resourceId.slice(0, 8)}...` : event.matchName || ''}
        </span>

        {/* Status Badge */}
        <span
          className={cn(
            'px-1.5 py-0.2 rounded font-bold text-[11px] shrink-0 border tabular-nums',
            event.status >= 500
              ? 'bg-red-500/10 text-red-400 border-red-500/20'
              : event.status >= 400
              ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
              : event.status >= 300
              ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
              : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
          )}
          data-testid="traffic-row-status"
        >
          {event.status}
        </span>

        {/* Payload Size */}
        {formattedPayloadSize && (
          <span
            className="text-muted-foreground shrink-0 w-14 text-right tabular-nums text-[10px] hidden sm:inline"
            data-testid="traffic-row-size"
          >
            {formattedPayloadSize}
          </span>
        )}

        {/* Duration */}
        <span className="text-muted-foreground shrink-0 w-16 text-right tabular-nums text-[11px]" data-testid="traffic-row-duration">
          {event.durationMs}ms
        </span>
      </div>

      {/* Expanded Inline Inspector */}
      {isExpanded && <TrafficEventDetails event={event} />}
    </div>
  );
}
