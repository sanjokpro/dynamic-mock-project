'use client';

import React, { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import { Globe, ChevronDown, Sun, Moon, Play, Bell, ArrowRight, Plus, Search, Menu, Route, X, Import, Settings, LogOut, Landmark, HelpCircle } from 'lucide-react';
import { useEnvironments } from '@/hooks/useEnvironments';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useTheme } from '@/context/ThemeContext';
import { useRoutes } from '@/hooks/useRoutes';
import { useCollections } from '@/hooks/useCollections';
import { useAuth } from '@/hooks/useAuth';
import { useBrandingContext } from '@/context/BrandingContext';
import { cn } from '@/lib/utils';
import { MockRoute } from '@/types';
import { ImportModal } from './ImportModal';
import { EnvironmentManagerModal } from './environments/EnvironmentManagerModal';
import { BankingSandboxModal } from './BankingSandboxModal';
import { KeyboardShortcutsModal } from './KeyboardShortcutsModal';
import { useDialogs } from '@/context/DialogContext';
import { toast } from 'sonner';

const METHOD_COLORS: Record<string, string> = {
  GET: 'text-blue-500',
  POST: 'text-emerald-500',
  PUT: 'text-orange-500',
  PATCH: 'text-yellow-500',
  DELETE: 'text-red-500',
  HEAD: 'text-purple-500',
  OPTIONS: 'text-gray-500',
};

