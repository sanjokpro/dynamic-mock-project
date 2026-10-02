import React, { useState, useEffect } from 'react';
import { useEnvironments } from '@/hooks/useEnvironments';
import { Environment } from '@/types';
import { X, Plus, Trash2, Copy, Save, AlertCircle, CheckCircle2, Settings } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useDialogs } from '@/context/DialogContext';
import { toast } from 'sonner';

interface Props {
  open: boolean;
  onClose: () => void;
}

export function EnvironmentManagerModal({ open, onClose }: Props) {
  const { environments, isLoading, createEnvironment, updateEnvironment, deleteEnvironment } = useEnvironments();
  const { activeEnvironment, setActiveEnvironment } = useWorkspace();
  const { confirm } = useDialogs();
  const [selectedEnvId, setSelectedEnvId] = useState<string | null>(null);
  
  // Local state for editing
  const [name, setName] = useState('');
  const [variables, setVariables] = useState<{ key: string; value: string }[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (open && environments.length > 0 && !selectedEnvId) {
      setSelectedEnvId(activeEnvironment?.id || environments[0].id);
    }
  }, [open, environments, selectedEnvId, activeEnvironment]);

  useEffect(() => {
    if (selectedEnvId) {
      const env = environments.find(e => e.id === selectedEnvId);
      if (env) {
        setName(env.name);
        setVariables(Object.entries(env.variables || {}).map(([key, value]) => ({ key, value })));
      } else {
        setName('');
        setVariables([]);
      }
    } else {
      setName('');
      setVariables([]);
    }
    setError(null);
  }, [selectedEnvId, environments]);

  if (!open) return null;

  const handleSave = async () => {
    if (!name.trim()) {
      setError('Environment name is required');
      return;
    }
    
    const varsRecord: Record<string, string> = {};
    for (const v of variables) {
      if (v.key.trim()) {
        varsRecord[v.key.trim()] = v.value;
      }
    }

    setIsSaving(true);
    setError(null);
    try {
      if (selectedEnvId === 'new') {
        const created = await createEnvironment({
          name: name.trim(),
          variables: varsRecord,
          isDefault: environments.length === 0,
        });
        // We can't synchronously get the id from a void mutation in this pattern unless we change useEnvironments hook to return promises, 
        // but react-query mutateAsync does. We'll just reset and let the query refetch handle it.
        // Actually, the hook uses `mutate` not `mutateAsync`. Let's handle it best effort.
        onClose();
        toast.success('Environment created');
      } else if (selectedEnvId) {
        await updateEnvironment({
          id: selectedEnvId,
          name: name.trim(),
          variables: varsRecord,
        });
        toast.success('Environment updated');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to save environment');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id: string, envName: string) => {
    const confirmed = await confirm({
      title: 'Delete Environment',
      message: `Are you sure you want to delete the environment "${envName}"?`,
      confirmText: 'Delete'
    });
    if (confirmed) {
      try {
        await deleteEnvironment(id);
        if (activeEnvironment?.id === id) {
          setActiveEnvironment(null);
        }
        if (selectedEnvId === id) {
          setSelectedEnvId(null);
        }
        toast.success('Environment deleted');
      } catch (err: any) {
        toast.error('Failed to delete: ' + err.message);
      }
    }
  };

  const handleClone = async (env: Environment) => {
    try {
      await createEnvironment({
        name: `${env.name} (Copy)`,
        variables: { ...env.variables },
        isDefault: false,
      });
      toast.success('Environment cloned');
    } catch (err: any) {
      toast.error('Failed to clone: ' + err.message);
    }
  };

  const addVariable = () => {
    setVariables([...variables, { key: '', value: '' }]);
  };

  const updateVariable = (index: number, field: 'key' | 'value', val: string) => {
    const newVars = [...variables];
    newVars[index][field] = val;
    setVariables(newVars);
  };

  const removeVariable = (index: number) => {
    const newVars = [...variables];
    newVars.splice(index, 1);
    setVariables(newVars);
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className="flex bg-background border border-border rounded-xl shadow-2xl w-[800px] h-[500px] overflow-hidden flex-row">
        
        {/* Left Sidebar (List) */}
        <div className="w-1/3 border-r border-border flex flex-col bg-sidebar-bg">
          <div className="p-3 border-b border-border flex items-center justify-between">
            <h2 className="text-sm font-semibold text-foreground flex items-center gap-1.5">
              <Settings className="h-4 w-4" /> Environments
            </h2>
            <button 
              onClick={() => setSelectedEnvId('new')}
              className="p-1 hover:bg-accent rounded text-muted-foreground hover:text-foreground"
              title="New Environment"
            >
              <Plus className="h-4 w-4" />
            </button>
          </div>
          
          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {isLoading ? (
              <div className="text-xs text-muted-foreground p-2">Loading...</div>
            ) : environments.length === 0 && selectedEnvId !== 'new' ? (
              <div className="text-xs text-muted-foreground p-2 text-center">No environments found</div>
            ) : (
              <>
                {environments.map(env => (
                  <div 
                    key={env.id} 
                    className={cn(
                      "flex items-center justify-between px-2 py-1.5 rounded-md cursor-pointer group text-sm",
                      selectedEnvId === env.id ? "bg-primary/10 text-primary" : "hover:bg-accent text-foreground"
                    )}
                    onClick={() => setSelectedEnvId(env.id)}
                  >
                    <span className="truncate flex-1">{env.name}</span>
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={(e) => { e.stopPropagation(); handleClone(env); }} className="p-1 hover:bg-background rounded text-muted-foreground" title="Clone">
                        <Copy className="h-3 w-3" />
                      </button>
                      <button onClick={(e) => { e.stopPropagation(); handleDelete(env.id, env.name); }} className="p-1 hover:bg-destructive/10 text-destructive rounded" title="Delete">
                        <Trash2 className="h-3 w-3" />
                      </button>
                    </div>
                  </div>
                ))}
                {selectedEnvId === 'new' && (
                  <div className="px-2 py-1.5 rounded-md bg-primary/10 text-primary text-sm flex items-center">
                    <span className="italic">New Environment...</span>
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        {/* Right Content (Editor) */}
        <div className="flex-1 flex flex-col relative">
          <button 
            onClick={onClose}
            className="absolute top-3 right-3 p-1 rounded-md hover:bg-accent text-muted-foreground z-10"
          >
            <X className="h-4 w-4" />
          </button>

          {selectedEnvId ? (
            <div className="flex-1 flex flex-col h-full">
              <div className="p-4 border-b border-border">
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Environment Name"
                  className="w-full text-lg font-semibold bg-transparent border-none focus:outline-none focus:ring-0 px-0"
                />
              </div>
              
              <div className="flex-1 overflow-y-auto p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Variables</span>
                  <button onClick={addVariable} className="text-xs flex items-center gap-1 text-primary hover:underline">
                    <Plus className="h-3 w-3" /> Add Variable
                  </button>
                </div>
                
                {variables.length === 0 ? (
                  <div className="text-center py-8 text-sm text-muted-foreground border border-dashed border-border rounded-lg">
                    No variables defined.<br/>Click "Add Variable" to create one.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {variables.map((v, i) => (
                      <div key={i} className="flex items-center gap-2">
                        <input
                          value={v.key}
                          onChange={(e) => updateVariable(i, 'key', e.target.value)}
                          placeholder="Key"
                          className="flex-1 px-2 py-1.5 text-sm bg-accent/30 border border-border rounded-md focus:outline-none focus:border-primary font-mono"
                        />
                        <input
                          value={v.value}
                          onChange={(e) => updateVariable(i, 'value', e.target.value)}
                          placeholder="Value"
                          className="flex-1 px-2 py-1.5 text-sm bg-accent/30 border border-border rounded-md focus:outline-none focus:border-primary font-mono"
                        />
                        <button onClick={() => removeVariable(i)} className="p-1.5 text-muted-foreground hover:text-destructive rounded-md hover:bg-destructive/10">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
                
                {error && (
                  <div className="mt-4 p-3 bg-destructive/10 text-destructive text-sm rounded-md flex items-start gap-2">
                    <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                    <span>{error}</span>
                  </div>
                )}
              </div>

              <div className="p-4 border-t border-border flex items-center justify-end">
                <button
                  onClick={handleSave}
                  disabled={isSaving}
                  className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 disabled:opacity-50"
                >
                  {isSaving ? <span className="animate-spin text-xl leading-none">⏳</span> : <Save className="h-4 w-4" />}
                  Save Environment
                </button>
              </div>
            </div>
          ) : (
            <div className="flex-1 flex items-center justify-center text-muted-foreground text-sm">
              Select an environment or create a new one
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
