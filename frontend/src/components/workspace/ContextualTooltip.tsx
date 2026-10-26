'use client';

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { HelpCircle, ExternalLink, X } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface ContextualTooltipProps {
  title: string;
  description: string;
  syntaxExample?: string;
  docLink?: string;
  side?: 'top' | 'right' | 'bottom' | 'left';
  className?: string;
}

export function ContextualTooltip({
  title,
  description,
  syntaxExample,
  docLink,
  side = 'top',
  className
}: ContextualTooltipProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Close on outside click
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  // Handle keyboard events (Escape to close)
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.stopPropagation();
      setIsOpen(false);
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setIsOpen(prev => !prev);
    }
  }, []);

  // Position classes depending on side
  const positionClasses = {
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 -translate-y-1/2 ml-2'
  }[side];

  return (
    <div
      ref={containerRef}
      className={cn("relative inline-flex items-center", className)}
      onMouseEnter={() => setIsOpen(true)}
      onMouseLeave={() => setIsOpen(false)}
    >
      <button
        type="button"
        aria-label={`Help: ${title}`}
        aria-expanded={isOpen}
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          setIsOpen(prev => !prev);
        }}
        onKeyDown={handleKeyDown}
        className="p-0.5 text-muted-foreground/60 hover:text-primary transition-colors focus:outline-none focus-visible:ring-1 focus-visible:ring-primary rounded-full cursor-pointer inline-flex items-center justify-center"
      >
        <HelpCircle className="h-3.5 w-3.5" />
      </button>

      {isOpen && (
        <div
          role="tooltip"
          className={cn(
            "absolute z-50 w-64 p-3 bg-popover text-popover-foreground border border-border rounded-lg shadow-xl text-left pointer-events-auto animate-in fade-in zoom-in-95 duration-100",
            positionClasses
          )}
        >
          <div className="flex items-start justify-between gap-1 mb-1">
            <h4 className="text-xs font-semibold text-foreground tracking-tight">{title}</h4>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setIsOpen(false);
              }}
              className="text-muted-foreground hover:text-foreground p-0.5 rounded"
              aria-label="Close tooltip"
            >
              <X className="h-3 w-3" />
            </button>
          </div>

          <p className="text-[11px] text-muted-foreground leading-relaxed">{description}</p>

          {syntaxExample && (
            <div className="mt-2 p-1.5 bg-muted/60 rounded border border-border/50 text-[10px] font-mono text-foreground break-all select-all">
              <code>{syntaxExample}</code>
            </div>
          )}

          {docLink && (
            <div className="mt-2 pt-1.5 border-t border-border/40">
              <a
                href={docLink}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 text-[10px] text-primary hover:underline font-medium"
              >
                <span>Documentation</span>
                <ExternalLink className="h-2.5 w-2.5" />
              </a>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
