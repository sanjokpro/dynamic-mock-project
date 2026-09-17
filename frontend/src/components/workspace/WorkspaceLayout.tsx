'use client';

import React, { useState, useCallback, useRef } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { RequestPanel } from './RequestPanel';
import { ResponsePanel } from './ResponsePanel';
import { BottomBar } from './BottomBar';
import { RouteDrawer } from './RouteDrawer';
import { useWorkspace } from '@/context/WorkspaceContext';
import { cn } from '@/lib/utils';
import { ErrorBoundary } from '../ui/ErrorBoundary';

export function WorkspaceLayout() {
  const { isDrawerOpen, setDrawerOpen, panelLayout, isSidebarOpen } = useWorkspace();
  const [sidebarWidth, setSidebarWidth] = useState(260);
  const [bottomPanelHeight, setBottomPanelHeight] = useState(200);
  const [bottomPanelOpen, setBottomPanelOpen] = useState(true);
  const [verticalRatio, setVerticalRatio] = useState(50); // Ratio between Request and Response panels (vertical layout)
  const [horizontalRatio, setHorizontalRatio] = useState(50); // Ratio for horizontal layout

  const isResizingSidebar = useRef(false);
  const isResizingBottom = useRef(false);
  const isResizingMain = useRef(false);

  const startResizingSidebar = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    isResizingSidebar.current = true;
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', stopResizing);
  }, []);

  const startResizingBottom = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    isResizingBottom.current = true;
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', stopResizing);
  }, []);

  const startResizingMain = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    isResizingMain.current = true;
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', stopResizing);
  }, []);

  const stopResizing = useCallback(() => {
    isResizingSidebar.current = false;
    isResizingBottom.current = false;
    isResizingMain.current = false;
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', stopResizing);
    document.body.style.cursor = 'default';
    document.body.style.userSelect = '';
  }, []);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (isResizingSidebar.current) {
      setSidebarWidth(Math.max(200, Math.min(600, e.clientX)));
      document.body.style.cursor = 'col-resize';
    } else if (isResizingBottom.current) {
      const height = window.innerHeight - e.clientY;
      setBottomPanelHeight(Math.max(40, Math.min(600, height)));
      document.body.style.cursor = 'row-resize';
    } else if (isResizingMain.current) {
      document.body.style.userSelect = 'none';
      if (panelLayout === 'vertical') {
        const container = document.getElementById('main-content-area');
        if (container) {
          const rect = container.getBoundingClientRect();
          const relativeY = e.clientY - rect.top;
          const ratio = (relativeY / rect.height) * 100;
          setVerticalRatio(Math.max(10, Math.min(90, ratio)));
        }
        document.body.style.cursor = 'row-resize';
      } else {
        const container = document.getElementById('main-content-area');
        if (container) {
          const rect = container.getBoundingClientRect();
          const relativeX = e.clientX - rect.left;
          const ratio = (relativeX / rect.width) * 100;
          setHorizontalRatio(Math.max(15, Math.min(85, ratio)));
        }
        document.body.style.cursor = 'col-resize';
      }
    }
  }, [panelLayout]);

  const isVertical = panelLayout === 'vertical';

  return (
    <div className="flex flex-col h-screen w-screen bg-background overflow-hidden text-foreground">
      <Header />
      
      {/* Route Drawer Overlay */}
      <RouteDrawer isOpen={isDrawerOpen} onClose={() => setDrawerOpen(false)} />

      <div className="flex flex-1 overflow-hidden">
        {/* Left Sidebar */}
        {isSidebarOpen && (
          <>
            <div style={{ width: sidebarWidth }} className="flex flex-col border-r bg-sidebar-bg overflow-hidden shrink-0">
              <Sidebar />
            </div>

            {/* Sidebar Resize Handle */}
            <div 
              onMouseDown={startResizingSidebar}
              className="w-[4px] cursor-col-resize hover:bg-primary/40 transition-colors bg-border shrink-0 resize-handle resize-handle-horizontal"
            />
          </>
        )}

        {/* Main Content Area */}
        <div className="flex-1 flex flex-col overflow-hidden min-w-0">
          <div 
            id="main-content-area" 
            className={cn(
              "flex-1 flex overflow-hidden",
              isVertical ? "flex-col" : "flex-row"
            )}
          >
            {/* Request Panel */}
            <div 
              style={isVertical ? { height: `${verticalRatio}%` } : { width: `${horizontalRatio}%` }}
              className="overflow-hidden min-h-0 min-w-0"
            >
              <ErrorBoundary fallback={<div className="p-4 text-red-500">Request Panel Error</div>}>
                <RequestPanel />
              </ErrorBoundary>
            </div>

            {/* Main Resize Handle */}
            <div 
              onMouseDown={startResizingMain}
              className={cn(
                "shrink-0 hover:bg-primary/40 transition-colors bg-border resize-handle",
                isVertical 
              ? "h-[4px] cursor-row-resize resize-handle-vertical" 
              : "w-[4px] cursor-col-resize resize-handle-horizontal"
              )}
            />

            {/* Response Panel */}
            <div 
              style={isVertical ? { height: `${100 - verticalRatio}%` } : { width: `${100 - horizontalRatio}%` }}
              className="overflow-hidden min-h-0 min-w-0"
            >
              <ErrorBoundary fallback={<div className="p-4 text-red-500">Response Panel Error</div>}>
                <ResponsePanel />
              </ErrorBoundary>
            </div>
          </div>

          {/* Bottom Panel Resize Handle */}
          {bottomPanelOpen && (
            <div 
              onMouseDown={startResizingBottom}
              className="h-[4px] cursor-row-resize hover:bg-primary/40 transition-colors bg-border shrink-0 resize-handle resize-handle-vertical"
            />
          )}

          {/* Bottom Panel (Console / History) */}
          <div 
            style={{ height: bottomPanelOpen ? bottomPanelHeight : 'auto' }}
            className={cn("border-t overflow-hidden shrink-0", bottomPanelOpen ? '' : '')}
          >
            <ErrorBoundary fallback={<div className="p-4 text-red-500">Traffic Panel Error</div>}>
              <BottomBar 
                isOpen={bottomPanelOpen} 
                onToggle={() => setBottomPanelOpen(!bottomPanelOpen)} 
              />
            </ErrorBoundary>
          </div>
        </div>
      </div>
    </div>
  );
}
