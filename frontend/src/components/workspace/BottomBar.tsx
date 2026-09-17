'use client';

import React, { useState } from 'react';
import { 
  Terminal, Trash2, Activity, ChevronDown, Clock, History as HistoryIcon, List
} from 'lucide-react';
import { useTraffic } from '@/hooks/useTraffic';
import { useWorkspace } from '@/context/WorkspaceContext';
import { cn } from '@/lib/utils';

type Tab = 'console' | 'history';

export function BottomBar({ isOpen, onToggle }: { isOpen: boolean; onToggle: () => void }) {
  const [activeTab, setActiveTab] = useState<Tab>('console');
  const { events, clearEvents } = useTraffic();
  const { lastResponse } = useWorkspace();

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className={cn(
        "flex items-center justify-between px-3 py-1 border-b shrink-0",
        "bg-panel-header-bg border-border"
      )}>
        <div className="flex items-center gap-1">
          <button
            onClick={onToggle}
            className="p-1 rounded hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
            title="Toggle panel"
          >
            <ChevronDown className={cn("h-3.5 w-3.5 transition-transform", isOpen ? "rotate-0" : "rotate-180")} />
          </button>
          <div className="flex items-center gap-0.5 ml-1">
            <button
              onClick={() => setActiveTab('console')}
              className={cn(
                "flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-t transition-colors",
                activeTab === 'console' 
                  ? "bg-background text-foreground font-medium border-t border-l border-r border-border" 
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Terminal className="h-3 w-3" />
              Console
              {events.length > 0 && activeTab === 'console' && (
                <span className="flex items-center gap-1 text-[9px] bg-primary/10 text-primary px-1 py-0.5 rounded-full">
                  <Activity className="h-2 w-2 animate-pulse" />
                  {events.length}
                </span>
              )}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={cn(
                "flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-t transition-colors",
                activeTab === 'history' 
                  ? "bg-background text-foreground font-medium border-t border-l border-r border-border" 
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <HistoryIcon className="h-3 w-3" />
              History
              {lastResponse && activeTab === 'history' && (
                <span className="text-[9px] text-muted-foreground">1</span>
              )}
            </button>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {activeTab === 'console' && (
            <button
              onClick={clearEvents}
              className="flex items-center gap-1 text-[10px] text-muted-foreground hover:text-foreground transition-colors"
            >
              <Trash2 className="h-3 w-3" /> Clear
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      {isOpen && (
        <div className="flex-1 overflow-hidden">
          {activeTab === 'console' ? (
            <ConsoleTab events={events} />
          ) : (
            <HistoryTab />
          )}
        </div>
      )}
    </div>
  );
}

function ConsoleTab({ events }: { events: any[] }) {
  return (
    <div className="h-full bg-black text-green-500 font-mono text-xs overflow-hidden">
      <div className="h-full p-2 overflow-y-auto space-y-1 custom-scrollbar">
        {events.length === 0 ? (
          <div className="flex items-center justify-center h-full text-green-900/50 italic">
            Waiting for traffic...
          </div>
        ) : (
          events.map((event) => {
            const protocolColor = event.protocol === 'GRPC' ? 'text-blue-400' :
              event.protocol === 'ISO8583' ? 'text-yellow-400' :
              event.protocol === 'GRAPHQL' ? 'text-purple-400' :
              'text-green-400';
            return (
              <div key={event.id} className="flex gap-2 group hover:bg-white/5 transition-colors rounded px-1 items-center">
                <span className={cn("text-[9px] font-bold shrink-0 w-[52px] text-center px-1 py-0.5 rounded",
                  protocolColor.replace('text-', 'bg-').replace('-400', '-900/30') + ' ' + protocolColor)}>
                  {event.protocol || 'HTTP'}
                </span>
                <span className="text-green-800 shrink-0 tabular-nums">
                  [{new Date(event.timestamp).toLocaleTimeString([], { hour12: false })}]
                </span>
                <span className={cn("font-bold shrink-0 w-10",
                  event.method === 'GET' ? 'text-blue-400' :
                  event.method === 'POST' ? 'text-orange-400' :
                  event.method === 'UNARY' ? 'text-blue-300' :
                  event.method === 'SERVER_STREAMING' ? 'text-green-300' :
                  event.method === 'BIDI_STREAMING' ? 'text-purple-300' :
                  'text-purple-400'
                )}>
                  {event.method || '---'}
                </span>
                <span className="text-green-300 truncate max-w-sm">{event.path || ''}</span>
                <span className="text-green-600 italic truncate hidden md:inline text-[10px]">
                  {event.resourceId ? `ID: ${event.resourceId.substring(0, 8)}...` : event.matchName ? event.matchName : 'No Match'}
                </span>
                <span className={cn("ml-auto shrink-0 font-bold", event.status >= 400 ? 'text-red-400' : 'text-white')}>
                  {event.status}
                </span>
                <span className="text-green-800 shrink-0 w-12 text-right tabular-nums">{event.durationMs}ms</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

function HistoryTab() {
  const { lastResponse } = useWorkspace();

  // Build history from last response
  const historyItems: Array<{
    method: string;
    path: string;
    status: number;
    statusText: string;
    durationMs: number;
    timestamp: Date;
  }> = [];

  if (lastResponse) {
    historyItems.push({
      method: 'GET',
      path: '/mock/hello',
      status: lastResponse.status,
      statusText: lastResponse.statusText,
      durationMs: lastResponse.durationMs,
      timestamp: new Date(),
    });
  }

  return (
    <div className="h-full bg-background">
      <div className="h-full overflow-y-auto custom-scrollbar">
        {historyItems.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-2">
            <List className="h-8 w-8 opacity-20" />
            <span className="text-xs italic">No request history yet</span>
          </div>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-border text-muted-foreground text-[10px] uppercase tracking-wider">
                <th className="text-left px-3 py-1.5 font-medium">Method</th>
                <th className="text-left px-3 py-1.5 font-medium">Path</th>
                <th className="text-left px-3 py-1.5 font-medium">Status</th>
                <th className="text-right px-3 py-1.5 font-medium">Time</th>
                <th className="text-right px-3 py-1.5 font-medium">Latency</th>
              </tr>
            </thead>
            <tbody>
              {historyItems.map((item, idx) => (
                <tr key={idx} className="border-b border-border/50 hover:bg-accent/30 transition-colors">
                  <td className="px-3 py-1.5">
                    <span className={cn("font-bold",
                      item.method === 'GET' ? 'text-blue-500' :
                      item.method === 'POST' ? 'text-emerald-500' :
                      item.method === 'PUT' ? 'text-orange-500' :
                      item.method === 'DELETE' ? 'text-red-500' :
                      'text-purple-500'
                    )}>
                      {item.method}
                    </span>
                  </td>
                  <td className="px-3 py-1.5 font-mono text-muted-foreground">{item.path}</td>
                  <td className="px-3 py-1.5">
                    <span className={cn("font-medium", item.status >= 400 ? 'text-red-500' : 'text-green-500')}>
                      {item.status} {item.statusText}
                    </span>
                  </td>
                  <td className="px-3 py-1.5 text-right text-muted-foreground tabular-nums">
                    <span className="flex items-center gap-1 justify-end">
                      <Clock className="h-2.5 w-2.5" />
                      {item.timestamp.toLocaleTimeString([], { hour12: false })}
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
