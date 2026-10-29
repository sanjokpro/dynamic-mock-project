'use client';

import React, { useState, useMemo, useEffect, useRef } from 'react';
import { 
  Terminal, Trash2, Activity, ChevronDown, Clock, 
  History as HistoryIcon, List, Search, X 
} from 'lucide-react';
import { useTraffic } from '@/hooks/useTraffic';
import { useWorkspace } from '@/context/WorkspaceContext';
import { ExecutionEvent } from '@/types';
import { TrafficEventRow } from './TrafficEventRow';
import { formatTimestampWithMs } from '@/lib/httpStatus';
import { cn } from '@/lib/utils';

type Tab = 'console' | 'history';
type ProtocolFilter = 'ALL' | 'HTTP' | 'WEBHOOK' | 'GRAPHQL' | 'GRPC' | 'ISO8583';

export interface HistoryItem {
  id: string;
  method: string;
  path: string;
  status: number;
  statusText: string;
  durationMs: number;
  timestamp: Date;
}

export function BottomBar({ isOpen, onToggle }: { isOpen: boolean; onToggle: () => void }) {
  const [activeTab, setActiveTab] = useState<Tab>('console');
  const [selectedProtocol, setSelectedProtocol] = useState<ProtocolFilter>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [sessionHistory, setSessionHistory] = useState<HistoryItem[]>([]);

  const { events, clearEvents } = useTraffic();
  const { lastResponse, activeRoute } = useWorkspace();
  const lastProcessedResponseRef = useRef<any>(null);

  // Record real test client executions into Session History
  useEffect(() => {
    if (!lastResponse) return;
    if (lastResponse === lastProcessedResponseRef.current) return;
    lastProcessedResponseRef.current = lastResponse;

    const method = activeRoute?.method || 'GET';
    const path = activeRoute?.path || '/mock';

    setSessionHistory((prev) => [
      {
        id: `hist-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
        method,
        path,
        status: lastResponse.status,
        statusText: lastResponse.statusText || 'OK',
        durationMs: lastResponse.durationMs || 0,
        timestamp: new Date(),
      },
      ...prev,
    ].slice(0, 50));
  }, [lastResponse, activeRoute]);

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const protocolCounts = useMemo(() => {
    const counts: Record<ProtocolFilter, number> = {
      ALL: events.length,
      HTTP: 0,
      WEBHOOK: 0,
      GRAPHQL: 0,
      GRPC: 0,
      ISO8583: 0,
    };
    for (const ev of events) {
      const proto = ev.protocol as ProtocolFilter;
      if (counts[proto] !== undefined) {
        counts[proto]++;
      }
    }
    return counts;
  }, [events]);

  const filteredEvents = useMemo(() => {
    return events.filter((ev) => {
      // Protocol filter
      if (selectedProtocol !== 'ALL' && ev.protocol !== selectedProtocol) {
        return false;
      }

      // Search filter
      if (searchQuery.trim()) {
        const query = searchQuery.trim().toLowerCase();
        const matchesPath = ev.path?.toLowerCase().includes(query);
        const matchesQuery = ev.queryString?.toLowerCase().includes(query);
        const matchesMethod = ev.method?.toLowerCase().includes(query);
        const matchesStatus = String(ev.status).includes(query);
        const matchesId = ev.id?.toLowerCase().includes(query);
        const matchesRouteId = ev.resourceId?.toLowerCase().includes(query);
        const matchesMatch = ev.matchName?.toLowerCase().includes(query);
        return Boolean(
          matchesPath || matchesQuery || matchesMethod || matchesStatus || matchesId || matchesRouteId || matchesMatch
        );
      }

      return true;
    });
  }, [events, selectedProtocol, searchQuery]);

  return (
    <div className="flex flex-col h-full bg-background" data-testid="bottom-bar-panel">
      {/* Header Bar */}
      <div
        className={cn(
          'flex flex-wrap items-center justify-between px-3 py-1 border-b shrink-0 gap-2',
          'bg-panel-header-bg border-border'
        )}
      >
        <div className="flex items-center gap-1">
          <button
            onClick={onToggle}
            className="p-1 rounded hover:bg-accent transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
            title="Toggle panel"
            data-testid="bottom-bar-toggle"
          >
            <ChevronDown className={cn('h-3.5 w-3.5 transition-transform', isOpen ? 'rotate-0' : 'rotate-180')} />
          </button>

          <div className="flex items-center gap-0.5 ml-1">
            <button
              onClick={() => setActiveTab('console')}
              data-testid="bottom-bar-tab-console"
              className={cn(
                'flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-t transition-colors cursor-pointer',
                activeTab === 'console'
                  ? 'bg-background text-foreground font-medium border-t border-l border-r border-border'
                  : 'text-muted-foreground hover:text-foreground'
              )}
            >
              <Terminal className="h-3 w-3" />
              <span>Console</span>
              {events.length > 0 && activeTab === 'console' && (
                <span className="flex items-center gap-1 text-[9px] bg-primary/10 text-primary px-1.5 py-0.5 rounded-full font-semibold">
                  <Activity className="h-2 w-2 animate-pulse" />
                  {events.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('history')}
              data-testid="bottom-bar-tab-history"
              className={cn(
                'flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-t transition-colors cursor-pointer',
                activeTab === 'history'
                  ? 'bg-background text-foreground font-medium border-t border-l border-r border-border'
                  : 'text-muted-foreground hover:text-foreground'
              )}
            >
              <HistoryIcon className="h-3 w-3" />
              <span>History</span>
              {sessionHistory.length > 0 && (
                <span className="text-[9px] bg-muted px-1.5 py-0.5 rounded-full font-mono text-muted-foreground">
                  {sessionHistory.length}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Toolbar Controls */}
        <div className="flex items-center gap-2">
          {activeTab === 'console' && (
            <>
              {/* Search Bar */}
              <div className="relative">
                <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-3 w-3 text-muted-foreground pointer-events-none" />
                <input
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Filter traffic..."
                  className="h-6 w-36 sm:w-48 pl-6 pr-6 text-[11px] bg-background border border-border rounded focus:outline-none focus:ring-1 focus:ring-primary outline-none"
                  data-testid="traffic-search-input"
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery('')}
                    className="absolute right-1.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
                    data-testid="traffic-search-clear"
                  >
                    <X className="h-2.5 w-2.5" />
                  </button>
                )}
              </div>

              {/* Clear Console */}
              <button
                onClick={clearEvents}
                className="flex items-center gap-1 text-[11px] text-muted-foreground hover:text-foreground transition-colors px-1.5 py-0.5 rounded hover:bg-accent cursor-pointer"
                title="Clear console events"
                data-testid="traffic-clear-btn"
              >
                <Trash2 className="h-3 w-3" />
                <span className="hidden sm:inline">Clear</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* Protocol Filter Pills Bar (Console tab only) */}
      {isOpen && activeTab === 'console' && (
        <div className="flex items-center gap-1.5 px-3 py-1 border-b border-border/80 bg-muted/20 overflow-x-auto shrink-0 text-[10px]">
          {(['ALL', 'HTTP', 'WEBHOOK', 'GRAPHQL', 'GRPC', 'ISO8583'] as ProtocolFilter[]).map((proto) => {
            const count = protocolCounts[proto];
            const isSelected = selectedProtocol === proto;
            return (
              <button
                key={proto}
                type="button"
                onClick={() => setSelectedProtocol(proto)}
                data-testid={`traffic-filter-${proto.toLowerCase()}`}
                className={cn(
                  'inline-flex items-center gap-1 px-2 py-0.5 rounded font-mono transition-colors cursor-pointer border',
                  isSelected
                    ? 'bg-primary text-primary-foreground border-primary font-bold'
                    : 'bg-background/80 text-muted-foreground hover:text-foreground border-border/60 hover:bg-accent'
                )}
              >
                <span>{proto}</span>
                <span className={cn('text-[9px] tabular-nums', isSelected ? 'opacity-90' : 'text-muted-foreground')}>
                  ({count})
                </span>
              </button>
            );
          })}
        </div>
      )}

      {/* Content Area */}
      {isOpen && (
        <div className="flex-1 overflow-hidden">
          {activeTab === 'console' ? (
            <div className="h-full overflow-y-auto custom-scrollbar bg-background" data-testid="traffic-console-list">
              {filteredEvents.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-1.5 py-8">
                  <Terminal className="h-6 w-6 opacity-30" />
                  <span className="text-xs italic" data-testid="traffic-empty-state">
                    {events.length === 0 ? 'Waiting for traffic...' : 'No traffic events match your filter criteria'}
                  </span>
                </div>
              ) : (
                filteredEvents.map((event) => (
                  <TrafficEventRow
                    key={event.id}
                    event={event}
                    isExpanded={expandedIds.has(event.id)}
                    onToggle={() => toggleExpanded(event.id)}
                  />
                ))
              )}
            </div>
          ) : (
            <HistoryTab items={sessionHistory} onClear={() => setSessionHistory([])} />
          )}
        </div>
      )}
    </div>
  );
}

function HistoryTab({ items, onClear }: { items: HistoryItem[]; onClear: () => void }) {
  return (
    <div className="h-full bg-background flex flex-col overflow-hidden" data-testid="bottom-bar-history-view">
      {items.length > 0 && (
        <div className="flex items-center justify-end px-3 py-1 border-b border-border bg-muted/20 shrink-0">
          <button
            onClick={onClear}
            className="flex items-center gap-1 text-[11px] text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
          >
            <Trash2 className="h-3 w-3" /> Clear History
          </button>
        </div>
      )}

      <div className="flex-1 overflow-y-auto custom-scrollbar">
        {items.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-2 py-8">
            <List className="h-8 w-8 opacity-20" />
            <span className="text-xs italic">No request history yet in this session</span>
          </div>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-border text-muted-foreground text-[10px] uppercase tracking-wider bg-muted/20">
                <th className="text-left px-3 py-1.5 font-medium">Method</th>
                <th className="text-left px-3 py-1.5 font-medium">Path</th>
                <th className="text-left px-3 py-1.5 font-medium">Status</th>
                <th className="text-right px-3 py-1.5 font-medium">Time</th>
                <th className="text-right px-3 py-1.5 font-medium">Latency</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id} className="border-b border-border/50 hover:bg-accent/30 transition-colors font-mono">
                  <td className="px-3 py-1.5">
                    <span
                      className={cn(
                        'font-bold text-[11px]',
                        item.method === 'GET'
                          ? 'text-blue-500'
                          : item.method === 'POST'
                          ? 'text-emerald-500'
                          : item.method === 'PUT'
                          ? 'text-orange-500'
                          : item.method === 'DELETE'
                          ? 'text-red-500'
                          : 'text-purple-500'
                      )}
                    >
                      {item.method}
                    </span>
                  </td>
                  <td className="px-3 py-1.5 text-foreground">{item.path}</td>
                  <td className="px-3 py-1.5">
                    <span className={cn('font-semibold', item.status >= 400 ? 'text-red-500' : 'text-emerald-500')}>
                      {item.status} {item.statusText}
                    </span>
                  </td>
                  <td className="px-3 py-1.5 text-right text-muted-foreground tabular-nums text-[11px]">
                    <span className="flex items-center gap-1 justify-end">
                      <Clock className="h-2.5 w-2.5" />
                      {formatTimestampWithMs(item.timestamp)}
                    </span>
                  </td>
                  <td className="px-3 py-1.5 text-right text-muted-foreground tabular-nums">{item.durationMs}ms</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
