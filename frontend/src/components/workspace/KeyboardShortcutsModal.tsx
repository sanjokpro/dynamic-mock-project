'use client';

import React, { useEffect } from 'react';
import { Keyboard, X } from 'lucide-react';

export interface KeyboardShortcutsModalProps {
  open: boolean;
  onClose: () => void;
}

interface ShortcutItem {
  keys: string[];
  description: string;
}

interface ShortcutCategory {
  category: string;
  shortcuts: ShortcutItem[];
}

const SHORTCUT_CATEGORIES: ShortcutCategory[] = [
  {
    category: 'Request Execution',
    shortcuts: [
      { keys: ['⌘', 'Enter'], description: 'Execute current test request (Send)' },
      { keys: ['⌘', 'S'], description: 'Save route configuration to server' },
      { keys: ['Ctrl', 'Enter'], description: 'Execute test request (Windows / Linux)' },
      { keys: ['Ctrl', 'S'], description: 'Save route configuration (Windows / Linux)' },
    ]
  },
  {
    category: 'Navigation & Focus',
    shortcuts: [
      { keys: ['⌘', 'K'], description: 'Focus quick route search palette' },
      { keys: ['?'], description: 'Open this keyboard shortcuts cheatsheet' },
      { keys: ['Esc'], description: 'Close any active modal, popover, or dropdown' },
    ]
  },
  {
    category: 'Workbench Productivity',
    shortcuts: [
      { keys: ['Tab'], description: 'Navigate between fields and accessible popovers' },
      { keys: ['Space', 'Enter'], description: 'Toggle checkboxes and tooltips' },
      { keys: ['cURL'], description: 'One-click copy terminal-executable cURL command' },
    ]
  }
];

export function KeyboardShortcutsModal({ open, onClose }: KeyboardShortcutsModalProps) {
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

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="shortcuts-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-lg bg-background border border-border rounded-xl shadow-2xl overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b bg-card">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Keyboard className="h-5 w-5" />
            </div>
            <div>
              <h3 id="shortcuts-modal-title" className="text-sm font-bold text-foreground">
                Keyboard Shortcuts
              </h3>
              <p className="text-[11px] text-muted-foreground">
                Speed up your testing workflow with hotkeys
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors cursor-pointer"
            aria-label="Close shortcuts"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 space-y-5 overflow-y-auto max-h-[65vh] custom-scrollbar">
          {SHORTCUT_CATEGORIES.map((cat) => (
            <div key={cat.category} className="space-y-2.5">
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider block">
                {cat.category}
              </span>
              <div className="space-y-1.5">
                {cat.shortcuts.map((item, idx) => (
                  <div
                    key={idx}
                    className="flex items-center justify-between text-xs py-1.5 px-2 rounded-md hover:bg-muted/40 transition-colors"
                  >
                    <span className="text-foreground/90">{item.description}</span>
                    <div className="flex items-center gap-1 shrink-0 ml-3">
                      {item.keys.map((k, kIdx) => (
                        <kbd
                          key={kIdx}
                          className="px-2 py-0.5 text-[11px] font-mono font-semibold bg-muted text-muted-foreground border border-border rounded shadow-xs"
                        >
                          {k}
                        </kbd>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Footer */}
        <div className="p-3 border-t bg-muted/20 flex items-center justify-between text-[11px] text-muted-foreground">
          <span>Press <kbd className="px-1 py-0.5 font-mono text-[10px] bg-muted border border-border rounded">?</kbd> anywhere to open</span>
          <button
            type="button"
            onClick={onClose}
            className="px-3 py-1 bg-secondary text-secondary-foreground hover:bg-accent rounded text-xs font-medium cursor-pointer transition-colors"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  );
}
