'use client';

import React, { useState, useEffect } from 'react';
import { X, Copy, Calendar, Clock, CheckSquare, Activity, Trash2 } from 'lucide-react';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useRoutes } from '@/hooks/useRoutes';
import { cn } from '@/lib/utils';

interface RouteDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export function RouteDrawer({ isOpen, onClose }: RouteDrawerProps) {
  const { activeRoute, setActiveRoute, setDrawerOpen } = useWorkspace();
  const { updateRoute, createRoute, deleteRoute } = useRoutes();

  const [localActive, setLocalActive] = useState(false);
  const [localDelayMs, setLocalDelayMs] = useState(0);
  const [localName, setLocalName] = useState('');
  const [localDescription, setLocalDescription] = useState('');
  const [isDirty, setIsDirty] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Sync local state from activeRoute when drawer opens or route changes
  useEffect(() => {
    if (activeRoute && isOpen) {
      setLocalActive(activeRoute.active ?? false);
      setLocalDelayMs(activeRoute.delayMs ?? 0);
      setLocalName(activeRoute.name || '');
      setLocalDescription('');
      setIsDirty(false);
    }
  }, [activeRoute?.id, isOpen]);

  const handleSave = async () => {
    if (!activeRoute) return;
    setIsSaving(true);
    try {
      await updateRoute({
        id: activeRoute.id,
        active: localActive,
        delayMs: localDelayMs,
        name: localName,
      });
      setIsDirty(false);
    } catch (e) {
      console.error('Failed to save route settings:', e);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDuplicate = async () => {
    if (!activeRoute) return;
    try {
      const { id, createdAt, updatedAt, version, ...routeData } = activeRoute;
      const newRouteData = {
        ...routeData,
        path: `${routeData.path}-copy-${Date.now()}`,
        active: false,
      };
      await createRoute(newRouteData);
      onClose();
    } catch (e) {
      console.error('Failed to duplicate route:', e);
    }
  };

  const handleDelete = async () => {
    if (!activeRoute) return;
    if (confirm('Are you sure you want to delete this stub?')) {
      try {
        await deleteRoute(activeRoute.id);
        setActiveRoute(null);
        setDrawerOpen(false);
      } catch (e) {
        console.error('Failed to delete route:', e);
      }
    }
  };

  // Close on Escape key
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.addEventListener('keydown', handleKey);
      return () => document.removeEventListener('keydown', handleKey);
    }
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/20 z-40 transition-opacity"
        onClick={onClose}
      />

      {/* Drawer Panel */}
      <div className={cn(
        "fixed top-0 right-0 h-full w-96 bg-background border-l border-border shadow-2xl z-50",
        "flex flex-col transition-transform duration-200 ease-in-out",
        isOpen ? "translate-x-0" : "translate-x-full"
      )}>
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-panel-header-bg">
          <div>
            <h2 className="text-sm font-semibold">Route Details</h2>
            {activeRoute && (
              <p className="text-[10px] text-muted-foreground mt-0.5">
                {activeRoute.method} {activeRoute.path}
              </p>
            )}
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-md hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-5">
          {/* Route Name */}
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Route Name</label>
            <input
              value={localName}
              onChange={(e) => { setLocalName(e.target.value); setIsDirty(true); }}
              placeholder="Route name"
              className="w-full bg-background border border-border rounded-md px-3 py-1.5 text-sm outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Description */}
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Description</label>
            <textarea
              value={localDescription}
              onChange={(e) => { setLocalDescription(e.target.value); setIsDirty(true); }}
              placeholder="Optional description"
              rows={3}
              className="w-full bg-background border border-border rounded-md px-3 py-1.5 text-sm outline-none focus:ring-1 focus:ring-primary resize-none"
            />
          </div>

          {/* Metadata */}
          {activeRoute && (
            <div className="space-y-1.5">
              <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Metadata</label>
              <div className="space-y-1 text-xs text-muted-foreground">
                <div className="flex items-center gap-2">
                  <Calendar className="h-3 w-3" />
                  <span>Created: {new Date(activeRoute.createdAt).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="h-3 w-3" />
                  <span>Modified: {new Date(activeRoute.updatedAt).toLocaleDateString()}</span>
                </div>
                {activeRoute.version !== undefined && (
                  <div className="flex items-center gap-2">
                    <Activity className="h-3 w-3" />
                    <span>Version: {activeRoute.version}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Settings */}
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Settings</label>
            <div className="space-y-2">
              <label className="flex items-center gap-3 px-3 py-2 rounded-md border border-border hover:bg-accent/30 transition-colors cursor-pointer">
                <div className={cn(
                  "w-4 h-4 rounded border-2 flex items-center justify-center transition-colors",
                  localActive ? "bg-primary border-primary" : "border-muted-foreground"
                )}>
                  {localActive && <CheckSquare className="h-3 w-3 text-primary-foreground" />}
                </div>
                <div className="flex-1">
                  <span className="text-sm font-medium">Active</span>
                  <p className="text-[10px] text-muted-foreground">Route is published and responding</p>
                </div>
                <input
                  type="checkbox"
                  checked={localActive}
                  onChange={(e) => { setLocalActive(e.target.checked); setIsDirty(true); }}
                  className="sr-only"
                />
              </label>
            </div>
          </div>

          {/* Delay Slider */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Response Delay</label>
              <span className="text-xs font-mono text-muted-foreground tabular-nums">{localDelayMs}ms</span>
            </div>
            <input
              type="range"
              min={0}
              max={5000}
              step={50}
              value={localDelayMs}
              onChange={(e) => { setLocalDelayMs(parseInt(e.target.value)); setIsDirty(true); }}
              className="w-full h-1.5 bg-border rounded-full appearance-none cursor-pointer accent-primary [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3.5 [&::-webkit-slider-thumb]:h-3.5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-primary"
            />
            <div className="flex justify-between text-[9px] text-muted-foreground">
              <span>0ms</span>
              <span>2.5s</span>
              <span>5s</span>
            </div>
          </div>

          {/* Response Preview */}
          {activeRoute?.responseTemplate && (
            <div className="space-y-1.5">
              <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Response Preview</label>
              <pre className="bg-code-bg text-code-foreground rounded-md p-3 text-xs font-mono overflow-x-auto max-h-32 custom-scrollbar">
                {activeRoute.responseTemplate.length > 300
                  ? activeRoute.responseTemplate.slice(0, 300) + '...'
                  : activeRoute.responseTemplate}
              </pre>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="border-t border-border p-3 bg-panel-header-bg flex items-center gap-2">
          <button
            onClick={handleSave}
            disabled={!isDirty || isSaving}
            className={cn(
              "flex-1 px-3 py-1.5 text-sm font-medium rounded-md transition-colors",
              isDirty
                ? "bg-primary text-primary-foreground hover:bg-primary/90"
                : "bg-muted text-muted-foreground cursor-not-allowed"
            )}
          >
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
          <button
            onClick={handleDuplicate}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md bg-accent text-foreground hover:bg-accent/80 transition-colors"
          >
            <Copy className="h-3.5 w-3.5" />
            Duplicate
          </button>
          <button
            onClick={handleDelete}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md bg-red-500/10 text-red-500 hover:bg-red-500/20 transition-colors"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Delete
          </button>
        </div>
      </div>
    </>
  );
}
