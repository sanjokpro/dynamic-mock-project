'use client';

import React, { useState, useEffect } from 'react';
import { Landmark, CheckCircle2, ArrowRight, X, Loader2, RefreshCw, ShieldCheck, Database, Layers } from 'lucide-react';
import { toast } from 'sonner';

export interface BankingSandboxModalProps {
  open: boolean;
  onClose: () => void;
  onLoadSuccess?: () => void;
}

export function BankingSandboxModal({
  open,
  onClose,
  onLoadSuccess
}: BankingSandboxModalProps) {
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && open) {
        onClose();
      }
    };
    if (open) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const handleLoadSandbox = async () => {
    setIsLoading(true);
    try {
      const res = await fetch('/api/sandbox/load', { method: 'POST' });
      if (res.ok) {
        toast.success('Reference Banking Sandbox loaded successfully');
        onLoadSuccess?.();
        onClose();
        // Reload to refresh routes and scenario stubs in sidebar
        if (typeof window !== 'undefined') {
          setTimeout(() => {
            window.location.reload();
          }, 400);
        }
      } else {
        const errorText = await res.text();
        toast.error(`Failed to seed banking sandbox: ${errorText || res.statusText}`);
      }
    } catch (err: any) {
      toast.error(`Error loading banking sandbox: ${err.message || 'Network error'}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="banking-sandbox-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-2xl bg-background border border-border rounded-xl shadow-2xl overflow-hidden flex flex-col">
        {/* Modal Header */}
        <div className="flex items-start justify-between p-5 border-b bg-card">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-primary/10 text-primary">
              <Landmark className="h-6 w-6" />
            </div>
            <div>
              <h3 id="banking-sandbox-modal-title" className="text-base font-bold text-foreground">
                Reference Banking Sandbox
              </h3>
              <p className="text-xs text-muted-foreground mt-0.5">
                5-Minute Financial Quickstart & ISO8583 Lifecycle Simulation
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors cursor-pointer"
            aria-label="Close dialog"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Modal Content */}
        <div className="p-6 space-y-6 overflow-y-auto max-h-[70vh] custom-scrollbar">
          {/* Architecture Overview */}
          <div className="text-xs text-muted-foreground leading-relaxed">
            The Reference Banking Sandbox provides production-grade endpoints demonstrating how Dynamic Mock orchestrates stateful, multi-protocol payment transactions across ISO8583 banking networks and REST gateways.
          </div>

          {/* 4-Step Transaction Flow Visualizer */}
          <div className="space-y-3">
            <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block">
              Simulated Financial Lifecycle
            </span>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {/* Step 1: 0100 */}
              <div className="p-3 rounded-lg border border-border bg-card/60 flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-blue-500/10 text-blue-500 text-xs font-bold flex items-center justify-center shrink-0 mt-0.5">
                  1
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                    <span>0100 Authorization</span>
                    <span className="text-[10px] font-mono text-blue-500 bg-blue-500/10 px-1 rounded">ISO8583</span>
                  </h4>
                  <p className="text-[11px] text-muted-foreground mt-1">
                    Reserves funds on account, generates Systems Trace Audit Number (STAN), and initiates state machine.
                  </p>
                </div>
              </div>

              {/* Step 2: 0200 */}
              <div className="p-3 rounded-lg border border-border bg-card/60 flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-emerald-500/10 text-emerald-500 text-xs font-bold flex items-center justify-center shrink-0 mt-0.5">
                  2
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                    <span>0200 Financial Presentment</span>
                    <span className="text-[10px] font-mono text-emerald-500 bg-emerald-500/10 px-1 rounded">Settlement</span>
                  </h4>
                  <p className="text-[11px] text-muted-foreground mt-1">
                    Finalizes transaction capture against the authorization hold, debiting the cardholder ledger.
                  </p>
                </div>
              </div>

              {/* Step 3: 0400 */}
              <div className="p-3 rounded-lg border border-border bg-card/60 flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-orange-500/10 text-orange-500 text-xs font-bold flex items-center justify-center shrink-0 mt-0.5">
                  3
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                    <span>0400 Acquirer Reversal</span>
                    <span className="text-[10px] font-mono text-orange-500 bg-orange-500/10 px-1 rounded">Rollback</span>
                  </h4>
                  <p className="text-[11px] text-muted-foreground mt-1">
                    Unwinds authorization holds when acquirer network timeouts or POS cancellations occur.
                  </p>
                </div>
              </div>

              {/* Step 4: State Machine */}
              <div className="p-3 rounded-lg border border-border bg-card/60 flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-purple-500/10 text-purple-500 text-xs font-bold flex items-center justify-center shrink-0 mt-0.5">
                  4
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                    <span>State Machine Inspector</span>
                    <span className="text-[10px] font-mono text-purple-500 bg-purple-500/10 px-1 rounded">Redis Key</span>
                  </h4>
                  <p className="text-[11px] text-muted-foreground mt-1">
                    Live inspection of state transition keys (<code className="text-foreground">scenario:state:Card Payment Flow</code>).
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Sandbox seed details notice */}
          <div className="p-3 rounded-lg bg-primary/5 border border-primary/20 flex items-center gap-2.5 text-xs text-primary">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>
              Loading the sandbox creates reference routes, payment collections, and stateful scenario choreography in your active workspace.
            </span>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t bg-muted/20 flex items-center justify-between">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-medium text-muted-foreground hover:text-foreground rounded-lg transition-colors cursor-pointer"
          >
            Close
          </button>

          <button
            type="button"
            onClick={handleLoadSandbox}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-primary-foreground bg-primary hover:bg-primary/90 disabled:opacity-50 rounded-lg shadow-sm transition-all cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Seeding Sandbox...</span>
              </>
            ) : (
              <>
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Load / Reset Banking Sandbox</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
