'use client';

import React, { useState, useMemo } from 'react';
import { useScenarios } from '@/hooks/useScenarios';
import { useTheme } from '@/context/ThemeContext';
import { Loader2, GitBranch, Play, StopCircle, RotateCcw, Trash2, ArrowRight, Clock, Code, LayoutGrid, List } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  ReactFlow,
  Node,
  Edge,
  Background,
  Controls,
  MarkerType,
  useNodesState,
  useEdgesState,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

type ViewMode = 'cards' | 'flow';

export function ScenarioEditor() {
  const { scenarios, isLoading, updateScenario, deleteScenario } = useScenarios();
  const [selectedScenarioId, setSelectedScenarioId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('cards');

  if (isLoading) return <div className="p-4 flex justify-center"><Loader2 className="animate-spin" /></div>;

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <div className="flex items-center justify-between p-3 border-b bg-panel-header-bg">
        <div className="flex items-center gap-2">
          <GitBranch className="h-4 w-4 text-violet-500" />
          <h2 className="text-sm font-bold uppercase tracking-wider">Scenario State Machines</h2>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-[10px] text-muted-foreground">{scenarios.length} scenario{scenarios.length !== 1 ? 's' : ''}</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto custom-scrollbar p-4">
        {scenarios.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-3">
            <GitBranch className="h-12 w-12 opacity-10" />
            <p className="text-sm">No scenarios configured</p>
            <p className="text-[10px]">Create a scenario via the API or seed data</p>
          </div>
        ) : (
          <div className="space-y-4">
            {scenarios.map((scenario) => {
              const states: any[] = scenario.states || [];
              const currentState = scenario.currentState || scenario.initialState;
              const isActive = scenario.active;
              const isSelected = selectedScenarioId === scenario.id;

              return (
                <div
                  key={scenario.id}
                  className={cn(
                    "border rounded-lg overflow-hidden transition-all",
                    isSelected ? "border-primary ring-1 ring-primary/30" : "border-border hover:border-primary/50"
                  )}
                >
                  {/* Scenario Header */}
                  <div
                    className={cn(
                      "flex items-center justify-between px-4 py-2.5 cursor-pointer transition-colors",
                      isActive ? "bg-primary/5" : "bg-panel-header-bg"
                    )}
                    onClick={() => setSelectedScenarioId(isSelected ? null : scenario.id)}
                  >
                    <div className="flex items-center gap-3">
                      <div className={cn("w-2 h-2 rounded-full", isActive ? "bg-green-500" : "bg-muted-foreground")} />
                      <div>
                        <span className="text-sm font-semibold">{scenario.name}</span>
                        {scenario.description && (
                          <p className="text-[10px] text-muted-foreground mt-0.5">{scenario.description}</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-[10px] text-muted-foreground tabular-nums">{scenario.executionCount ?? 0} execs</span>
                      <span className={cn("text-[10px] font-medium px-1.5 py-0.5 rounded", isActive ? "bg-green-500/10 text-green-500" : "bg-muted text-muted-foreground")}>
                        {isActive ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                  </div>

                  {/* Expanded Content */}
                  {isSelected && (
                    <div className="border-t border-border">
                      {/* View Mode Toggle */}
                      {states.length > 1 && (
                        <div className="flex items-center gap-1 px-4 pt-3 pb-1">
                          <button
                            onClick={() => setViewMode('cards')}
                            className={cn("flex items-center gap-1 px-2 py-1 text-[10px] rounded transition-colors", viewMode === 'cards' ? "bg-accent text-foreground" : "text-muted-foreground hover:text-foreground")}
                          >
                            <List className="h-3 w-3" /> Cards
                          </button>
                          <button
                            onClick={() => setViewMode('flow')}
                            className={cn("flex items-center gap-1 px-2 py-1 text-[10px] rounded transition-colors", viewMode === 'flow' ? "bg-accent text-foreground" : "text-muted-foreground hover:text-foreground")}
                          >
                            <LayoutGrid className="h-3 w-3" /> Flow Diagram
                          </button>
                        </div>
                      )}

                      {viewMode === 'flow' && states.length > 1 ? (
                        <div className="h-64 border-t border-border">
                          <ScenarioFlowDiagram key={scenario.id + '-flow'} states={states} transitions={states.flatMap((s: any) => (s.transitions || []).map((t: any) => ({ from: s.name, ...t })))} currentState={currentState} />
                        </div>
                      ) : (
                        <>
                          {/* State Cards View */}
                          <div className="p-4">
                            <div className="flex items-start gap-2 overflow-x-auto pb-2 custom-scrollbar">
                              {states.map((state: any, idx: number) => (
                                <React.Fragment key={state.name}>
                                  <div className={cn(
                                    "min-w-[180px] max-w-[220px] border rounded-lg p-3 transition-colors shrink-0",
                                    currentState === state.name
                                      ? "border-primary bg-primary/5 ring-1 ring-primary/30"
                                      : "border-border bg-card"
                                  )}>
                                    <div className="flex items-center gap-2 mb-2">
                                      <div className={cn("w-2 h-2 rounded-full shrink-0", currentState === state.name ? "bg-primary animate-pulse" : "bg-muted-foreground")} />
                                      <span className={cn("text-xs font-bold", currentState === state.name ? "text-primary" : "text-foreground")}>{state.name}</span>
                                      {idx === 0 && <span className="text-[8px] text-muted-foreground uppercase bg-muted px-1 py-0.5 rounded">Start</span>}
                                      {state.transitions && state.transitions.length === 0 && <span className="text-[8px] text-muted-foreground uppercase bg-muted px-1 py-0.5 rounded">End</span>}
                                    </div>
                                    {state.description && <p className="text-[9px] text-muted-foreground mb-2">{state.description}</p>}
                                    <div className="space-y-1">
                                      {state.responseStatus && (
                                        <div className="text-[9px] text-muted-foreground flex items-center gap-1">
                                          <Code className="h-2.5 w-2.5" /> Status: {state.responseStatus}
                                        </div>
                                      )}
                                      {state.delayMs > 0 && (
                                        <div className="text-[9px] text-muted-foreground flex items-center gap-1">
                                          <Clock className="h-2.5 w-2.5" /> Delay: {state.delayMs}ms
                                        </div>
                                      )}
                                      {state.transitions && state.transitions.length > 0 && (
                                        <div className="text-[9px] text-muted-foreground flex items-center gap-1">
                                          <ArrowRight className="h-2.5 w-2.5" /> {state.transitions.length} transition{state.transitions.length !== 1 ? 's' : ''}
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                  {idx < states.length - 1 && (
                                    <div className="flex items-center pt-6 shrink-0">
                                      <ArrowRight className="h-4 w-4 text-muted-foreground/40" />
                                    </div>
                                  )}
                                </React.Fragment>
                              ))}
                            </div>
                          </div>

                          {/* Transitions Detail */}
                          {states.some((s: any) => s.transitions && s.transitions.length > 0) && (
                            <div className="px-4 pb-4">
                              <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-2 block">Transitions</span>
                              <div className="space-y-1">
                                {states.filter((s: any) => s.transitions && s.transitions.length > 0).map((state: any) =>
                                  state.transitions!.map((t: any, ti: number) => (
                                    <div key={`${state.name}-${ti}`} className="flex items-center gap-2 text-xs px-3 py-1.5 rounded bg-muted/30 border border-border/50">
                                      <span className="font-medium text-foreground">{state.name}</span>
                                      <ArrowRight className="h-3 w-3 text-muted-foreground" />
                                      <span className="font-medium text-primary">{t.targetState}</span>
                                      {t.condition && (
                                        <><span className="text-muted-foreground">when</span><span className="font-mono text-[10px] text-muted-foreground bg-muted px-1 rounded">{t.condition}</span></>
                                      )}
                                      {t.priority !== undefined && t.priority !== null && <span className="text-[9px] text-muted-foreground ml-auto">p{t.priority}</span>}
                                    </div>
                                  ))
                                )}
                              </div>
                            </div>
                          )}
                        </>
                      )}

                      {/* Actions */}
                      <div className="px-4 pb-4 flex items-center gap-2">
                        {isActive ? (
                          <button onClick={async () => { try { await updateScenario({ id: scenario.id, active: false }); } catch (e) { console.error(e); } }}
                            className="flex items-center gap-1.5 px-3 py-1.5 text-xs rounded-md bg-yellow-500/10 text-yellow-600 hover:bg-yellow-500/20 transition-colors">
                            <StopCircle className="h-3 w-3" /> Deactivate
                          </button>
                        ) : (
                          <button onClick={async () => { try { await updateScenario({ id: scenario.id, active: true }); } catch (e) { console.error(e); } }}
                            className="flex items-center gap-1.5 px-3 py-1.5 text-xs rounded-md bg-green-500/10 text-green-600 hover:bg-green-500/20 transition-colors">
                            <Play className="h-3 w-3" /> Activate
                          </button>
                        )}
                        <button onClick={async () => { try { await updateScenario({ id: scenario.id, currentState: scenario.initialState, executionCount: 0 }); } catch (e) { console.error(e); } }}
                          className="flex items-center gap-1.5 px-3 py-1.5 text-xs rounded-md bg-accent text-foreground hover:bg-accent/80 transition-colors">
                          <RotateCcw className="h-3 w-3" /> Reset
                        </button>
                        <button onClick={async () => { if (confirm(`Delete scenario "${scenario.name}"?`)) { try { await deleteScenario(scenario.id); setSelectedScenarioId(null); } catch (e) { console.error(e); } } }}
                          className="flex items-center gap-1.5 px-3 py-1.5 text-xs rounded-md bg-destructive/10 text-destructive hover:bg-destructive/20 transition-colors ml-auto">
                          <Trash2 className="h-3 w-3" /> Delete
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function ScenarioFlowDiagram({ states, transitions, currentState }: { states: any[]; transitions: any[]; currentState: string }) {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const nodes: Node[] = useMemo(() => {
    const spacing = 220;
    const startY = 80;
    return states.map((state: any, idx: number) => ({
      id: state.name,
      type: 'default',
      position: { x: 40 + idx * spacing, y: startY },
      data: {
        label: state.name,
        description: state.description,
        isCurrent: currentState === state.name,
        isStart: idx === 0,
        isEnd: !state.transitions || state.transitions.length === 0,
      },
      style: {
        background: currentState === state.name
          ? (isDark ? '#1e40af' : '#dbeafe')
          : (isDark ? '#1e293b' : '#f1f5f9'),
        color: isDark ? '#e2e8f0' : '#0f172a',
        border: currentState === state.name
          ? `2px solid ${isDark ? '#3b82f6' : '#2563eb'}`
          : `1px solid ${isDark ? '#334155' : '#e2e8f0'}`,
        borderRadius: '8px',
        padding: '10px 16px',
        fontSize: '12px',
        fontFamily: 'var(--font-geist-mono), monospace',
        minWidth: '140px',
        textAlign: 'center' as const,
      },
    }));
  }, [states, currentState, isDark]);

  const edges: Edge[] = useMemo(() => {
    return transitions.map((t: any, idx: number) => ({
      id: `e-${t.from}-${t.targetState}-${idx}`,
      source: t.from,
      target: t.targetState,
      label: t.condition || '',
      markerEnd: { type: MarkerType.ArrowClosed, color: '#64748b' },
      style: {
        stroke: '#64748b',
        strokeWidth: 1.5,
      },
      labelStyle: {
        fontSize: 10,
        fill: '#94a3b8',
      },
      labelBgStyle: {
        fill: '#0b1120',
        fillOpacity: 0.8,
      },
      animated: true,
    }));
  }, [transitions]);

  const [flowNodes, , onNodesChange] = useNodesState(nodes);
  const [flowEdges, , onEdgesChange] = useEdgesState(edges);

  return (
    <ReactFlow
      nodes={flowNodes}
      edges={flowEdges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      fitView
      fitViewOptions={{ padding: 0.3 }}
      nodesDraggable={true}
      panOnDrag={true}
      zoomOnScroll={true}
      minZoom={0.5}
      maxZoom={2}
      className="bg-card"
    >
      <Background color="#334155" gap={20} />
      <Controls showInteractive={false} className="[&>button]:bg-card [&>button]:border-border [&>button]:text-muted-foreground" />
    </ReactFlow>
  );
}
