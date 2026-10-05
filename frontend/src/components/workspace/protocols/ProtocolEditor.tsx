'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Protocol } from '@/types';
import { useProtocolEndpoints } from '@/hooks/useProtocolEndpoints';
import { CodeEditor } from '../CodeEditor';
import { Loader2, Save, Check, Plus, Trash2, ChevronDown, ChevronUp, Search, SendHorizontal, RefreshCw, Copy, Terminal } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useScenarios } from '@/hooks/useScenarios';
import { useIso8583Simulator } from '@/hooks/useIso8583';
import iso8583Dict from '@/data/iso8583-fields.json';

// ===================== ISO8583 Dictionary Helpers =====================

type FieldDef = { name: string; abbr: string; format: string; description: string };
const fields: Record<string, FieldDef> = (iso8583Dict as any).fields;
const mtiDescriptions: Record<string, string> = (iso8583Dict as any).mtiDescriptions;
const commonResponseCodes: Record<string, string> = (iso8583Dict as any).commonResponseCodes;

function getFieldLabel(fieldNum: string): string {
  const def = fields[fieldNum];
  if (!def) return fieldNum;
  return `${fieldNum} — ${def.name} (${def.abbr})`;
}

function getFieldDescription(fieldNum: string): string {
  const def = fields[fieldNum];
  if (!def) return '';
  return `Format: ${def.format}\n${def.description}`;
}

interface ProtocolEditorProps {
  protocol: Protocol;
  endpointId: string;
}

export function ProtocolEditor({ protocol, endpointId }: ProtocolEditorProps) {
  const { endpoints, isLoading, updateEndpoint } = useProtocolEndpoints(protocol);
  const endpoint = endpoints.find(e => e.id === endpointId);

  if (isLoading) return <div className="p-4 flex justify-center"><Loader2 className="animate-spin" /></div>;
  if (!endpoint) return <div className="p-4 text-sm text-muted-foreground">Endpoint not found.</div>;

  switch (protocol) {
    case 'GRAPHQL':
      return <GraphQLEditor endpoint={endpoint} onSave={updateEndpoint} />;
    case 'GRPC':
      return <GrpcEditor endpoint={endpoint} onSave={updateEndpoint} />;
    case 'ISO8583':
      return <Iso8583Editor endpoint={endpoint} onSave={updateEndpoint} />;
    default:
      return (
        <div className="h-full flex flex-col p-4 gap-4">
          <h2 className="text-sm font-bold uppercase tracking-wider">{protocol} Configuration</h2>
          <div className="flex-1 border rounded-md overflow-hidden">
            <CodeEditor value={JSON.stringify(endpoint, null, 2)} language="json" readOnly={false} />
          </div>
        </div>
      );
  }
}

// ===================== GraphQL Editor =====================

