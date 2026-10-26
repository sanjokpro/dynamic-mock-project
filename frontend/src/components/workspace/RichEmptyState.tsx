'use client';

import React from 'react';
import { Landmark, Plus, Import, GitBranch, ArrowRight, Sparkles } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface RichEmptyStateProps {
  onOpenSandboxModal: () => void;
  onCreateRoute: () => void;
  onOpenImportModal: () => void;
}

export function RichEmptyState({
  onOpenSandboxModal,
  onCreateRoute,
  onOpenImportModal,
}: RichEmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center h-full p-8 overflow-y-auto bg-background/50 custom-scrollbar">
      <div className="max-w-3xl w-full space-y-6">
        {/* Hero header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold mb-1">
            <Sparkles className="h-3.5 w-3.5" />
            <span>Developer Simulation Workbench</span>
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-foreground">
            Get Started with Dynamic Mock
          </h2>
          <p className="text-sm text-muted-foreground max-w-lg mx-auto">
            Select an existing endpoint from the sidebar, or launch one of our pre-configured integration environments below.
          </p>
        </div>

        {/* 2x2 Action Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          {/* Card 1: Reference Banking Sandbox (Flagship Moat) */}
          <div className="relative group p-5 rounded-xl border-2 border-primary/30 bg-gradient-to-br from-primary/5 via-card to-card hover:border-primary transition-all shadow-xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 rounded-lg bg-primary/10 text-primary">
                  <Landmark className="h-5 w-5" />
                </div>
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-primary text-primary-foreground">
                  Moat Demo
                </span>
              </div>
              <h3 className="text-base font-semibold text-foreground mb-1">
                Reference Banking Sandbox
              </h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                5-minute financial quickstart demonstrating ISO8583 Authorization (0100), Capture (0200), Reversal (0400), and state machine transitions.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-border/50">
              <button
                type="button"
                onClick={onOpenSandboxModal}
                className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition-colors cursor-pointer shadow-xs"
              >
                <span>Launch Banking Sandbox</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>

          {/* Card 2: Create REST Mock Stub */}
          <div className="p-5 rounded-xl border border-border bg-card hover:border-primary/50 transition-all shadow-xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 rounded-lg bg-blue-500/10 text-blue-500">
                  <Plus className="h-5 w-5" />
                </div>
                <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground">
                  Quick Create
                </span>
              </div>
              <h3 className="text-base font-semibold text-foreground mb-1">
                Create REST Mock Stub
              </h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Configure a new HTTP mock route with path matchers, custom response templates, dynamic Handlebars scripting, and latency simulation.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-border/50">
              <button
                type="button"
                onClick={onCreateRoute}
                className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-secondary hover:bg-accent text-secondary-foreground text-xs font-medium transition-colors cursor-pointer"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>New REST Endpoint</span>
              </button>
            </div>
          </div>

          {/* Card 3: Import API Spec */}
          <div className="p-5 rounded-xl border border-border bg-card hover:border-primary/50 transition-all shadow-xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-500">
                  <Import className="h-5 w-5" />
                </div>
                <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground">
                  Migration
                </span>
              </div>
              <h3 className="text-base font-semibold text-foreground mb-1">
                Import API Specification
              </h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Ingest existing Postman Collections (v2.1) or OpenAPI / Swagger (v3.0) specifications to automatically scaffold mock stubs in seconds.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-border/50">
              <button
                type="button"
                onClick={onOpenImportModal}
                className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-secondary hover:bg-accent text-secondary-foreground text-xs font-medium transition-colors cursor-pointer"
              >
                <Import className="h-3.5 w-3.5" />
                <span>Import Postman / OpenAPI</span>
              </button>
            </div>
          </div>

          {/* Card 4: Multi-Protocol Simulator */}
          <div className="p-5 rounded-xl border border-border bg-card hover:border-primary/50 transition-all shadow-xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="p-2.5 rounded-lg bg-amber-500/10 text-amber-500">
                  <GitBranch className="h-5 w-5" />
                </div>
                <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground">
                  Enterprise
                </span>
              </div>
              <h3 className="text-base font-semibold text-foreground mb-1">
                Multi-Protocol Simulator
              </h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Build stateful multi-step choreography with Redis state machine, or simulate non-HTTP protocols including ISO8583, gRPC, and GraphQL.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-border/50">
              <button
                type="button"
                onClick={onOpenSandboxModal}
                className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-secondary hover:bg-accent text-secondary-foreground text-xs font-medium transition-colors cursor-pointer"
              >
                <span>Explore Protocol Demos</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
