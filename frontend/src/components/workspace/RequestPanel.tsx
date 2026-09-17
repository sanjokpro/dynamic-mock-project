'use client';

import React, { useState, useEffect } from 'react';
import { Play, Save, ChevronDown, Loader2, Plus, Trash2, Check } from 'lucide-react';
import { useTestClient } from '@/hooks/useTestClient';
import { useRoutes } from '@/hooks/useRoutes';
import { useWorkspace } from '@/context/WorkspaceContext';
import { CodeEditor } from './CodeEditor';
import { MockRoute } from '@/types';
import { ProtocolEditor } from './protocols/ProtocolEditor';
import { ScenarioEditor } from './scenarios/ScenarioEditor';

type Tab = 'params' | 'headers' | 'request-body' | 'matchers' | 'body' | 'scripts';

export function RequestPanel() {
  const [activeTab, setActiveTab] = useState<Tab>('params');
  const { activeRoute, setActiveRoute, setLastResponse, activeEnvironment } = useWorkspace();
  const { updateRoute, isLoading: isSaving } = useRoutes();
  const { execute, isLoading: isExecuting } = useTestClient();

  const [method, setMethod] = useState('GET');
  const [path, setPath] = useState('');
  const [url, setUrl] = useState('{{BASE_URL}}/hello');
  const [responseStatus, setResponseStatus] = useState(200);
  const [responseTemplate, setResponseTemplate] = useState('');
  const [preScript, setPreScript] = useState('');
  const [postScript, setPostScript] = useState('');
  const [queryParams, setQueryParams] = useState<Record<string, string>>({});
  const [responseHeaders, setResponseHeaders] = useState<Record<string, string>>({});
  const [requestBodyType, setRequestBodyType] = useState<'none' | 'json' | 'form' | 'raw'>('none');
  const [requestBodyContent, setRequestBodyContent] = useState('');
  const [matchers, setMatchers] = useState<{
    headers?: Record<string, string>;
    queryParams?: Record<string, string>;
    bodyMatchType?: string;
    bodyMatchPattern?: string;
  }>({});
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    console.log("RequestPanel activeRoute changed:", activeRoute);
    if (activeRoute) {
      setMethod(activeRoute.method || 'GET');
      const path = activeRoute.path || '';
      setPath(path);
      setUrl(`{{BASE_URL}}${path.startsWith('/') ? path : '/' + path}`);
      setResponseStatus(activeRoute.responseStatus || 200);
      setResponseTemplate(activeRoute.responseTemplate || '');
      setPreScript(activeRoute.preScript || '');
      setPostScript(activeRoute.postScript || '');
      setQueryParams(activeRoute.queryParams || {});
      setResponseHeaders(activeRoute.responseHeaders || {});
      setMatchers(activeRoute.matchers || {});
      setRequestBodyType('none');
      setRequestBodyContent('');
    }
  }, [activeRoute]);

  const handleSave = async () => {
    if (!activeRoute) return;
    
    const updatedRoute: Partial<MockRoute> & { id: string } = {
      id: activeRoute.id,
      method: method as any,
      path,
      responseStatus,
      responseTemplate,
      queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined,
      responseHeaders: Object.keys(responseHeaders).length > 0 ? responseHeaders : undefined,
      matchers: (matchers.headers && Object.keys(matchers.headers).length > 0) ||
                (matchers.queryParams && Object.keys(matchers.queryParams).length > 0) ||
                (matchers.bodyMatchType && matchers.bodyMatchType !== 'none') ? matchers : undefined,
      preScript,
      postScript,
      scriptLanguage: 'js'
    };

    try {
      const saved = await updateRoute(updatedRoute);
      setActiveRoute(saved);
      setIsSuccess(true);
      setTimeout(() => setIsSuccess(false), 2000);
    } catch (error) {
      console.error('Failed to save route:', error);
    }
  };

  const handleSend = () => {
    const headers: Record<string, string> = {};
    if (activeEnvironment) {
      headers['X-Dynamic-Mock-Env'] = activeEnvironment.id;
    }

    const body = requestBodyType !== 'none' && requestBodyContent ? requestBodyContent : undefined;
    if (requestBodyType === 'json' && body) {
      headers['Content-Type'] = 'application/json';
    } else if (requestBodyType === 'raw' && body) {
      headers['Content-Type'] = 'text/plain';
    } else if (requestBodyType === 'form' && body) {
      headers['Content-Type'] = 'application/x-www-form-urlencoded';
    }

    execute({ url, method, headers, body }, {
      onSuccess: (data) => {
        setLastResponse(data);
      }
    });
  };

  if (!activeRoute) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-4 bg-background">
        <div className="p-4 bg-muted/20 rounded-full">
          <Play className="h-8 w-8 opacity-20" />
        </div>
        <p className="text-sm">Select a stub from the sidebar to start editing</p>
      </div>
    );
  }

  // Handle Protocol Parity Views
  const isProtocolStub = activeRoute.protocol && activeRoute.protocol !== 'HTTP';
  
  if (isProtocolStub) {
    return <ProtocolEditor key={activeRoute.id} protocol={activeRoute.protocol} endpointId={activeRoute.id} />;
  }
  
  if (activeRoute.scenarioName) {
     return <ScenarioEditor />;
  }

  return (
    <div className="flex flex-col h-full bg-background">
      <div className="flex items-center justify-between p-2 border-b bg-muted/20">
        <div className="flex items-center gap-2 flex-1">
          <div className="flex items-center bg-background border rounded-md overflow-hidden">
            <select 
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              className="px-3 py-1.5 text-xs font-bold text-blue-500 bg-transparent border-r hover:bg-accent transition-colors appearance-none cursor-pointer outline-none"
            >
              {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].map(m => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            <div className="px-2 text-muted-foreground text-xs font-mono">/mock</div>
            <input 
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="{{BASE_URL}}/api/example"
              className="px-3 py-1.5 text-sm bg-transparent border-none focus:outline-none w-96 font-mono"
            />
          </div>
          <button 
            onClick={handleSend}
            disabled={isExecuting}
            className="bg-primary text-primary-foreground px-4 py-1.5 rounded-md text-sm font-medium flex items-center gap-2 hover:bg-primary/90 transition-colors disabled:opacity-50"
          >
            {isExecuting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4 fill-current" />}
            Send
          </button>
        </div>
        <div className="flex items-center gap-2">
          <button 
            onClick={handleSave}
            disabled={isSaving}
            className={cn(
              "flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all",
              isSuccess ? "bg-green-500 text-white" : "bg-accent hover:bg-accent/80 text-foreground"
            )}
            title="Save changes"
          >
            {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : (isSuccess ? <Check className="h-4 w-4" /> : <Save className="h-4 w-4" />)}
            {isSuccess ? 'Saved' : 'Save'}
          </button>
        </div>
      </div>

      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex border-b text-sm bg-muted/5">
          {[
            { id: 'params', label: 'Params' },
            { id: 'headers', label: 'Headers' },
            { id: 'request-body', label: 'Body' },
            { id: 'matchers', label: 'Matchers' },
            { id: 'body', label: 'Response Body' },
            { id: 'scripts', label: 'Scripts' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as Tab)}
              className={cn(
                "px-4 py-2 transition-colors",
                activeTab === tab.id ? "border-b-2 border-primary font-medium text-foreground" : "text-muted-foreground hover:text-foreground"
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div className="flex-1 overflow-auto">
          {activeTab === 'params' && <div className="p-4"><KeyValueEditor title="Query Parameters" entries={queryParams} onChange={setQueryParams} /></div>}
          {activeTab === 'headers' && (
            <div className="p-4 space-y-6">
              <div className="flex items-center gap-4">
                <span className="text-xs font-bold text-muted-foreground uppercase">Status Code:</span>
                <input 
                  type="number"
                  value={responseStatus}
                  onChange={(e) => setResponseStatus(parseInt(e.target.value))}
                  className="w-20 bg-background border rounded px-2 py-1 text-sm outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
              <KeyValueEditor title="Response Headers" entries={responseHeaders} onChange={setResponseHeaders} />
            </div>
          )}
          {activeTab === 'request-body' && (
            <div className="p-4 space-y-4">
              <div className="flex items-center gap-3">
                <span className="text-xs font-bold text-muted-foreground uppercase">Body Type:</span>
                <select
                  value={requestBodyType}
                  onChange={(e) => setRequestBodyType(e.target.value as any)}
                  className="bg-background border rounded px-2 py-1 text-xs focus:ring-1 focus:ring-primary outline-none"
                >
                  <option value="none">No Body</option>
                  <option value="json">JSON</option>
                  <option value="form">Form Data</option>
                  <option value="raw">Raw</option>
                </select>
              </div>
              {requestBodyType !== 'none' && (
                <div className="h-[200px] border rounded-md overflow-hidden">
                  <CodeEditor 
                    value={requestBodyContent}
                    onChange={(val) => setRequestBodyContent(val || '')}
                    language={requestBodyType === 'json' ? 'json' : 'javascript'}
                    enableValidation={requestBodyType === 'json'}
                  />
                </div>
              )}
              {requestBodyType === 'form' && (
                <div className="text-xs text-muted-foreground italic">
                  Enter form data as key=value pairs, one per line.
                </div>
              )}
            </div>
          )}
          {activeTab === 'matchers' && <div className="p-4"><MatcherEditor matchers={matchers} onChange={setMatchers} /></div>}
          {activeTab === 'body' && (
            <div className="h-full">
              <CodeEditor 
                value={responseTemplate} 
                onChange={(val) => setResponseTemplate(val || '')}
                language="json" 
              />
            </div>
          )}
          {activeTab === 'scripts' && (
            <div className="h-full flex flex-col p-4 gap-4 overflow-hidden">
              <div className="flex-1 flex flex-col gap-2 overflow-hidden">
                <span className="text-xs font-bold text-muted-foreground uppercase">Pre-Request Script (JS)</span>
                <div className="flex-1 border rounded-md overflow-hidden">
                  <CodeEditor 
                    value={preScript} 
                    onChange={(val) => setPreScript(val || '')}
                    language="javascript" 
                  />
                </div>
              </div>
              <div className="flex-1 flex flex-col gap-2 overflow-hidden">
                <span className="text-xs font-bold text-muted-foreground uppercase">Post-Response Script (JS)</span>
                <div className="flex-1 border rounded-md overflow-hidden">
                  <CodeEditor 
                    value={postScript} 
                    onChange={(val) => setPostScript(val || '')}
                    language="javascript" 
                  />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function KeyValueEditor({ title, entries = {}, onChange }: { title: string; entries?: Record<string, string>; onChange?: (entries: Record<string, string>) => void }) {
  const [rows, setRows] = useState<Array<{ key: string; value: string }>>(() =>
    Object.entries(entries).map(([key, value]) => ({ key, value }))
  );

  useEffect(() => {
    setRows(Object.entries(entries).map(([key, value]) => ({ key, value })));
  }, [entries]);

  const emitChange = (newRows: Array<{ key: string; value: string }>) => {
    const result: Record<string, string> = {};
    newRows.forEach(({ key, value }) => {
      if (key.trim()) {
        result[key.trim()] = value;
      }
    });
    onChange?.(result);
  };

  const addRow = () => {
    const newRows = [...rows, { key: '', value: '' }];
    setRows(newRows);
    emitChange(newRows);
  };

  const updateRow = (index: number, field: 'key' | 'value', val: string) => {
    const newRows = rows.map((row, i) => (i === index ? { ...row, [field]: val } : row));
    setRows(newRows);
    emitChange(newRows);
  };

  const deleteRow = (index: number) => {
    const newRows = rows.filter((_, i) => i !== index);
    setRows(newRows);
    emitChange(newRows);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">{title}</span>
        <button onClick={addRow} className="flex items-center gap-1 text-xs text-primary hover:underline transition-colors">
          <Plus className="h-3 w-3" /> Add Row
        </button>
      </div>
      <div className="space-y-2">
        {rows.length === 0 && (
          <div className="text-[10px] text-muted-foreground italic py-1">No entries defined.</div>
        )}
        {rows.map((row, index) => (
          <div key={index} className="flex items-center gap-2 group">
            <input
              placeholder="Key"
              value={row.key}
              onChange={(e) => updateRow(index, 'key', e.target.value)}
              className="flex-1 bg-background border rounded px-2 py-1 text-xs focus:ring-1 focus:ring-primary outline-none"
            />
            <input
              placeholder="Value"
              value={row.value}
              onChange={(e) => updateRow(index, 'value', e.target.value)}
              className="flex-1 bg-background border rounded px-2 py-1 text-xs focus:ring-1 focus:ring-primary outline-none"
            />
            <button
              onClick={() => deleteRow(index)}
              className="p-1 text-muted-foreground opacity-0 group-hover:opacity-100 hover:text-destructive transition-all"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

function MatcherEditor({ matchers, onChange }: { matchers: Record<string, any>; onChange: (m: Record<string, any>) => void }) {
  const headerEntries = Object.entries((matchers as any)?.headers || {}).map(([k, v]) => ({ key: k, value: v as string }));
  const queryEntries = Object.entries((matchers as any)?.queryParams || {}).map(([k, v]) => ({ key: k, value: v as string }));
  const bodyMatchType = (matchers as any)?.bodyMatchType || 'none';
  const bodyMatchPattern = (matchers as any)?.bodyMatchPattern || '';

  const update = (patch: Record<string, any>) => {
    onChange({ ...matchers, ...patch });
  };

  return (
    <div className="space-y-6">
      <div>
        <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider block mb-4">Request Matchers</span>
        <div className="space-y-3">
          <MatcherTypeCard
            title="Headers"
            entries={headerEntries}
            onAdd={() => {
              const current = { ...((matchers as any)?.headers || {}) };
              update({ headers: { ...current, ['']: '' } });
            }}
            onUpdate={(entries) => {
              const result: Record<string, string> = {};
              entries.forEach(({ key, value }: { key: string; value: string }) => {
                if (key.trim()) result[key.trim()] = value;
              });
              update({ headers: Object.keys(result).length > 0 ? result : undefined });
            }}
          />
          <MatcherTypeCard
            title="Query Params"
            entries={queryEntries}
            onAdd={() => {
              const current = { ...((matchers as any)?.queryParams || {}) };
              update({ queryParams: { ...current, ['']: '' } });
            }}
            onUpdate={(entries) => {
              const result: Record<string, string> = {};
              entries.forEach(({ key, value }: { key: string; value: string }) => {
                if (key.trim()) result[key.trim()] = value;
              });
              update({ queryParams: Object.keys(result).length > 0 ? result : undefined });
            }}
          />
          <div className="border rounded-lg p-3 bg-muted/10">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Body Matchers</span>
            </div>
            <div className="space-y-2">
              <select
                value={bodyMatchType}
                onChange={(e) => update({ bodyMatchType: e.target.value || 'none' })}
                className="w-full bg-background border rounded px-2 py-1 text-xs focus:ring-1 focus:ring-primary outline-none"
              >
                <option value="none">No body matching</option>
                <option value="equals">Equals</option>
                <option value="contains">Contains</option>
                <option value="regex">Regex</option>
                <option value="jsonpath">JSONPath</option>
                <option value="xpath">XPath</option>
              </select>
              {bodyMatchType !== 'none' && (
                <div className="flex items-center gap-2">
                  <input
                    placeholder={bodyMatchType === 'jsonpath' ? '$.field == value' : bodyMatchType === 'xpath' ? '//*[local-name()=\'id\'] == 123' : bodyMatchType === 'regex' ? '^pattern$' : 'value'}
                    value={bodyMatchPattern}
                    onChange={(e) => update({ bodyMatchPattern: e.target.value })}
                    className="flex-1 bg-background border rounded px-2 py-1 text-xs font-mono focus:ring-1 focus:ring-primary outline-none"
                  />
                  {bodyMatchPattern && (
                    <button
                      onClick={() => update({ bodyMatchPattern: '' })}
                      className="p-1 text-muted-foreground hover:text-destructive transition-colors"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function MatcherTypeCard({ title, entries, onAdd, onUpdate }: { title: string; entries: Array<{ key: string; value: string }>; onAdd: () => void; onUpdate: (entries: Array<{ key: string; value: string }>) => void }) {
  return (
    <div className="border rounded-lg p-3 bg-muted/10">
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-medium">{title} Matchers</span>
        <button onClick={onAdd} className="text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded hover:bg-primary/20 transition-colors">Add Matcher</button>
      </div>
      {entries.length === 0 && (
        <div className="text-[10px] text-muted-foreground italic">No matchers defined for {title.toLowerCase()}.</div>
      )}
      {entries.map((entry, idx) => (
        <div key={idx} className="flex items-center gap-2 mt-2 group">
          <input
            placeholder="Field name"
            value={entry.key}
            onChange={(e) => {
              const updated = entries.map((r, i) => i === idx ? { ...r, key: e.target.value } : r);
              onUpdate(updated);
            }}
            className="flex-1 bg-background border rounded px-2 py-1 text-xs font-mono focus:ring-1 focus:ring-primary outline-none"
          />
          <span className="text-xs text-muted-foreground">~</span>
          <input
            placeholder="Regex pattern"
            value={entry.value}
            onChange={(e) => {
              const updated = entries.map((r, i) => i === idx ? { ...r, value: e.target.value } : r);
              onUpdate(updated);
            }}
            className="flex-1 bg-background border rounded px-2 py-1 text-xs font-mono focus:ring-1 focus:ring-primary outline-none"
          />
          <button
            onClick={() => onUpdate(entries.filter((_, i) => i !== idx))}
            className="p-1 text-muted-foreground opacity-0 group-hover:opacity-100 hover:text-destructive transition-all"
          >
            <Trash2 className="h-3 w-3" />
          </button>
        </div>
      ))}
    </div>
  );
}

import { cn } from '@/lib/utils';