function GraphQLEditor({ endpoint, onSave }: { endpoint: any; onSave: (data: any) => Promise<any> }) {
  const [schema, setSchema] = useState(endpoint.schema || '');
  const [resolvers, setResolvers] = useState<any[]>(endpoint.resolvers || []);
  const [isSuccess, setIsSuccess] = useState(false);
  const [expandedResolver, setExpandedResolver] = useState<number | null>(null);

  const handleSave = async () => {
    try {
      const saved = await onSave({ id: endpoint.id, schema, resolvers });
      if (saved?.schema !== undefined) setSchema(saved.schema);
      if (saved?.resolvers !== undefined) setResolvers(saved.resolvers);
      setIsSuccess(true);
      setTimeout(() => setIsSuccess(false), 2000);
    } catch (e) {
      console.error('Failed to save GraphQL endpoint:', e);
    }
  };

  const addResolver = () => {
    setResolvers([...resolvers, { operationType: 'Query', fieldName: '', responseTemplate: '', script: '', delayMs: 0 }]);
    setExpandedResolver(resolvers.length);
  };

  const updateResolver = (index: number, field: string, value: any) => {
    setResolvers(resolvers.map((r, i) => i === index ? { ...r, [field]: value } : r));
  };

  const removeResolver = (index: number) => {
    setResolvers(resolvers.filter((_, i) => i !== index));
  };

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <div className="flex items-center justify-between p-2 border-b bg-muted/20">
        <h2 className="text-sm font-bold uppercase tracking-wider">GraphQL Configuration</h2>
        <button onClick={handleSave} className={cn("flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all", isSuccess ? "bg-green-500 text-white" : "bg-accent hover:bg-accent/80 text-foreground")}>
          {isSuccess ? <><Check className="h-4 w-4" /> Saved</> : <><Save className="h-4 w-4" /> Save</>}
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4 space-y-6">
        <div className="space-y-2">
          <label className="text-xs font-bold text-muted-foreground uppercase">Schema (SDL)</label>
          <div className="h-48 border rounded-md overflow-hidden">
            <CodeEditor value={schema} onChange={(v) => setSchema(v || '')} language="graphql" />
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-muted-foreground uppercase">Resolvers</label>
            <button onClick={addResolver} className="flex items-center gap-1 text-xs text-primary hover:underline">
              <Plus className="h-3 w-3" /> Add Resolver
            </button>
          </div>
          {resolvers.length === 0 && <p className="text-xs text-muted-foreground italic">No resolvers defined.</p>}
          {resolvers.map((resolver, idx) => (
            <div key={idx} className="border rounded-lg p-3 bg-muted/10 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <select value={resolver.operationType} onChange={(e) => updateResolver(idx, 'operationType', e.target.value)}
                    className="text-[10px] font-bold bg-primary/10 text-primary px-2 py-0.5 rounded border-0 outline-none">
                    <option>Query</option><option>Mutation</option><option>Subscription</option>
                  </select>
                  <span className="text-sm font-mono">{resolver.fieldName || 'Unnamed'}</span>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => setExpandedResolver(expandedResolver === idx ? null : idx)} className="p-1 text-muted-foreground hover:text-foreground">
                    {expandedResolver === idx ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                  </button>
                  <button onClick={() => removeResolver(idx)} className="p-1 text-muted-foreground hover:text-destructive">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              {expandedResolver === idx && (
                <div className="space-y-2 pt-2 border-t">
                  <input placeholder="Field name (e.g., hello)" value={resolver.fieldName} onChange={(e) => updateResolver(idx, 'fieldName', e.target.value)}
                    className="w-full bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary" />
                  <div>
                    <span className="text-[10px] text-muted-foreground">Response Template (Handlebars)</span>
                    <div className="h-24 border rounded-md overflow-hidden mt-1">
                      <CodeEditor value={resolver.responseTemplate || ''} onChange={(v) => updateResolver(idx, 'responseTemplate', v || '')} language="json" />
                    </div>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground">Script (JS/Python)</span>
                    <div className="h-24 border rounded-md overflow-hidden mt-1">
                      <CodeEditor value={resolver.script || ''} onChange={(v) => updateResolver(idx, 'script', v || '')} language="javascript" />
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div>
                      <span className="text-[10px] text-muted-foreground">Delay (ms)</span>
                      <input type="number" value={resolver.delayMs || 0} onChange={(e) => updateResolver(idx, 'delayMs', parseInt(e.target.value) || 0)}
                        className="w-20 bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary ml-2" />
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Language</span>
                      <select value={resolver.scriptLanguage || 'js'} onChange={(e) => updateResolver(idx, 'scriptLanguage', e.target.value)}
                        className="bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary ml-2">
                        <option value="js">JavaScript</option>
                        <option value="python">Python</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ===================== gRPC Editor =====================

function GrpcEditor({ endpoint, onSave }: { endpoint: any; onSave: (data: any) => Promise<any> }) {
  const [serviceName, setServiceName] = useState(endpoint.serviceName || '');
  const [protoSchema, setProtoSchema] = useState(endpoint.protoSchema || '');
  const [port, setPort] = useState(endpoint.port || 50051);
  const [methods, setMethods] = useState<any[]>(endpoint.methods || []);
  const [isSuccess, setIsSuccess] = useState(false);
  const [expandedMethod, setExpandedMethod] = useState<number | null>(null);

  const handleSave = async () => {
    try {
      const saved = await onSave({ id: endpoint.id, serviceName, protoSchema, port, methods });
      if (saved?.serviceName !== undefined) setServiceName(saved.serviceName);
      if (saved?.protoSchema !== undefined) setProtoSchema(saved.protoSchema);
      if (saved?.port !== undefined) setPort(saved.port);
      if (saved?.methods !== undefined) setMethods(saved.methods);
      setIsSuccess(true);
      setTimeout(() => setIsSuccess(false), 2000);
    } catch (e) {
      console.error('Failed to save gRPC endpoint:', e);
    }
  };

  const addMethod = () => {
    setMethods([...methods, { methodName: '', methodType: 'UNARY', responseTemplate: '', script: '', scriptLanguage: 'js', delayMs: 0, statusCode: '0', errorMessage: '' }]);
    setExpandedMethod(methods.length);
  };

  const updateMethod = (index: number, field: string, value: any) => {
    setMethods(methods.map((m, i) => i === index ? { ...m, [field]: value } : m));
  };

  const removeMethod = (index: number) => {
    setMethods(methods.filter((_, i) => i !== index));
  };

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <div className="flex items-center justify-between p-2 border-b bg-muted/20">
        <h2 className="text-sm font-bold uppercase tracking-wider">gRPC Configuration</h2>
        <button onClick={handleSave} className={cn("flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all", isSuccess ? "bg-green-500 text-white" : "bg-accent hover:bg-accent/80 text-foreground")}>
          {isSuccess ? <><Check className="h-4 w-4" /> Saved</> : <><Save className="h-4 w-4" /> Save</>}
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-xs font-bold text-muted-foreground uppercase">Service Name</label>
            <input value={serviceName} onChange={(e) => setServiceName(e.target.value)} placeholder="com.example.UserService"
              className="w-full bg-background border rounded px-2 py-1 text-sm font-mono outline-none focus:ring-1 focus:ring-primary" />
          </div>
          <div className="space-y-2">
            <label className="text-xs font-bold text-muted-foreground uppercase">Port</label>
            <input type="number" value={port} onChange={(e) => setPort(parseInt(e.target.value) || 50051)}
              className="w-full bg-background border rounded px-2 py-1 text-sm outline-none focus:ring-1 focus:ring-primary" />
          </div>
        </div>

        <div className="space-y-2">
          <label className="text-xs font-bold text-muted-foreground uppercase">Proto Schema</label>
          <div className="h-32 border rounded-md overflow-hidden">
            <CodeEditor value={protoSchema} onChange={(v) => setProtoSchema(v || '')} language="protobuf" />
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-muted-foreground uppercase">Methods</label>
            <button onClick={addMethod} className="flex items-center gap-1 text-xs text-primary hover:underline">
              <Plus className="h-3 w-3" /> Add Method
            </button>
          </div>
          {methods.length === 0 && <p className="text-xs text-muted-foreground italic">No methods defined.</p>}
          {methods.map((method, idx) => (
            <div key={idx} className="border rounded-lg p-3 bg-muted/10 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className={cn("text-[10px] font-bold px-2 py-0.5 rounded", method.methodType === 'UNARY' ? 'bg-blue-500/10 text-blue-500' : method.methodType === 'SERVER_STREAMING' ? 'bg-green-500/10 text-green-500' : method.methodType === 'CLIENT_STREAMING' ? 'bg-yellow-500/10 text-yellow-500' : 'bg-purple-500/10 text-purple-500')}>
                    {method.methodType || 'UNARY'}
                  </span>
                  <span className="text-sm font-mono">{method.methodName || 'Unnamed'}</span>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => setExpandedMethod(expandedMethod === idx ? null : idx)} className="p-1 text-muted-foreground hover:text-foreground">
                    {expandedMethod === idx ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                  </button>
                  <button onClick={() => removeMethod(idx)} className="p-1 text-muted-foreground hover:text-destructive">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              {expandedMethod === idx && (
                <div className="space-y-2 pt-2 border-t">
                  <div className="grid grid-cols-2 gap-2">
                    <input placeholder="Method name (e.g., GetUser)" value={method.methodName} onChange={(e) => updateMethod(idx, 'methodName', e.target.value)}
                      className="bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary" />
                    <select value={method.methodType} onChange={(e) => updateMethod(idx, 'methodType', e.target.value)}
                      className="bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary">
                      <option value="UNARY">Unary</option>
                      <option value="SERVER_STREAMING">Server Streaming</option>
                      <option value="CLIENT_STREAMING">Client Streaming</option>
                      <option value="BIDI_STREAMING">Bidirectional Streaming</option>
                    </select>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground">Response Template</span>
                    <div className="h-20 border rounded-md overflow-hidden mt-1">
                      <CodeEditor value={method.responseTemplate || ''} onChange={(v) => updateMethod(idx, 'responseTemplate', v || '')} language="json" />
                    </div>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    <div>
                      <span className="text-[10px] text-muted-foreground">Delay (ms)</span>
                      <input type="number" value={method.delayMs || 0} onChange={(e) => updateMethod(idx, 'delayMs', parseInt(e.target.value) || 0)}
                        className="w-full bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary mt-1" />
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Status Code</span>
                      <input value={method.statusCode || '0'} onChange={(e) => updateMethod(idx, 'statusCode', e.target.value)}
                        className="w-full bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary mt-1" />
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Script Language</span>
                      <select value={method.scriptLanguage || 'js'} onChange={(e) => updateMethod(idx, 'scriptLanguage', e.target.value)}
                        className="w-full bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary mt-1">
                        <option value="js">JavaScript</option>
                        <option value="python">Python</option>
                      </select>
                    </div>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground">Error Message</span>
                    <input value={method.errorMessage || ''} onChange={(e) => updateMethod(idx, 'errorMessage', e.target.value)} placeholder="Leave empty for success"
                      className="w-full bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary mt-1" />
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ===================== ISO8583 Editor =====================

function Iso8583Editor({ endpoint, onSave }: { endpoint: any; onSave: (data: any) => Promise<any> }) {
  const [port, setPort] = useState(endpoint.port || 8583);
  const [encoding, setEncoding] = useState(endpoint.encoding || 'ASCII');
  const [headerLengthType, setHeaderLengthType] = useState(endpoint.headerLengthType || '2BYTE');
  const [mocks, setMocks] = useState<any[]>(endpoint.mocks || []);
  const [interceptorScript, setInterceptorScript] = useState(endpoint.interceptorScript || '');
  const [interceptorEnabled, setInterceptorEnabled] = useState(endpoint.interceptorEnabled || false);
  const [customXmlEnabled, setCustomXmlEnabled] = useState(endpoint.customXmlEnabled || false);
  const [customServerXml, setCustomServerXml] = useState(endpoint.customServerXml || '');
  const [isSuccess, setIsSuccess] = useState(false);
  const [expandedMock, setExpandedMock] = useState<number | null>(null);
  const [packagerName, setPackagerName] = useState<string | null>(endpoint.packagerName || null);
  const [packagerUploading, setPackagerUploading] = useState(false);
  const [packagerError, setPackagerError] = useState<string | null>(null);
  const { scenarios } = useScenarios();
  const packagerInputRef = useRef<HTMLInputElement>(null);

  const handlePackagerUpload = async (file: File) => {
    setPackagerUploading(true);
    setPackagerError(null);
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await fetch(`/api/iso8583/endpoints/${endpoint.id}/packager`, {
        method: 'POST',
        body: formData,
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || `Upload failed (${res.status})`);
      }
      const data = await res.json();
      setPackagerName(data.packagerName || file.name);
    } catch (e: any) {
      setPackagerError(e.message || 'Upload failed');
    } finally {
      setPackagerUploading(false);
    }
  };

  const handlePackagerReset = async () => {
    setPackagerUploading(true);
    setPackagerError(null);
    try {
      const res = await fetch(`/api/iso8583/endpoints/${endpoint.id}/packager`, { method: 'DELETE' });
      if (!res.ok) throw new Error(`Reset failed (${res.status})`);
      setPackagerName(null);
    } catch (e: any) {
      setPackagerError(e.message || 'Reset failed');
    } finally {
      setPackagerUploading(false);
    }
  };

  const handleSave = async () => {
    try {
      const saved = await onSave({ id: endpoint.id, port, encoding, headerLengthType, mocks, interceptorScript, interceptorEnabled, customXmlEnabled, customServerXml });
      if (saved?.port !== undefined) setPort(saved.port);
      if (saved?.encoding !== undefined) setEncoding(saved.encoding);
      if (saved?.mocks !== undefined) setMocks(saved.mocks);
      if (saved?.interceptorScript !== undefined) setInterceptorScript(saved.interceptorScript);
      setIsSuccess(true);
      setTimeout(() => setIsSuccess(false), 2000);
    } catch (e) {
      console.error('Failed to save ISO8583 endpoint:', e);
    }
  };

  const addMock = () => {
    setMocks([...mocks, { name: '', mti: '0200', matchers: {}, responseFields: {}, responseCode: '00', priority: 0, enabled: true, delayMs: 0, scenarioName: '' }]);
    setExpandedMock(mocks.length);
  };

  const updateMock = (index: number, field: string, value: any) => {
    setMocks(mocks.map((m, i) => i === index ? { ...m, [field]: value } : m));
  };

  const removeMock = (index: number) => {
    setMocks(mocks.filter((_, i) => i !== index));
  };

  type EditorTab = 'config' | 'simulator';
  const [activeTab, setActiveTab] = useState<EditorTab>('config');

  return (
    <div className="h-full flex flex-col overflow-hidden">
      {/* Tab bar */}
      <div className="flex items-center justify-between px-2 border-b bg-muted/20">
        <div className="flex items-center gap-0">
          <button
            onClick={() => setActiveTab('config')}
            className={cn(
              'px-4 py-2 text-xs font-semibold border-b-2 transition-colors',
              activeTab === 'config'
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
          >
            Configuration
          </button>
          <button
            onClick={() => setActiveTab('simulator')}
            className={cn(
              'flex items-center gap-1.5 px-4 py-2 text-xs font-semibold border-b-2 transition-colors',
              activeTab === 'simulator'
                ? 'border-violet-500 text-violet-500'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
          >
            <Terminal className="h-3 w-3" />
            Simulator
          </button>
        </div>
        {activeTab === 'config' && (
          <button onClick={handleSave} className={cn('flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all', isSuccess ? 'bg-green-500 text-white' : 'bg-accent hover:bg-accent/80 text-foreground')}>
            {isSuccess ? <><Check className="h-4 w-4" /> Saved</> : <><Save className="h-4 w-4" /> Save</>}
          </button>
        )}
      </div>

      {activeTab === 'simulator' && (
        <Iso8583SimulatorPanel endpointId={endpoint.id} isActive={!!endpoint.active} />
      )}

      {activeTab === 'config' && (
      <div className="flex-1 overflow-auto p-4 space-y-6">
        <div className="grid grid-cols-3 gap-4">
          <div className="space-y-2">
            <label className="text-xs font-bold text-muted-foreground uppercase">Port</label>
            <input type="number" value={port} onChange={(e) => setPort(parseInt(e.target.value) || 8583)}
              className="w-full bg-background border rounded px-2 py-1 text-sm outline-none focus:ring-1 focus:ring-primary" />
          </div>
          <div className="space-y-2">
            <label className="text-xs font-bold text-muted-foreground uppercase">Encoding</label>
            <select value={encoding} onChange={(e) => setEncoding(e.target.value)}
              className="w-full bg-background border rounded px-2 py-1 text-sm outline-none focus:ring-1 focus:ring-primary">
              <option value="ASCII">ASCII</option>
              <option value="EBCDIC">EBCDIC</option>
              <option value="BINARY">Binary</option>
            </select>
          </div>
          <div className="space-y-2">
            <label className="text-xs font-bold text-muted-foreground uppercase">Header Length</label>
            <select value={headerLengthType} onChange={(e) => setHeaderLengthType(e.target.value)}
              className="w-full bg-background border rounded px-2 py-1 text-sm outline-none focus:ring-1 focus:ring-primary">
              <option value="NONE">None</option>
              <option value="2BYTE">2 Bytes</option>
              <option value="4BYTE">4 Bytes</option>
            </select>
          </div>
        </div>

        {/* Packager Upload Section */}
        <div className="border rounded-lg p-3 bg-muted/10 space-y-2">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-muted-foreground uppercase">Packager Definition</label>
            <div className="flex items-center gap-2">
              {packagerName ? (
                <>
                  <span className="text-xs text-green-400 font-mono">{packagerName}</span>
                  <button
                    onClick={handlePackagerReset}
                    disabled={packagerUploading}
                    className="text-[10px] text-destructive hover:underline disabled:opacity-50"
                  >
                    Reset to Default
                  </button>
                </>
              ) : (
                <span className="text-[10px] text-muted-foreground italic">Using bundled ISO 8583:1987 default</span>
              )}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input
              ref={packagerInputRef}
              type="file"
              accept=".xml,text/xml,application/xml"
              className="hidden"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) handlePackagerUpload(f);
                e.target.value = '';
              }}
            />
            <button
              onClick={() => packagerInputRef.current?.click()}
              disabled={packagerUploading}
              className="text-xs px-2 py-1 border rounded hover:bg-accent/60 disabled:opacity-50 transition-colors"
            >
              {packagerUploading ? 'Uploading…' : 'Upload packager.xml'}
            </button>
            <span className="text-[10px] text-muted-foreground">jPOS GenericPackager XML format</span>
          </div>
          {packagerError && (
            <p className="text-[10px] text-destructive">{packagerError}</p>
          )}
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-muted-foreground uppercase">Mock Scenarios</label>
            <button onClick={addMock} className="flex items-center gap-1 text-xs text-primary hover:underline">
              <Plus className="h-3 w-3" /> Add Mock
            </button>
          </div>
          {mocks.length === 0 && <p className="text-xs text-muted-foreground italic">No mocks defined.</p>}
          {mocks.map((mock, idx) => (
            <div key={idx} className="border rounded-lg p-3 bg-muted/10 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-bold bg-orange-500/10 text-orange-500 px-2 py-0.5 rounded">{mock.mti || '0200'}</span>
                  <span className="text-sm font-medium">{mock.name || 'Unnamed'}</span>
                  <span className={cn("text-[10px] px-1.5 py-0.5 rounded", mock.enabled ? 'bg-green-500/10 text-green-500' : 'bg-muted text-muted-foreground')}>
                    {mock.enabled ? 'Active' : 'Disabled'}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => setExpandedMock(expandedMock === idx ? null : idx)} className="p-1 text-muted-foreground hover:text-foreground">
                    {expandedMock === idx ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                  </button>
                  <button onClick={() => removeMock(idx)} className="p-1 text-muted-foreground hover:text-destructive">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              {expandedMock === idx && (
                <div className="space-y-2 pt-2 border-t">
                  <div className="grid grid-cols-2 gap-2">
                    <input placeholder="Mock name" value={mock.name} onChange={(e) => updateMock(idx, 'name', e.target.value)}
                      className="bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary" />
                    <div className="flex items-center gap-2">
                      <select value={mock.mti} onChange={(e) => updateMock(idx, 'mti', e.target.value)}
                        className="flex-1 bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary">
                        {Object.entries(mtiDescriptions).map(([code, desc]) => (
                          <option key={code} value={code}>{code} — {desc}</option>
                        ))}
                      </select>
                      <input placeholder="Resp MTI" value={mock.responseMti || ''} onChange={(e) => updateMock(idx, 'responseMti', e.target.value)}
                        className="w-20 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary" title="Response MTI (leave empty to auto-compute)" />
                    </div>
                  </div>
                  
                  {/* Scenario Binding */}
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] font-bold text-muted-foreground uppercase">Link to Scenario</span>
                      <select 
                        value={mock.scenarioName || ''} 
                        onChange={(e) => updateMock(idx, 'scenarioName', e.target.value)}
                        className="flex-1 bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary"
                      >
                        <option value="">None (Standalone Mock)</option>
                        {scenarios.map(s => (
                          <option key={s.id} value={s.name}>{s.name}</option>
                        ))}
                      </select>
                    </div>
                    {mock.scenarioName && (
                      <p className="text-[10px] text-blue-500 italic">
                        Scenario linked. The scenario's postScript and JSON responseTemplate will override this mock's default fields.
                      </p>
                    )}
                  </div>

                  <div className="grid grid-cols-3 gap-2">
                    <div>
                      <span className="text-[10px] text-muted-foreground">Response Code (f39)</span>
                      <input value={mock.responseCode || '00'} onChange={(e) => updateMock(idx, 'responseCode', e.target.value)}
                        className="w-full bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary mt-1" />
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Priority</span>
                      <input type="number" value={mock.priority || 0} onChange={(e) => updateMock(idx, 'priority', parseInt(e.target.value) || 0)}
                        className="w-full bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary mt-1" />
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Delay (ms)</span>
                      <input type="number" value={mock.delayMs || 0} onChange={(e) => updateMock(idx, 'delayMs', parseInt(e.target.value) || 0)}
                        className="w-full bg-background border rounded px-2 py-1 text-xs outline-none focus:ring-1 focus:ring-primary mt-1" />
                    </div>
                  </div>

                  {/* Response Fields */}
                  <div>
                    <span className="text-[10px] text-muted-foreground">Response Fields — type a field number or name to search</span>
                    <ResponseFieldsEditor
                      fields={mock.responseFields || {}}
                      onChange={(v) => updateMock(idx, 'responseFields', v)}
                      showDictionary
                    />
                  </div>

                  {/* Script */}
                  <div>
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-muted-foreground">Response Script</span>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] text-muted-foreground">Enabled</span>
                        <input type="checkbox" checked={mock.scriptEnabled || false} onChange={(e) => updateMock(idx, 'scriptEnabled', e.target.checked)}
                          className="rounded" />
                      </div>
                    </div>
                    {mock.scriptEnabled && (
                      <div className="h-24 border rounded-md overflow-hidden mt-1">
                        <CodeEditor value={mock.script || ''} onChange={(v) => updateMock(idx, 'script', v || '')} language="javascript" />
                      </div>
                    )}
                  </div>

                  {/* Matchers */}
                  <div>
                    <span className="text-[10px] text-muted-foreground">Field Matchers — field number → regex pattern (e.g. 2 → ^4111.*)</span>
                    <ResponseFieldsEditor
                      fields={mock.matchers || {}}
                      onChange={(v) => updateMock(idx, 'matchers', v)}
                      showDictionary
                      valuePlaceholder="regex or exact value"
                    />
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Interceptor */}
        <div className="border rounded-lg p-3 bg-muted/10 space-y-2">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-muted-foreground uppercase">Interceptor Script</label>
            <div className="flex items-center gap-2">
              <span className="text-[10px] text-muted-foreground">Enabled</span>
              <input type="checkbox" checked={interceptorEnabled} onChange={(e) => setInterceptorEnabled(e.target.checked)} className="rounded" />
            </div>
          </div>
          {interceptorEnabled && (
            <div className="h-32 border rounded-md overflow-hidden">
              <CodeEditor value={interceptorScript} onChange={(v) => setInterceptorScript(v || '')} language="javascript" />
            </div>
          )}
        </div>

        {/* Advanced: Custom XML */}
        <details className="border rounded-lg p-3 bg-muted/10">
          <summary className="text-xs font-bold text-muted-foreground uppercase cursor-pointer select-none">Advanced — Custom jPOS XML</summary>
          <div className="mt-2 space-y-2">
            <div className="flex items-center gap-2">
              <input type="checkbox" checked={customXmlEnabled} onChange={(e) => setCustomXmlEnabled(e.target.checked)} className="rounded" />
              <span className="text-xs text-muted-foreground">Enable custom XML editor (overrides form fields)</span>
            </div>
            {customXmlEnabled && (
              <div className="h-48 border rounded-md overflow-hidden">
                <CodeEditor value={customServerXml} onChange={(v) => setCustomServerXml(v || '')} language="xml" />
              </div>
            )}
          </div>
        </details>
      </div>
      )}
    </div>
  );
}

// ===================== ISO8583 Message Simulator Panel =====================

function Iso8583SimulatorPanel({ endpointId, isActive }: { endpointId: string; isActive: boolean }) {
  const { simulate, isSimulating, result, reset } = useIso8583Simulator(endpointId);
  const [selectedMti, setSelectedMti] = useState('0100');
  const [fieldRows, setFieldRows] = useState<Array<{ fieldNum: string; value: string }>>([]);
  const [showRequestHex, setShowRequestHex] = useState(false);
  const [showResponseHex, setShowResponseHex] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const handleAddField = () => setFieldRows(prev => [...prev, { fieldNum: '', value: '' }]);

  const handleRemoveField = (idx: number) => setFieldRows(prev => prev.filter((_, i) => i !== idx));

  const handleSend = async () => {
    const fieldMap: Record<string, string> = {};
    fieldRows.forEach(({ fieldNum, value }) => {
      if (fieldNum.trim() && value.trim()) fieldMap[fieldNum.trim()] = value.trim();
    });
    try { await simulate({ mti: selectedMti, fields: fieldMap }); } catch (_) { /* surfaced via result */ }
  };

  const copyToClipboard = async (text: string, key: string) => {
    await navigator.clipboard.writeText(text);
    setCopiedField(key);
    setTimeout(() => setCopiedField(null), 1500);
  };

  const loadTemplate = () => {
    const templates: Record<string, Array<{ fieldNum: string; value: string }>> = {
      '0100': [
        { fieldNum: '2', value: '4111111111111111' },
        { fieldNum: '3', value: '000000' },
        { fieldNum: '4', value: '000000010000' },
        { fieldNum: '11', value: '123456' },
        { fieldNum: '41', value: 'TERM0001' },
        { fieldNum: '42', value: 'MERCHANT00001  ' },
        { fieldNum: '49', value: '840' },
      ],
      '0200': [
        { fieldNum: '2', value: '4111111111111111' },
        { fieldNum: '3', value: '000000' },
        { fieldNum: '4', value: '000000010000' },
        { fieldNum: '11', value: '123456' },
        { fieldNum: '41', value: 'TERM0001' },
      ],
      '0420': [
        { fieldNum: '2', value: '4111111111111111' },
        { fieldNum: '3', value: '000000' },
        { fieldNum: '4', value: '000000010000' },
        { fieldNum: '11', value: '123456' },
        { fieldNum: '37', value: 'RRN0001234567' },
      ],
    };
    setFieldRows(templates[selectedMti] || [{ fieldNum: '2', value: '' }, { fieldNum: '3', value: '' }]);
    reset();
  };

  return (
    <div className="flex-1 overflow-auto flex flex-col min-h-0">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-2 border-b bg-muted/10 shrink-0">
        <div className="flex items-center gap-2">
          <Terminal className="h-4 w-4 text-violet-500" />
          <span className="text-xs font-bold uppercase tracking-wider">ISO8583 Message Simulator</span>
        </div>
        {!isActive && (
          <span className="text-[10px] px-2 py-0.5 rounded bg-yellow-500/10 text-yellow-500 border border-yellow-500/20">
            ⚠ Endpoint not active — activate it first
          </span>
        )}
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Left: Builder */}
        <div className="flex-1 overflow-auto p-4 space-y-4 border-r min-w-0">
          {/* MTI */}
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">Message Type (MTI)</label>
            <select
              value={selectedMti}
              onChange={e => { setSelectedMti(e.target.value); reset(); }}
              className="w-full bg-background border rounded px-2 py-1.5 text-sm outline-none focus:ring-1 focus:ring-violet-500"
            >
              {Object.entries(mtiDescriptions).map(([code, desc]) => (
                <option key={code} value={code}>{code} — {desc}</option>
              ))}
            </select>
          </div>

          {/* Fields */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">Request Fields</label>
              <div className="flex gap-3">
                <button onClick={loadTemplate} className="text-[10px] text-violet-400 hover:underline">Load Template</button>
                <button onClick={handleAddField} className="flex items-center gap-0.5 text-[10px] text-primary hover:underline">
                  <Plus className="h-2.5 w-2.5" /> Add Field
                </button>
              </div>
            </div>
            {fieldRows.length === 0 && (
              <div className="text-center py-8 text-muted-foreground text-xs border border-dashed rounded-lg">
                <p>No fields added.</p>
                <button onClick={loadTemplate} className="text-violet-400 hover:underline mt-1 text-[10px]">Load template for {selectedMti}</button>
              </div>
            )}
            <div className="space-y-1.5">
              {fieldRows.map((row, idx) => {
                const fd = (fields as Record<string, FieldDef>)[row.fieldNum];
                return (
                  <div key={idx} className="flex items-start gap-1.5 group">
                    <input
                      placeholder="F#"
                      value={row.fieldNum}
                      onChange={e => setFieldRows(prev => prev.map((r, i) => i === idx ? { ...r, fieldNum: e.target.value } : r))}
                      className="w-12 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-violet-500 mt-4"
                    />
                    <div className="flex-1 min-w-0">
                      {fd && <div className="text-[9px] text-muted-foreground truncate mb-0.5">{fd.name} ({fd.format})</div>}
                      <input
                        placeholder={fd ? fd.abbr : 'Value'}
                        value={row.value}
                        onChange={e => setFieldRows(prev => prev.map((r, i) => i === idx ? { ...r, value: e.target.value } : r))}
                        className="w-full bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-violet-500"
                      />
                    </div>
                    <button onClick={() => handleRemoveField(idx)} className="mt-4 p-1 opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-destructive transition-all">
                      <Trash2 className="h-3 w-3" />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Send Button */}
          <button
            onClick={handleSend}
            disabled={isSimulating || !isActive}
            className={cn(
              'w-full flex items-center justify-center gap-2 py-2 rounded-md text-sm font-semibold transition-all',
              isSimulating || !isActive
                ? 'bg-muted text-muted-foreground cursor-not-allowed'
                : 'bg-violet-600 hover:bg-violet-500 text-white shadow shadow-violet-900/40'
            )}
          >
            {isSimulating
              ? <><Loader2 className="h-4 w-4 animate-spin" /> Sending…</>
              : <><SendHorizontal className="h-4 w-4" /> Send {selectedMti}</>
            }
          </button>

          {/* Request hex */}
          {result?.requestHex && (
            <div>
              <button onClick={() => setShowRequestHex(v => !v)} className="text-[10px] text-muted-foreground hover:text-foreground">
                {showRequestHex ? '▾' : '▸'} Request Hex
              </button>
              {showRequestHex && (
                <pre className="mt-1 text-[9px] font-mono bg-muted/30 rounded p-2 overflow-x-auto whitespace-pre-wrap break-all text-muted-foreground">
                  {result.requestHex}
                </pre>
              )}
            </div>
          )}
        </div>

        {/* Right: Response */}
        <div className="flex-1 overflow-auto p-4 space-y-3 min-w-0">
          <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider block">Response</label>

          {!result && !isSimulating && (
            <div className="flex flex-col items-center justify-center h-48 text-muted-foreground">
              <SendHorizontal className="h-10 w-10 opacity-10 mb-3" />
              <p className="text-xs">Send a message to see the response</p>
            </div>
          )}

          {isSimulating && (
            <div className="flex flex-col items-center justify-center h-48 text-muted-foreground">
              <Loader2 className="h-8 w-8 animate-spin opacity-30 mb-3" />
              <p className="text-xs">Waiting for response…</p>
            </div>
          )}

          {result && !isSimulating && (
            <div className="space-y-3">
              {/* Status banner */}
              <div className={cn(
                'flex items-center justify-between px-3 py-2 rounded-md text-xs font-medium',
                result.success
                  ? 'bg-green-500/10 text-green-500 border border-green-500/20'
                  : 'bg-destructive/10 text-destructive border border-destructive/20'
              )}>
                <span>{result.success ? `✓ ${result.responseMti} received` : `✗ ${result.errorType || 'Error'}`}</span>
                {result.latencyMs != null && result.latencyMs > 0 && (
                  <span className="text-muted-foreground font-normal">{result.latencyMs}ms</span>
                )}
              </div>

              {/* Error message */}
              {!result.success && result.errorMessage && (
                <div className="text-xs text-destructive bg-destructive/5 rounded p-3 border border-destructive/10">
                  {result.errorMessage}
                </div>
              )}

              {/* Decoded fields */}
              {result.success && result.responseFields && Object.keys(result.responseFields).length > 0 && (
                <div>
                  <div className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider mb-1">Decoded Fields</div>
                  <div className="border rounded-md overflow-hidden">
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="bg-muted/20">
                          <th className="text-left px-3 py-1.5 text-[10px] text-muted-foreground font-medium w-10">F#</th>
                          <th className="text-left px-3 py-1.5 text-[10px] text-muted-foreground font-medium">Name</th>
                          <th className="text-left px-3 py-1.5 text-[10px] text-muted-foreground font-medium">Value</th>
                          <th className="w-8" />
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border/50">
                        {Object.entries(result.responseFields)
                          .sort((a, b) => parseInt(a[0]) - parseInt(b[0]))
                          .map(([fn, val]) => {
                            const fd = (fields as Record<string, FieldDef>)[fn];
                            const isRc = fn === '39';
                            const rcDesc = isRc ? (commonResponseCodes as Record<string, string>)[val] : null;
                            const ck = `f-${fn}`;
                            return (
                              <tr key={fn} className={cn('hover:bg-muted/10 group', isRc && val === '00' ? 'bg-green-500/5' : '')}>
                                <td className="px-3 py-1.5 font-mono text-muted-foreground text-[10px]">{fn}</td>
                                <td className="px-3 py-1.5 text-muted-foreground text-[10px]">
                                  {fd ? <span title={fd.description}>{fd.name}</span> : <span className="italic">Field {fn}</span>}
                                  {rcDesc && <span className="ml-1 text-[9px]">({rcDesc})</span>}
                                </td>
                                <td className="px-3 py-1.5 font-mono text-[10px]">
                                  <span className={cn(isRc && val === '00' ? 'text-green-500 font-semibold' : '')}>{val}</span>
                                </td>
                                <td className="px-2 py-1.5">
                                  <button onClick={() => copyToClipboard(val, ck)} className="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-foreground transition-opacity">
                                    {copiedField === ck ? <Check className="h-3 w-3 text-green-500" /> : <Copy className="h-3 w-3" />}
                                  </button>
                                </td>
                              </tr>
                            );
                          })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Response hex */}
              {result.responseHex && (
                <div>
                  <button onClick={() => setShowResponseHex(v => !v)} className="text-[10px] text-muted-foreground hover:text-foreground">
                    {showResponseHex ? '▾' : '▸'} Response Hex
                  </button>
                  {showResponseHex && (
                    <pre className="mt-1 text-[9px] font-mono bg-muted/30 rounded p-2 overflow-x-auto whitespace-pre-wrap break-all text-muted-foreground">
                      {result.responseHex}
                    </pre>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ===================== Field Autocomplete Input =====================

function FieldNumberInput({

  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  const [query, setQuery] = useState(value);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => { setQuery(value); }, [value]);

  // Close on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const matches = query.trim()
    ? Object.entries(fields).filter(([num, def]) =>
        num.startsWith(query.trim()) ||
        def.name.toLowerCase().includes(query.toLowerCase()) ||
        def.abbr.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 8)
    : [];

  const handleSelect = (num: string) => {
    setQuery(num);
    onChange(num);
    setOpen(false);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
    onChange(e.target.value);
    setOpen(true);
  };

  const label = fields[query];

  return (
    <div ref={ref} className="relative w-48 flex-shrink-0">
      <div className="relative">
        <input
          value={query}
          onChange={handleChange}
          onFocus={() => setOpen(true)}
          placeholder="Field # or name"
          className="w-full bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary pr-6"
        />
        <Search className="absolute right-1.5 top-1 h-3 w-3 text-muted-foreground" />
      </div>
      {/* Field label tooltip */}
      {label && (
        <div className="text-[10px] text-blue-400 truncate mt-0.5 px-0.5" title={label.description}>
          {label.abbr} — {label.name}
        </div>
      )}
      {/* Dropdown */}
      {open && matches.length > 0 && (
        <div className="absolute z-50 top-full left-0 mt-0.5 w-72 bg-popover border rounded shadow-lg max-h-56 overflow-auto">
          {matches.map(([num, def]) => (
            <button
              key={num}
              onMouseDown={(e) => { e.preventDefault(); handleSelect(num); }}
              className="w-full text-left px-2 py-1.5 text-xs hover:bg-accent/60 flex flex-col gap-0"
              title={`Format: ${def.format}\n${def.description}`}
            >
              <span className="font-mono font-bold">{num} — <span className="text-orange-400">{def.abbr}</span></span>
              <span className="text-muted-foreground truncate">{def.name}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ===================== Response Fields Editor =====================

function ResponseFieldsEditor({
  fields: inputFields,
  onChange,
  showDictionary = false,
  valuePlaceholder = '{{field.value}} or static text',
}: {
  fields: Record<string, string>;
  onChange: (fields: Record<string, string>) => void;
  showDictionary?: boolean;
  valuePlaceholder?: string;
}) {
  const [rows, setRows] = useState<Array<{ key: string; value: string }>>(() =>
    Object.entries(inputFields || {}).map(([k, v]) => ({ key: k, value: v }))
  );

  useEffect(() => {
    setRows(Object.entries(inputFields || {}).map(([k, v]) => ({ key: k, value: v })));
  }, [inputFields]);

  const emitChange = (newRows: Array<{ key: string; value: string }>) => {
    const result: Record<string, string> = {};
    newRows.forEach(({ key, value }) => { if (key.trim()) result[key.trim()] = value; });
    onChange(result);
  };

  const addRow = () => {
    const newRows = [...rows, { key: '', value: '' }];
    setRows(newRows);
    emitChange(newRows);
  };

  const updateKey = (index: number, val: string) => {
    const newRows = rows.map((r, i) => i === index ? { ...r, key: val } : r);
    setRows(newRows);
    emitChange(newRows);
  };

  const updateValue = (index: number, val: string) => {
    const newRows = rows.map((r, i) => i === index ? { ...r, value: val } : r);
    setRows(newRows);
    emitChange(newRows);
  };

  const deleteRow = (index: number) => {
    const newRows = rows.filter((_, i) => i !== index);
    setRows(newRows);
    emitChange(newRows);
  };

  return (
    <div className="space-y-1.5 mt-1">
      {rows.map((row, idx) => (
        <div key={idx} className="flex items-start gap-2 group">
          {showDictionary ? (
            <FieldNumberInput value={row.key} onChange={(v) => updateKey(idx, v)} />
          ) : (
            <input
              placeholder="Field #"
              value={row.key}
              onChange={(e) => updateKey(idx, e.target.value)}
              className="w-20 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary"
            />
          )}
          <input
            placeholder={valuePlaceholder}
            value={row.value}
            onChange={(e) => updateValue(idx, e.target.value)}
            className="flex-1 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary mt-0"
          />
          <button
            onClick={() => deleteRow(idx)}
            className="p-1 text-muted-foreground opacity-0 group-hover:opacity-100 hover:text-destructive transition-all mt-0.5"
          >
            <Trash2 className="h-3 w-3" />
          </button>
        </div>
      ))}
      <button onClick={addRow} className="text-[10px] text-primary hover:underline mt-1">
        <Plus className="h-3 w-3 inline mr-0.5" /> Add field
      </button>
    </div>
  );
}
