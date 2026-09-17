'use client';

import React, { createContext, useContext, useState } from 'react';
import { MockRoute, Environment, TestResponse } from '@/types';

export type PanelLayout = 'vertical' | 'horizontal';

interface WorkspaceContextType {
  activeRoute: MockRoute | null;
  setActiveRoute: (route: MockRoute | null) => void;
  activeEnvironment: Environment | null;
  setActiveEnvironment: (env: Environment | null) => void;
  lastResponse: TestResponse | null;
  setLastResponse: (response: TestResponse | null) => void;
  isDrawerOpen: boolean;
  setDrawerOpen: (open: boolean) => void;
  isSidebarOpen: boolean;
  toggleSidebar: () => void;
  panelLayout: PanelLayout;
  setPanelLayout: (layout: PanelLayout) => void;
}

const WorkspaceContext = createContext<WorkspaceContextType | undefined>(undefined);

export function WorkspaceProvider({ children }: { children: React.ReactNode }) {
  const [activeRoute, setActiveRoute] = useState<MockRoute | null>(null);
  const [activeEnvironment, setActiveEnvironment] = useState<Environment | null>(null);
  const [lastResponse, setLastResponse] = useState<TestResponse | null>(null);
  const [isDrawerOpen, setDrawerOpen] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [panelLayout, setPanelLayout] = useState<PanelLayout>('vertical');

  const toggleSidebar = () => setIsSidebarOpen(prev => !prev);

  return (
    <WorkspaceContext.Provider value={{
      activeRoute,
      setActiveRoute,
      activeEnvironment,
      setActiveEnvironment,
      lastResponse,
      setLastResponse,
      isDrawerOpen,
      setDrawerOpen,
      isSidebarOpen,
      toggleSidebar,
      panelLayout,
      setPanelLayout
    }}>
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (context === undefined) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }
  return context;
}
