'use client';

import React, { useState, useMemo } from 'react';
import { Folder, FolderOpen, Plus, Search, Settings, Loader2, Inbox, Globe, ChevronRight, ChevronDown, Sun, Moon, GitBranch, Server, Edit2, Copy, Trash2, X } from 'lucide-react';
import { useCollections } from '@/hooks/useCollections';
import { useRoutes } from '@/hooks/useRoutes';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useTheme } from '@/context/ThemeContext';
import { useDialogs } from '@/context/DialogContext';
import { toast } from 'sonner';
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
  const { collections, isLoading: isCollectionsLoading, createCollection, updateCollection, deleteCollection, addItemToCollection } = useCollections(userId);
  const { routes, isLoading: isRoutesLoading, createRoute } = useRoutes();
  const { setActiveRoute, activeRoute, setDrawerOpen, activeView, setActiveView } = useWorkspace();
  const { theme, toggleTheme } = useTheme();
  const { confirm, prompt } = useDialogs();
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedCollections, setExpandedCollections] = useState<Set<string>>(new Set());
  const [moveRouteMode, setMoveRouteMode] = useState<{ sourceCollection: any, item: any } | null>(null);

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

  const handleCreateCollection = async () => {
    const name = await prompt({ title: 'New Collection', placeholder: 'Collection Name' });
    if (name) {
      try {
        await createCollection({ name, userId });
        toast.success('Collection created');
      } catch (e: any) {
        toast.error('Failed to create collection: ' + e.message);
      }
    }
  };

  const handleRenameCollection = async (id: string, currentName: string) => {
    const newName = await prompt({ title: 'Rename Collection', defaultValue: currentName, placeholder: 'New Name' });
    if (newName && newName !== currentName) {
      try {
        await updateCollection({ id, name: newName });
        toast.success('Collection renamed');
      } catch (e: any) {
        toast.error('Failed to rename collection: ' + e.message);
      }
    }
  };

  const handleDeleteCollection = async (id: string, name: string) => {
    const confirmed = await confirm({ 
      title: 'Delete Collection', 
      message: `Are you sure you want to delete the collection "${name}"?`,
      confirmText: 'Delete'
    });
    if (confirmed) {
      try {
        await deleteCollection(id);
        toast.success('Collection deleted');
      } catch (e: any) {
        toast.error('Failed to delete collection: ' + e.message);
      }
    }
  };

  const handleCloneCollection = async (collection: any) => {
    try {
      const newCollection = await createCollection({ name: `${collection.name} (Copy)`, userId });
      if (newCollection && collection.items && collection.items.length > 0) {
        for (const item of collection.items) {
          await addItemToCollection({
            collectionId: newCollection.id,
            item: {
              protocol: item.protocol,
              resourceId: item.resourceId,
              name: item.name
            }
          });
        }
      }
      toast.success('Collection cloned');
    } catch (e: any) {
      toast.error('Failed to clone collection: ' + e.message);
    }
  };

  const handleRemoveItem = async (collection: any, itemId: string) => {
    const confirmed = await confirm({
      title: 'Remove Route',
      message: 'Remove this route from the collection?',
      confirmText: 'Remove'
    });
    if (confirmed) {
      try {
        const newItems = collection.items.filter((i: any) => i.id !== itemId);
        await updateCollection({ id: collection.id, items: newItems });
        toast.success('Route removed from collection');
      } catch (e: any) {
        toast.error('Failed to remove route: ' + e.message);
      }
    }
  };

  const handleMoveRoute = async (targetCollectionId: string) => {
    if (!moveRouteMode) return;
    const { sourceCollection, item } = moveRouteMode;
    
    if (sourceCollection.id === targetCollectionId) {
      setMoveRouteMode(null);
      return;
    }

    try {
      await addItemToCollection({
        collectionId: targetCollectionId,
        item: { protocol: item.protocol, resourceId: item.resourceId, name: item.name }
      });
      const newItems = sourceCollection.items.filter((i: any) => i.id !== item.id);
      await updateCollection({ id: sourceCollection.id, items: newItems });
      toast.success('Route moved');
    } catch (e: any) {
      toast.error('Failed to move route: ' + e.message);
    } finally {
      setMoveRouteMode(null);
    }
  };

  const handleAddRoute = async (collectionId?: string) => {
    const name = await prompt({ title: 'New Route', placeholder: 'Route Name' });
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
        toast.success('Route created');
      } catch (err: any) {
        toast.error("Failed to add route: " + err.message);
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
      
      {/* View Toggle */}
      <div className="flex p-2 gap-1 border-b border-sidebar-border bg-muted/20">
        <button 
          onClick={() => setActiveView('routes')}
          className={cn(
            "flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs rounded-md transition-colors", 
            activeView === 'routes' ? "bg-background shadow-sm text-foreground font-medium" : "text-muted-foreground hover:bg-accent/50 hover:text-foreground"
          )}
        >
          <Server className="h-3.5 w-3.5" /> Routes
        </button>
        <button 
          onClick={() => setActiveView('scenarios')}
          className={cn(
            "flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs rounded-md transition-colors", 
            activeView === 'scenarios' ? "bg-background shadow-sm text-foreground font-medium" : "text-muted-foreground hover:bg-accent/50 hover:text-foreground"
          )}
        >
          <GitBranch className="h-3.5 w-3.5" /> Scenarios
        </button>
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
                    <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <span
                        role="button"
                        tabIndex={0}
                        onClick={(e) => { e.stopPropagation(); handleAddRoute(collection.id); }}
                        className="p-0.5 hover:bg-background rounded text-muted-foreground hover:text-foreground cursor-pointer"
                        title="Add route"
                      >
                        <Plus className="h-3 w-3" />
                      </span>
                      <span
                        role="button"
                        tabIndex={0}
                        onClick={(e) => { e.stopPropagation(); handleRenameCollection(collection.id, collection.name); }}
                        className="p-0.5 hover:bg-background rounded text-muted-foreground hover:text-foreground cursor-pointer"
                        title="Rename collection"
                      >
                        <Edit2 className="h-3 w-3" />
                      </span>
                      <span
                        role="button"
                        tabIndex={0}
                        onClick={(e) => { e.stopPropagation(); handleCloneCollection(collection); }}
                        className="p-0.5 hover:bg-background rounded text-muted-foreground hover:text-foreground cursor-pointer"
                        title="Clone collection"
                      >
                        <Copy className="h-3 w-3" />
                      </span>
                      <span
                        role="button"
                        tabIndex={0}
                        onClick={(e) => { e.stopPropagation(); handleDeleteCollection(collection.id, collection.name); }}
                        className="p-0.5 hover:bg-destructive/20 rounded text-muted-foreground hover:text-destructive cursor-pointer"
                        title="Delete collection"
                      >
                        <Trash2 className="h-3 w-3" />
                      </span>
                    </div>
                  </button>
                  {isExpanded && collection.items && collection.items.length > 0 && (
                    <div className="ml-4 border-l border-border pl-1.5 space-y-0.5 mt-0.5">
                      {collection.items.map((item) => {
                        const route = routes.find(r => r.id === item.resourceId);
                        return (
                          <div key={item.id} className="flex items-center group/item w-full">
                            <button
                              onClick={() => handleSelectRoute(item.resourceId)}
                              className={cn(
                                "flex items-center gap-2 flex-1 px-2 py-1 text-xs rounded-sm transition-colors text-left",
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
                            <div className="flex items-center opacity-0 group-hover/item:opacity-100 pr-1">
                              <button 
                                onClick={(e) => { e.stopPropagation(); setMoveRouteMode({ sourceCollection: collection, item }); }} 
                                className="p-0.5 text-muted-foreground hover:text-foreground rounded" title="Move Route"
                              >
                                <Folder className="h-3 w-3" />
                              </button>
                              <button 
                                onClick={(e) => { e.stopPropagation(); handleRemoveItem(collection, item.id); }} 
                                className="p-0.5 text-muted-foreground hover:text-destructive rounded" title="Remove from Collection"
                              >
                                <X className="h-3 w-3" />
                              </button>
                            </div>
                          </div>
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

      {moveRouteMode && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-background/50 backdrop-blur-sm">
          <div className="bg-background border border-border rounded-lg shadow-xl w-[300px] p-4 flex flex-col gap-3">
            <h3 className="text-sm font-bold text-foreground">Move "{moveRouteMode.item.name}"</h3>
            <p className="text-xs text-muted-foreground mb-1">Select target collection:</p>
            <div className="max-h-60 overflow-y-auto space-y-1">
              {collections.map(c => (
                <button
                  key={c.id}
                  onClick={() => handleMoveRoute(c.id)}
                  disabled={c.id === moveRouteMode.sourceCollection.id}
                  className={cn(
                    "w-full text-left px-2 py-1.5 text-xs rounded-md transition-colors",
                    c.id === moveRouteMode.sourceCollection.id ? "opacity-50 cursor-not-allowed bg-accent/30" : "hover:bg-accent text-foreground"
                  )}
                >
                  <Folder className="h-3.5 w-3.5 inline-block mr-2 text-primary" />
                  {c.name} {c.id === moveRouteMode.sourceCollection.id && "(Current)"}
                </button>
              ))}
            </div>
            <div className="flex justify-end mt-2">
              <button 
                onClick={() => setMoveRouteMode(null)}
                className="px-3 py-1.5 text-xs font-medium bg-accent text-foreground rounded-md hover:bg-accent/80"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