export function Header() {
  const { environments, isLoading: envsLoading } = useEnvironments();
  const { activeEnvironment, setActiveEnvironment, panelLayout, setPanelLayout, setActiveRoute, toggleSidebar } = useWorkspace();
  const { theme, toggleTheme } = useTheme();
  const { routes, createRoute } = useRoutes();
  const { user, logout } = useAuth();
  const { branding } = useBrandingContext();
  const [logoError, setLogoError] = useState(false);
  const userId = user?.id ?? '';

  useEffect(() => {
    setLogoError(false);
  }, [branding.logoUrl]);
  const { collections } = useCollections(userId);
  const { prompt } = useDialogs();
  const [envDropdownOpen, setEnvDropdownOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const [envManagerOpen, setEnvManagerOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [bankingSandboxOpen, setBankingSandboxOpen] = useState(false);
  const [shortcutsOpen, setShortcutsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (environments.length > 0 && !activeEnvironment) {
      const defaultEnv = environments.find(e => e.isDefault) || environments[0];
      setActiveEnvironment(defaultEnv);
    }
  }, [environments, activeEnvironment, setActiveEnvironment]);

  // Close dropdowns on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setEnvDropdownOpen(false);
      }
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // Keyboard shortcut: Cmd+K or Ctrl+K to focus search, ? to open shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        searchInputRef.current?.focus();
        setShowSuggestions(true);
      } else if (e.key === '?' && !e.metaKey && !e.ctrlKey) {
        const tag = (document.activeElement?.tagName || '').toLowerCase();
        if (tag !== 'input' && tag !== 'textarea' && !(document.activeElement as HTMLElement)?.isContentEditable) {
          e.preventDefault();
          setShortcutsOpen(prev => !prev);
        }
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  const filteredSuggestions = useMemo(() => {
    if (!searchQuery.trim()) return routes.slice(0, 8);
    const q = searchQuery.toLowerCase();
    return routes.filter(r =>
      r.name?.toLowerCase().includes(q) ||
      r.path?.toLowerCase().includes(q) ||
      r.method?.toLowerCase().includes(q)
    ).slice(0, 12);
  }, [routes, searchQuery]);

  // Reset highlight when suggestions change
  useEffect(() => {
    setHighlightedIndex(-1);
  }, [filteredSuggestions]);

  const handleSelectSuggestion = useCallback((route: MockRoute) => {
    setActiveRoute(route);
    setSearchQuery('');
    setShowSuggestions(false);
    searchInputRef.current?.blur();
  }, [setActiveRoute]);

  const handleSearchKeyDown = useCallback((e: React.KeyboardEvent) => {
    const items = filteredSuggestions;
    
    switch (e.key) {
      case 'Escape':
        setShowSuggestions(false);
        searchInputRef.current?.blur();
        break;
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex(prev => 
          prev < items.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex(prev => 
          prev > 0 ? prev - 1 : items.length - 1
        );
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < items.length) {
          handleSelectSuggestion(items[highlightedIndex]);
        } else if (items.length > 0) {
          handleSelectSuggestion(items[0]);
        }
        break;
    }
  }, [filteredSuggestions, highlightedIndex, handleSelectSuggestion]);

  const handleQuickCreateRoute = useCallback(async () => {
    const name = await prompt({ title: 'New Route', placeholder: 'Route Name' });
    if (name) {
      try {
        const targetCollection = collections?.[0];
        if (targetCollection) {
          const newRoute = await createRoute({ 
            name, 
            method: 'GET', 
            path: '/' + name.toLowerCase().replace(/\s+/g, '-'),
            protocol: 'HTTP' 
          });
          if (newRoute) {
            setActiveRoute(newRoute);
            toast.success('Route created');
          }
        } else {
          const newRoute = await createRoute({ 
            name, 
            method: 'GET', 
            path: '/' + name.toLowerCase().replace(/\s+/g, '-'),
            protocol: 'HTTP' 
          });
          if (newRoute) {
            setActiveRoute(newRoute);
            toast.success('Route created');
          }
        }
      } catch (err: any) {
        toast.error('Failed to create route: ' + err.message);
      }
    }
  }, [collections, createRoute, setActiveRoute, prompt]);

  return (
    <header className="h-12 border-b bg-background flex items-center justify-between px-3 shrink-0 gap-2">
      {/* ===== Left Section ===== */}
      <div className="flex items-center gap-2">
        {/* Sidebar Toggle */}
        <button
          onClick={toggleSidebar}
          className="p-1.5 hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground"
          title="Toggle sidebar"
        >
          <Menu className="h-4 w-4" />
        </button>

        <div className="w-px h-5 bg-border" />

        {/* Logo */}
        <div className="flex items-center gap-2 mr-1">
          {branding.logoUrl && !logoError ? (
            <img
              src={branding.logoUrl}
              alt={branding.productName}
              onError={() => setLogoError(true)}
              className="h-5 w-auto object-contain"
            />
          ) : (
            <div className="bg-primary p-1 rounded-md">
              <Globe className="h-3.5 w-3.5 text-primary-foreground" />
            </div>
          )}
          <span className="font-semibold text-sm tracking-tight hidden sm:inline">
            {branding.productName}
          </span>
        </div>

        <div className="w-px h-5 bg-border hidden sm:block" />

        {/* Environment Selector */}
        <div className="relative flex items-center gap-1" ref={dropdownRef}>
          <button
            onClick={() => setEnvDropdownOpen(!envDropdownOpen)}
            className="flex items-center gap-1.5 px-2 h-7 text-xs bg-accent/30 border border-border rounded-md hover:bg-accent transition-colors"
          >
            <div className={cn("w-1.5 h-1.5 rounded-full shrink-0", activeEnvironment ? "bg-green-500" : "bg-muted-foreground")} />
            <span className="font-medium max-w-[90px] truncate text-foreground">
              {envsLoading ? 'Loading...' : activeEnvironment?.name || 'No Env'}
            </span>
            <ChevronDown className={cn("h-3 w-3 text-muted-foreground transition-transform shrink-0", envDropdownOpen && "rotate-180")} />
          </button>
          
          <button
            onClick={() => setEnvManagerOpen(true)}
            className="p-1.5 h-7 w-7 flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-accent rounded-md transition-colors"
            title="Manage Environments"
          >
            <Settings className="h-3.5 w-3.5" />
          </button>

          {envDropdownOpen && (
            <div className="absolute left-0 top-full mt-1 w-52 bg-background border border-border rounded-lg shadow-lg z-50 py-1">
              <div className="px-3 py-1.5 text-[10px] font-bold text-muted-foreground uppercase tracking-wider">Environments</div>
              {environments.length === 0 ? (
                <div className="px-3 py-2 text-xs text-muted-foreground">No environments</div>
              ) : (
                environments.map((env) => (
                  <button
                    key={env.id}
                    onClick={() => { setActiveEnvironment(env); setEnvDropdownOpen(false); }}
                    className={cn(
                      "flex items-center gap-2 w-full px-3 py-1.5 text-xs text-left hover:bg-accent transition-colors",
                      activeEnvironment?.id === env.id && "bg-accent/50 font-medium"
                    )}
                  >
                    <div className={cn("w-1.5 h-1.5 rounded-full shrink-0", env.isDefault ? "bg-primary" : "bg-green-500")} />
                    <span className="flex-1 truncate text-foreground">{env.name}</span>
                    {env.isDefault && <span className="text-[9px] text-muted-foreground uppercase">Default</span>}
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Quick Actions (desktop) */}
        <div className="hidden sm:flex items-center gap-1.5 ml-1">
          <button
            type="button"
            onClick={() => setBankingSandboxOpen(true)}
            className="flex items-center gap-1.5 px-2.5 h-7 text-xs font-semibold text-primary bg-primary/10 hover:bg-primary/20 rounded-md transition-all shadow-xs cursor-pointer border border-primary/20"
            title="Open Banking Demo Sandbox (Optional ISO8583 Simulation)"
          >
            <Landmark className="h-3.5 w-3.5" />
            <span>Banking Demo Sandbox</span>
            <span className="hidden lg:inline text-[9px] font-bold bg-primary text-primary-foreground px-1 py-0.2 rounded">Demo</span>
          </button>
          <button
            onClick={() => setImportOpen(true)}
            className="flex items-center gap-1 px-2 h-7 text-xs text-muted-foreground hover:text-foreground hover:bg-accent rounded-md transition-colors"
          >
            <Import className="h-3 w-3" />
            <span>Import</span>
          </button>
        </div>
      </div>

      {/* ===== Center: Search with Suggestions ===== */}
      <div className="flex-1 max-w-lg mx-auto" ref={searchRef}>
        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
          <input
            ref={searchInputRef}
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setShowSuggestions(true); }}
            onFocus={() => setShowSuggestions(true)}
            onKeyDown={handleSearchKeyDown}
            placeholder="Search routes... (⌘K)"
            className="w-full h-8 pl-8 pr-8 text-xs bg-accent/20 border border-border rounded-md focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary outline-none placeholder:text-muted-foreground/60"
            aria-label="Search routes"
            role="combobox"
            aria-expanded={showSuggestions}
            aria-autocomplete="list"
          />
          {searchQuery && (
            <button
              onClick={() => { setSearchQuery(''); setShowSuggestions(false); searchInputRef.current?.focus(); }}
              className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 rounded hover:bg-accent text-muted-foreground"
              tabIndex={-1}
            >
              <X className="h-3 w-3" />
            </button>
          )}

          {/* Suggestions Dropdown */}
          {showSuggestions && (
            <div className="absolute left-0 right-0 top-full mt-1 bg-background border border-border rounded-lg shadow-lg z-50 overflow-hidden" role="listbox">
              {filteredSuggestions.length === 0 ? (
                <div className="px-3 py-4 text-center">
                  <Route className="h-5 w-5 mx-auto mb-1 text-muted-foreground/40" />
                  <p className="text-xs text-muted-foreground">
                    {searchQuery ? 'No routes match your search' : 'Start typing to find routes'}
                  </p>
                  {!searchQuery && (
                    <p className="text-[10px] text-muted-foreground/60 mt-1">
                      Tip: Typed by name, path, or method
                    </p>
                  )}
                </div>
              ) : (
                <div>
                  <div className="px-3 py-1.5 text-[10px] font-bold text-muted-foreground uppercase tracking-wider border-b border-border">
                    Routes
                  </div>
                  <div className="max-h-60 overflow-y-auto">
                    {filteredSuggestions.map((route, index) => (
                      <button
                        key={route.id}
                        onClick={() => handleSelectSuggestion(route)}
                        onMouseEnter={() => setHighlightedIndex(index)}
                        role="option"
                        aria-selected={highlightedIndex === index}
                        className={cn(
                          "flex items-center gap-2 w-full px-3 py-2 text-xs text-left transition-colors border-b border-border/50 last:border-0",
                          highlightedIndex === index 
                            ? "bg-accent text-foreground" 
                            : "hover:bg-accent/50 text-muted-foreground"
                        )}
                      >
                        <span className={cn("font-bold text-[10px] w-10 shrink-0 uppercase tabular-nums", METHOD_COLORS[route.method] || 'text-blue-500')}>
                          {route.method}
                        </span>
                        <div className="flex-1 min-w-0">
                          <span className="block truncate text-foreground font-medium">{route.name || route.path}</span>
                          <span className="block truncate text-muted-foreground/70 text-[10px] font-mono">{route.path}</span>
                        </div>
                        {route.protocol && route.protocol !== 'HTTP' && (
                          <span className="text-[9px] text-muted-foreground uppercase px-1.5 py-0.5 bg-muted rounded shrink-0">{route.protocol}</span>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ===== Right Section ===== */}
      <div className="flex items-center gap-1">
        {/* Quick Add Route */}
        <button
          onClick={handleQuickCreateRoute}
          className="flex items-center gap-1 px-2.5 h-7 bg-primary text-primary-foreground text-xs font-medium rounded-md hover:bg-primary/90 transition-colors shadow-sm"
          title="Create new route (quick)"
        >
          <Plus className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">New</span>
        </button>

        <div className="w-px h-5 bg-border ml-1" />

        {/* Layout Toggle */}
        <button
          onClick={() => setPanelLayout(panelLayout === 'vertical' ? 'horizontal' : 'vertical')}
          className={cn(
            "p-1.5 rounded-md transition-all",
            panelLayout === 'horizontal'
              ? "bg-primary/10 text-primary hover:bg-primary/20"
              : "text-muted-foreground hover:text-foreground hover:bg-accent"
          )}
          title={`Switch to ${panelLayout === 'vertical' ? 'horizontal' : 'vertical'} layout`}
        >
          <ArrowRight className={cn("h-3.5 w-3.5 transition-transform", panelLayout === 'horizontal' ? 'rotate-90' : 'rotate-0')} />
        </button>

        {/* Theme Toggle */}
        <button
          onClick={toggleTheme}
          className="p-1.5 hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground"
          title="Toggle theme"
        >
          {theme === 'dark' ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
        </button>

        {/* Notifications */}
        <button
          className="p-1.5 hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground relative"
          title="Notifications"
        >
          <Bell className="h-3.5 w-3.5" />
          <span className="absolute top-1 right-1 w-1.5 h-1.5 bg-destructive rounded-full ring-1 ring-background" />
        </button>

        {/* Keyboard Shortcuts Cheatsheet */}
        <button
          type="button"
          onClick={() => setShortcutsOpen(true)}
          className="p-1.5 hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground"
          title="Keyboard shortcuts (?)"
          aria-label="Keyboard shortcuts"
        >
          <HelpCircle className="h-3.5 w-3.5" />
        </button>

        {/* User menu */}
        <div className="relative" ref={userMenuRef}>
          <button
            id="header-user-menu-btn"
            onClick={() => setUserMenuOpen((v) => !v)}
            className="h-7 w-7 rounded-full bg-gradient-to-tr from-violet-500 to-blue-500 flex items-center justify-center text-[10px] font-bold text-white border border-white/20 shadow-sm hover:shadow-md transition-shadow"
            title={user?.username ?? 'Account'}
            aria-haspopup="true"
            aria-expanded={userMenuOpen}
          >
            {user?.username ? user.username.slice(0, 2).toUpperCase() : 'U'}
          </button>

          {userMenuOpen && (
            <div className="absolute right-0 top-full mt-1 w-44 bg-background border border-border rounded-lg shadow-lg z-50 py-1">
              <div className="px-3 py-2 border-b border-border">
                <p className="text-xs font-medium truncate">{user?.username}</p>
                {user?.email && (
                  <p className="text-[10px] text-muted-foreground truncate mt-0.5">{user.email}</p>
                )}
              </div>
              <button
                id="header-logout-btn"
                onClick={() => { setUserMenuOpen(false); logout(); }}
                className="flex items-center gap-2 w-full px-3 py-2 text-xs text-left text-destructive hover:bg-destructive/10 transition-colors"
              >
                <LogOut className="h-3.5 w-3.5" />
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>

      <ImportModal open={importOpen} onClose={() => setImportOpen(false)} />
      <EnvironmentManagerModal open={envManagerOpen} onClose={() => setEnvManagerOpen(false)} />
      <BankingSandboxModal open={bankingSandboxOpen} onClose={() => setBankingSandboxOpen(false)} />
      <KeyboardShortcutsModal open={shortcutsOpen} onClose={() => setShortcutsOpen(false)} />
    </header>
  );
}
