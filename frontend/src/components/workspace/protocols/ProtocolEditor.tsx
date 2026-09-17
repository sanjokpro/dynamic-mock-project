'use client';

import React, { useState, useEffect } from 'react';
import { Protocol } from '@/types';
import { useProtocolEndpoints } from '@/hooks/useProtocolEndpoints';
import { CodeEditor } from '../CodeEditor';
import { Loader2, Save, Check, Plus, Trash2, ChevronDown, ChevronUp } from 'lucide-react';
import { cn } from '@/lib/utils';

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
    setMocks([...mocks, { name: '', mti: '0200', matchers: {}, responseFields: {}, responseCode: '00', priority: 0, enabled: true, delayMs: 0 }]);
    setExpandedMock(mocks.length);
  };

  const updateMock = (index: number, field: string, value: any) => {
    setMocks(mocks.map((m, i) => i === index ? { ...m, [field]: value } : m));
  };

  const removeMock = (index: number) => {
    setMocks(mocks.filter((_, i) => i !== index));
  };

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <div className="flex items-center justify-between p-2 border-b bg-muted/20">
        <h2 className="text-sm font-bold uppercase tracking-wider">ISO8583 Configuration</h2>
        <button onClick={handleSave} className={cn("flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all", isSuccess ? "bg-green-500 text-white" : "bg-accent hover:bg-accent/80 text-foreground")}>
          {isSuccess ? <><Check className="h-4 w-4" /> Saved</> : <><Save className="h-4 w-4" /> Save</>}
        </button>
      </div>
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
                        <option value="0100">0100 - Authorization Request</option>
                        <option value="0110">0110 - Authorization Response</option>
                        <option value="0200">0200 - Financial Request</option>
                        <option value="0210">0210 - Financial Response</option>
                        <option value="0220">0220 - Acquirer Financial Advice</option>
                        <option value="0400">0400 - Reversal Request</option>
                        <option value="0420">0420 - Reversal Advice</option>
                        <option value="0800">0800 - Network Management</option>
                        <option value="0810">0810 - Network Management Response</option>
                      </select>
                      <input placeholder="Resp MTI" value={mock.responseMti || ''} onChange={(e) => updateMock(idx, 'responseMti', e.target.value)}
                        className="w-20 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary" title="Response MTI (leave empty to auto-compute)" />
                    </div>
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
                    <span className="text-[10px] text-muted-foreground">Response Fields (field number → template)</span>
                    <ResponseFieldsEditor
                      fields={mock.responseFields || {}}
                      onChange={(v) => updateMock(idx, 'responseFields', v)}
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
                    <span className="text-[10px] text-muted-foreground">Field Matchers (field.2 → ^4111.*)</span>
                    <ResponseFieldsEditor
                      fields={mock.matchers || {}}
                      onChange={(v) => updateMock(idx, 'matchers', v)}
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
    </div>
  );
}

function ResponseFieldsEditor({ fields, onChange }: { fields: Record<string, string>; onChange: (fields: Record<string, string>) => void }) {
  const [rows, setRows] = useState<Array<{ key: string; value: string }>>(() =>
    Object.entries(fields || {}).map(([k, v]) => ({ key: k, value: v }))
  );

  useEffect(() => {
    setRows(Object.entries(fields || {}).map(([k, v]) => ({ key: k, value: v })));
  }, [fields]);

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

  const updateRow = (index: number, field: 'key' | 'value', val: string) => {
    const newRows = rows.map((r, i) => i === index ? { ...r, [field]: val } : r);
    setRows(newRows);
    emitChange(newRows);
  };

  const deleteRow = (index: number) => {
    const newRows = rows.filter((_, i) => i !== index);
    setRows(newRows);
    emitChange(newRows);
  };

  return (
    <div className="space-y-1 mt-1">
      {rows.map((row, idx) => (
        <div key={idx} className="flex items-center gap-2 group">
          <input placeholder="Field #" value={row.key} onChange={(e) => updateRow(idx, 'key', e.target.value)}
            className="w-20 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary" />
          <input placeholder="{{field.value}} or static text" value={row.value} onChange={(e) => updateRow(idx, 'value', e.target.value)}
            className="flex-1 bg-background border rounded px-2 py-1 text-xs font-mono outline-none focus:ring-1 focus:ring-primary" />
          <button onClick={() => deleteRow(idx)} className="p-1 text-muted-foreground opacity-0 group-hover:opacity-100 hover:text-destructive transition-all">
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
