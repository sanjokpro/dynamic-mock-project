'use client';

import React, { useState, useEffect } from 'react';
import { 
  Landmark, 
  CheckCircle2, 
  ArrowLeft, 
  X, 
  Loader2, 
  RefreshCw, 
  HelpCircle, 
  Users, 
  Code2, 
  Layers, 
  Sparkles 
} from 'lucide-react';
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
        toast.success('Banking Demo Sandbox loaded successfully');
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
        toast.error(`Failed to seed banking demo sandbox: ${errorText || res.statusText}`);
      }
    } catch (err: any) {
      toast.error(`Error loading banking demo sandbox: ${err.message || 'Network error'}`);
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
      <div className="relative w-full max-w-2xl bg-background border border-border rounded-xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-start justify-between p-5 border-b bg-card shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-primary/10 text-primary">
              <Landmark className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 id="banking-sandbox-modal-title" className="text-base font-bold text-foreground">
                  Banking Demo Sandbox
                </h3>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                  Optional Demo
                </span>
                <span className="hidden sm:inline-block text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20">
                  ISO8583 Showcase
                </span>
              </div>
              <p className="text-xs text-muted-foreground mt-0.5">
                Reference Financial Multi-Protocol & Stateful Transaction Lifecycle Demonstration
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

        {/* Modal Scrollable Content */}
        <div className="p-6 space-y-6 overflow-y-auto custom-scrollbar">
          {/* Visual Workflow Distinction Banner */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 p-3.5 rounded-xl border border-border/80 bg-muted/20">
            <div className="space-y-1 p-2 rounded-lg bg-background/80 border border-border/60">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                <Code2 className="h-3.5 w-3.5 text-blue-500" />
                <span>Regular REST Mocking</span>
              </div>
              <p className="text-[11px] text-muted-foreground leading-relaxed">
                Your standard workbench workflow. Create custom endpoints, test request matchers, dynamic response templates, and latency simulation.
              </p>
            </div>

            <div className="space-y-1 p-2 rounded-lg bg-primary/5 border border-primary/20">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-primary">
                <Sparkles className="h-3.5 w-3.5 text-primary" />
                <span>Banking Demo Sandbox</span>
              </div>
              <p className="text-[11px] text-muted-foreground leading-relaxed">
                An optional evaluation demo showcasing complex ISO8583 banking message flows, payment switch lifecycles, and Redis state machines.
              </p>
            </div>
          </div>

          {/* Guidance Cards: "What is this?" & "Who should use this?" */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* What is this? */}
            <div className="p-3.5 rounded-xl border border-border bg-card space-y-1.5">
              <div className="flex items-center gap-2 text-xs font-bold text-foreground">
                <div className="p-1 rounded bg-blue-500/10 text-blue-500">
                  <HelpCircle className="h-3.5 w-3.5" />
                </div>
                <span>What is this?</span>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed">
                A pre-packaged reference template containing simulated banking routes. It showcases how Dynamic Mock orchestrates stateful, multi-protocol payment switches across ISO8583 card networks, settlement engines, and REST API gateways.
              </p>
            </div>

            {/* Who should use this? */}
            <div className="p-3.5 rounded-xl border border-border bg-card space-y-1.5">
              <div className="flex items-center gap-2 text-xs font-bold text-foreground">
                <div className="p-1 rounded bg-emerald-500/10 text-emerald-500">
                  <Users className="h-3.5 w-3.5" />
                </div>
                <span>Who should use this?</span>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Intended for FinTech engineers, payment architects, and developers evaluating multi-step state machine transactions. <span className="font-semibold text-foreground">If you only need standard REST API mocking, this demo is completely optional.</span>
              </p>
            </div>
          </div>

          {/* 4-Step Transaction Flow Visualizer */}
          <div className="space-y-3">
            <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block">
              Sample Financial Lifecycle Included in Demo
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
          <div className="p-3 rounded-lg bg-muted/40 border border-border/80 flex items-center gap-2.5 text-xs text-muted-foreground">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-primary" />
            <span>
              Loading this demo seeds reference routes and collections into your workspace without affecting existing custom routes. You can clear or reset it anytime.
            </span>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t bg-muted/20 flex flex-col sm:flex-row items-center justify-between gap-3 shrink-0">
          {/* Continue with REST Mocking Action */}
          <button
            type="button"
            onClick={onClose}
            className="w-full sm:w-auto flex items-center justify-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-foreground bg-background hover:bg-accent border border-border rounded-lg transition-colors cursor-pointer shadow-xs"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>Continue with REST Mocking</span>
          </button>

          {/* Load Sandbox Action */}
          <button
            type="button"
            onClick={handleLoadSandbox}
            disabled={isLoading}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2 text-xs font-semibold text-primary-foreground bg-primary hover:bg-primary/90 disabled:opacity-50 rounded-lg shadow-sm transition-all cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Seeding Demo Sandbox...</span>
              </>
            ) : (
              <>
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Load / Reset Banking Demo Sandbox</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
