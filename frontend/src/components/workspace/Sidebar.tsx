'use client';

import React, { useState, useMemo } from 'react';
import { Folder, FolderOpen, Plus, Search, Settings, Loader2, Inbox, Globe, ChevronRight, ChevronDown, Sun, Moon } from 'lucide-react';
import { useCollections } from '@/hooks/useCollections';
import { useRoutes } from '@/hooks/useRoutes';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useTheme } from '@/context/ThemeContext';
import { MockRoute } from '@/types';
import { cn } from '@/lib/utils';

const METHOD_COLORS: Record<string, string> = {
  GET: 'text-blue-500',
  POST: 'text-emerald-500',
  PUT: 'text-orange-500',
  PATCH: 'text-yellow-500',
  DELETE: 'text-red-500',
  HEAD: 'text-purple-500',
  OPTIONS: 'text-gray-500',
};

export function Sidebar() {
  const userId = 'default-user';
  const { collections, isLoading: isCollectionsLoading, createCollection, addItemToCollection } = useCollections(userId);
  const { routes, isLoading: isRoutesLoading, createRoute } = useRoutes();
  const { setActiveRoute, activeRoute, setDrawerOpen } = useWorkspace();
  const { theme, toggleTheme } = useTheme();
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedCollections, setExpandedCollections] = useState<Set<string>>(new Set());

  const toggleCollection = (id: string) => {
    setExpandedCollections(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const filteredRoutes = useMemo(() => {
    if (!searchQuery.trim()) return routes;
    const q = searchQuery.toLowerCase();
    return routes.filter(r => 
      r.name?.toLowerCase().includes(q) || 
      r.path?.toLowerCase().includes(q) ||
      r.method?.toLowerCase().includes(q)
    );
  }, [routes, searchQuery]);

  const handleCreateCollection = () => {
    const name = prompt('Collection Name:');
    if (name) {
      createCollection({ name, userId });
    }
  };

  const handleAddRoute = async (collectionId?: string) => {
    const name = prompt('Route Name:');
    if (name) {
      try {
        const newRoute = await createRoute({ name, method: 'GET', path: '/new-path', protocol: 'HTTP' });
        if (newRoute && collectionId) {
          await addItemToCollection({
            collectionId,
            item: {
              protocol: 'HTTP',
              resourceId: newRoute.id,
              name: name
            }
          });
        }
      } catch (err) {
        console.error("Failed to add route:", err);
      }
    }
  };

  const handleSelectRoute = (routeId: string) => {
    const route = routes.find(r => r.id === routeId);
    if (route) {
      setActiveRoute(route);
    }
  };

  return (
    <div className="flex flex-col h-full bg-sidebar-bg">
      {/* Search */}
      <div className="p-3 border-b border-sidebar-border">
        <div className="relative">
          <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
          <input 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search collections..." 
            className="w-full pl-8 pr-3 py-1.5 text-xs bg-background border border-border rounded-md focus:outline-none focus:ring-1 focus:ring-primary outline-none"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2 custom-scrollbar">
        {/* Collections Section */}
        <div className="flex items-center justify-between px-2 py-1.5">
          <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Collections</span>
          <button
            onClick={handleCreateCollection}
            className="flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground hover:text-foreground hover:bg-accent rounded transition-colors"
            title="Create new collection"
          >
            <Plus className="h-3 w-3" />
            <span>New</span>
          </button>
        </div>
        
        {isCollectionsLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ) : collections.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-muted-foreground gap-2">
            <Inbox className="h-8 w-8 opacity-20" />
            <span className="text-xs">No collections found</span>
          </div>
        ) : (
          <div className="space-y-0.5">
            {collections.map((collection) => {
              const isExpanded = expandedCollections.has(collection.id);
              return (
                <div key={collection.id} className="flex flex-col">
                  <button
                    onClick={() => toggleCollection(collection.id)}
                    className="flex items-center gap-1.5 w-full px-2 py-1.5 text-xs rounded-md hover:bg-accent transition-colors group text-left"
                  >
                    {isExpanded ? (
                      <ChevronDown className="h-3 w-3 text-muted-foreground shrink-0" />
                    ) : (
                      <ChevronRight className="h-3 w-3 text-muted-foreground shrink-0" />
                    )}
                    {isExpanded ? (
                      <FolderOpen className="h-3.5 w-3.5 text-primary shrink-0" />
                    ) : (
                      <Folder className="h-3.5 w-3.5 text-primary shrink-0" />
                    )}
                    <span className="flex-1 truncate font-medium text-foreground">{collection.name}</span>
                    <span className="text-[10px] text-muted-foreground tabular-nums">{collection.items?.length || 0}</span>
                    <span
                      role="button"
                      tabIndex={0}
                      onClick={(e) => { e.stopPropagation(); handleAddRoute(collection.id); }}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.stopPropagation(); handleAddRoute(collection.id); } }}
                      className="opacity-0 group-hover:opacity-100 p-0.5 hover:bg-background rounded text-muted-foreground hover:text-foreground transition-all cursor-pointer"
                      title="Add route to collection"
                    >
                      <Plus className="h-3 w-3" />
                    </span>
                  </button>
                  {isExpanded && collection.items && collection.items.length > 0 && (
                    <div className="ml-4 border-l border-border pl-1.5 space-y-0.5 mt-0.5">
                      {collection.items.map((item) => {
                        const route = routes.find(r => r.id === item.resourceId);
                        return (
                          <button
                            key={item.id}
                            onClick={() => handleSelectRoute(item.resourceId)}
                            className={cn(
                              "flex items-center gap-2 w-full px-2 py-1 text-xs rounded-sm transition-colors text-left",
                              activeRoute?.id === item.resourceId 
                                ? "bg-accent text-foreground" 
                                : "text-muted-foreground hover:text-foreground hover:bg-accent/50"
                            )}
                          >
                            {route?.method ? (
                              <span className={cn("font-bold text-[10px] w-10 shrink-0 uppercase tabular-nums", METHOD_COLORS[route.method] || 'text-blue-500')}>
                                {route.method}
                              </span>
                            ) : (
                              <span className="text-[10px] w-10 shrink-0 uppercase text-muted-foreground">{item.protocol}</span>
                            )}
                            <span className="truncate">{item.name}</span>
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Unorganized Routes */}
        <div className="mt-6 mb-2 px-2 flex items-center justify-between">
          <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">All Stubs</span>
          <button
            onClick={() => handleAddRoute()}
            className="flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground hover:text-foreground hover:bg-accent rounded transition-colors"
            title="Add new stub"
          >
            <Plus className="h-3 w-3" />
            <span>New</span>
          </button>
        </div>
        {isRoutesLoading ? (
          <div className="flex items-center justify-center py-4">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          </div>
        ) : filteredRoutes.length === 0 ? (
          <div className="px-2 text-[10px] text-muted-foreground italic">No routes found.</div>
        ) : (
          <div className="space-y-0.5">
            {filteredRoutes.map((route) => (
              <button
                key={route.id}
                onClick={() => setActiveRoute(route)}
                className={cn(
                  "flex items-center gap-2 w-full px-2 py-1.5 text-xs rounded-md transition-colors text-left",
                  activeRoute?.id === route.id ? "bg-accent text-foreground" : "text-muted-foreground hover:text-foreground hover:bg-accent/50"
                )}
              >
                <span className={cn("font-bold text-[10px] w-10 shrink-0 uppercase tabular-nums", METHOD_COLORS[route.method] || 'text-blue-500')}>
                  {route.method}
                </span>
                <span className="flex-1 truncate">{route.path}</span>
                {route.protocol && route.protocol !== 'HTTP' && (
                  <span className="text-[9px] text-muted-foreground uppercase px-1 bg-muted rounded">{route.protocol}</span>
                )}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="p-2 border-t border-sidebar-border flex items-center justify-between bg-sidebar-bg">
        <button 
          onClick={handleCreateCollection}
          className="flex items-center gap-2 px-3 py-1.5 text-xs hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground"
        >
          <Plus className="h-3.5 w-3.5" />
          New Collection
        </button>
        <div className="flex items-center gap-1">
          <button onClick={toggleTheme} className="p-1.5 hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground" title="Toggle theme">
            {theme === 'dark' ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
          </button>
          <button onClick={() => setDrawerOpen(true)} className="p-1.5 hover:bg-accent rounded-md transition-colors text-muted-foreground hover:text-foreground" title="Route Settings">
            <Settings className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
