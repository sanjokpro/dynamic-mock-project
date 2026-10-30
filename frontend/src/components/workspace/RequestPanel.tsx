'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Play, Save, Loader2, Check, Terminal, PlaySquare, Settings2 } from 'lucide-react';
import { useTestClient } from '@/hooks/useTestClient';
import { useRoutes } from '@/hooks/useRoutes';
import { useWorkspace } from '@/context/WorkspaceContext';
import { MockRoute } from '@/types';
import { ProtocolEditor } from './protocols/ProtocolEditor';
import { ScenarioEditor } from './scenarios/ScenarioEditor';
import { TestClientView } from './TestClientView';
import { MockDefinitionView } from './MockDefinitionView';
import { RichEmptyState } from './RichEmptyState';
import { BankingSandboxModal } from './BankingSandboxModal';
import { ImportModal } from './ImportModal';
import { HeaderRow } from './HeadersEditor';
import { generateCurlCommand, copyToClipboard } from '@/lib/curl';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

export function parseQueryParamsFromUrl(rawUrl: string): Record<string, string> {
  const qIdx = rawUrl.indexOf('?');
  if (qIdx === -1) return {};
  const queryString = rawUrl.slice(qIdx + 1);
  if (!queryString.trim()) return {};
  const params: Record<string, string> = {};
  for (const pair of queryString.split('&')) {
    if (!pair) continue;
    const eqIdx = pair.indexOf('=');
    if (eqIdx === -1) {
      const k = pair.trim();
      if (k) params[k] = '';
    } else {
      const k = pair.slice(0, eqIdx).trim();
      const v = pair.slice(eqIdx + 1);
      if (k) params[k] = v;
    }
  }
  return params;
}

export function buildUrlWithQueryParams(basePathUrl: string, params?: Record<string, string>): string {
  const basePart = basePathUrl.split('?')[0];
  const entries = Object.entries(params || {}).filter(([k]) => k.trim() !== '');
  if (entries.length === 0) return basePart;
  const qs = entries.map(([k, v]) => (v !== '' ? `${k.trim()}=${v}` : k.trim())).join('&');
  return `${basePart}?${qs}`;
}

export function RequestPanel() {
  const [viewMode, setViewMode] = useState<'test' | 'mock'>('test');
  const [sandboxModalOpen, setSandboxModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);

  const { activeRoute, setActiveRoute, setLastResponse, activeEnvironment } = useWorkspace();
  const { updateRoute, createRoute, isLoading: isSaving } = useRoutes();
  const { execute, isLoading: isExecuting } = useTestClient();

  const [method, setMethod] = useState('GET');
  const [path, setPath] = useState('');
  const [url, setUrl] = useState('{{BASE_URL}}/hello');
  const [responseStatus, setResponseStatus] = useState(200);
  const [responseTemplate, setResponseTemplate] = useState('');
  const [preScript, setPreScript] = useState('');
  const [postScript, setPostScript] = useState('');
  const [queryParams, setQueryParams] = useState<Record<string, string>>({});
  const [requestHeaders, setRequestHeaders] = useState<Record<string, string>>({});
  const [headerRows, setHeaderRows] = useState<HeaderRow[]>([]);
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

  const isLoadedRef = useRef<boolean>(false);
  const currentRouteIdRef = useRef<string | null>(null);

  // Sync state from activeRoute or localStorage draft
  useEffect(() => {
    if (!activeRoute) {
      isLoadedRef.current = false;
      currentRouteIdRef.current = null;
      return;
    }

    isLoadedRef.current = false;
    currentRouteIdRef.current = activeRoute.id;

    setMethod(activeRoute.method || 'GET');
    const activePath = activeRoute.path || '';
    const baseUrlForPath = `{{BASE_URL}}${activePath.startsWith('/') ? activePath : '/' + activePath}`;
    setPath(activePath);
    setResponseStatus(activeRoute.responseStatus || 200);
    setResponseTemplate(activeRoute.responseTemplate || '');
    setPreScript(activeRoute.preScript || '');
    setPostScript(activeRoute.postScript || '');
    setResponseHeaders(activeRoute.responseHeaders || {});
    setMatchers(activeRoute.matchers || {});

    // Restore draft from localStorage
    let restoredDraft = false;
    let restoredParams: Record<string, string> = activeRoute.queryParams || {};
    if (typeof window !== 'undefined') {
      try {
        const saved = localStorage.getItem(`dynamicmock:test_draft:${activeRoute.id}`);
        if (saved) {
          const draft = JSON.parse(saved);
          if (draft && typeof draft === 'object') {
            setRequestHeaders(draft.requestHeaders || {});
            const draftParams = draft.queryParams && Object.keys(draft.queryParams).length > 0
              ? draft.queryParams
              : (activeRoute.queryParams || {});
            restoredParams = draftParams;
            setQueryParams(draftParams);
            setRequestBodyType(draft.requestBodyType || 'none');
            setRequestBodyContent(draft.requestBodyContent || '');

            // Backward-compatible headerRows restoration
            if (draft.headerRows && Array.isArray(draft.headerRows)) {
              setHeaderRows(draft.headerRows);
            } else if (draft.requestHeaders) {
              setHeaderRows(
                Object.entries(draft.requestHeaders).map(([key, value], idx) => ({
                  id: `legacy-${idx}-${key}`,
                  key,
                  value: value as string,
                  enabled: true
                }))
              );
            }
            restoredDraft = true;
          }
        }
      } catch (e) {
        console.warn('Failed to load test request draft from localStorage', e);
      }
    }

    if (!restoredDraft) {
      setQueryParams(restoredParams);
      setRequestHeaders({});
      setHeaderRows([]);
      setRequestBodyType('none');
      setRequestBodyContent('');
    }

    setUrl(buildUrlWithQueryParams(baseUrlForPath, restoredParams));

    const timer = setTimeout(() => {
      isLoadedRef.current = true;
    }, 50);

    return () => clearTimeout(timer);
  }, [activeRoute?.id]);

  // Persist draft on changes
  useEffect(() => {
    if (!isLoadedRef.current || !activeRoute?.id) return;
    if (currentRouteIdRef.current !== activeRoute.id) return;

    if (typeof window !== 'undefined') {
      try {
        const effectiveParams = url.includes('?')
          ? { ...queryParams, ...parseQueryParamsFromUrl(url) }
          : queryParams;
        const draft = {
          url,
          requestHeaders,
          queryParams: effectiveParams,
          requestBodyType,
          requestBodyContent,
          headerRows,
          updatedAt: Date.now()
        };
        localStorage.setItem(`dynamicmock:test_draft:${activeRoute.id}`, JSON.stringify(draft));
      } catch (e) {
        console.warn('Failed to persist test request draft to localStorage', e);
      }
    }
  }, [url, requestHeaders, queryParams, requestBodyType, requestBodyContent, headerRows, activeRoute?.id]);

  const handleSave = useCallback(async () => {
    if (!activeRoute) return;
    let effectivePath = path.split('?')[0].trim();
    let effectiveQueryParams: Record<string, string> = { ...queryParams };
    if (url) {
      const clean = url.replace(/\{\{BASE_URL\}\}/g, '').split('?')[0].trim();
      if (clean) effectivePath = clean.startsWith('/') ? clean : '/' + clean;
      if (url.includes('?')) {
        effectiveQueryParams = parseQueryParamsFromUrl(url);
      } else {
        effectiveQueryParams = {};
      }
    }
    setQueryParams(effectiveQueryParams);

    const updatedRoute: Partial<MockRoute> & { id: string } = {
      id: activeRoute.id,
      method: method as any,
      path: effectivePath,
      active: true,
      responseStatus,
      responseTemplate,
      queryParams: effectiveQueryParams,
      responseHeaders: Object.keys(responseHeaders).length > 0 ? responseHeaders : undefined,
      matchers: (matchers.headers && Object.keys(matchers.headers).length > 0) ||
        (matchers.queryParams && Object.keys(matchers.queryParams).length > 0) ||
        (matchers.bodyMatchType && matchers.bodyMatchType !== 'none') ? matchers : {},
      preScript,
      postScript,
      scriptLanguage: 'js'
    };

    try {
      const saved = await updateRoute(updatedRoute);
      const baseUrlForSaved = `{{BASE_URL}}${effectivePath.startsWith('/') ? effectivePath : '/' + effectivePath}`;
      const savedFullUrl = buildUrlWithQueryParams(baseUrlForSaved, effectiveQueryParams);
      if (typeof window !== 'undefined') {
        try {
          const draft = {
            url: savedFullUrl,
            requestHeaders,
            queryParams: effectiveQueryParams,
            requestBodyType,
            requestBodyContent,
            headerRows,
            updatedAt: Date.now()
          };
          localStorage.setItem(`dynamicmock:test_draft:${activeRoute.id}`, JSON.stringify(draft));
        } catch (e) {
          console.warn('Failed to persist test request draft on save', e);
        }
      }
      setActiveRoute(saved);
      setUrl(savedFullUrl);
      setIsSuccess(true);
      toast.success('Route saved successfully');
      setTimeout(() => setIsSuccess(false), 2000);
    } catch (error) {
      console.error('Failed to save route:', error);
      toast.error('Failed to save route');
    }
  }, [activeRoute, method, path, url, responseStatus, responseTemplate, queryParams, requestHeaders, requestBodyType, requestBodyContent, headerRows, responseHeaders, matchers, preScript, postScript, updateRoute, setActiveRoute]);

  // Synchronize Content-Type header with selected Body Type (Postman-style)
  const syncContentTypeForBodyType = useCallback((
    type: 'none' | 'json' | 'form' | 'raw',
    currentHeaders: Record<string, string>,
    currentRows: HeaderRow[]
  ) => {
    const targetContentType =
      type === 'json'
        ? 'application/json'
        : type === 'form'
          ? 'application/x-www-form-urlencoded'
          : type === 'raw'
            ? 'text/plain'
            : null;

    // Filter out any existing Content-Type header (case-insensitive)
    const newHeaders: Record<string, string> = {};
    for (const [k, v] of Object.entries(currentHeaders)) {
      if (k.toLowerCase() !== 'content-type') {
        newHeaders[k] = v;
      }
    }

    let newRows = currentRows.filter(r => r.key.toLowerCase() !== 'content-type');

    if (targetContentType) {
      newHeaders['Content-Type'] = targetContentType;
      newRows = [
        ...newRows,
        {
          id: `ct-${Date.now()}`,
          key: 'Content-Type',
          value: targetContentType,
          enabled: true
        }
      ];
    }

    return { headers: newHeaders, rows: newRows };
  }, []);

  const handleRequestBodyTypeChange = useCallback((type: 'none' | 'json' | 'form' | 'raw') => {
    setRequestBodyType(type);
    const { headers: updatedHeaders, rows: updatedRows } = syncContentTypeForBodyType(type, requestHeaders, headerRows);
    setRequestHeaders(updatedHeaders);
    setHeaderRows(updatedRows);
  }, [requestHeaders, headerRows, syncContentTypeForBodyType]);

  const handleSend = useCallback(() => {
    let targetUrl = url;
    const queryEntries = Object.entries(queryParams).filter(([k]) => k.trim());
    if (queryEntries.length > 0) {
      const [baseUrlPart, existingQuery] = targetUrl.split('?');
      const searchParams = new URLSearchParams(existingQuery || '');
      queryEntries.forEach(([k, v]) => searchParams.set(k, v));
      targetUrl = `${baseUrlPart}?${searchParams.toString()}`;
    }

    // Build headers with case-insensitive Content-Type enforcement based on body type
    const headers: Record<string, string> = {};
    for (const [k, v] of Object.entries(requestHeaders)) {
      if (k.trim() && k.toLowerCase() !== 'content-type') {
        headers[k.trim()] = v;
      }
    }

    if (requestBodyType === 'json') {
      headers['Content-Type'] = 'application/json';
    } else if (requestBodyType === 'form') {
      headers['Content-Type'] = 'application/x-www-form-urlencoded';
    } else if (requestBodyType === 'raw') {
      const existingCt = Object.entries(requestHeaders).find(([k]) => k.toLowerCase() === 'content-type');
      headers['Content-Type'] = existingCt ? existingCt[1] : 'text/plain';
    }

    if (activeEnvironment) {
      headers['X-Dynamic-Mock-Env'] = activeEnvironment.id;
    }

    const body = requestBodyType !== 'none' && requestBodyContent ? requestBodyContent : undefined;

    execute({ url: targetUrl, method, headers, body }, {
      onSuccess: (data) => setLastResponse(data)
    });
  }, [url, queryParams, requestHeaders, activeEnvironment, requestBodyType, requestBodyContent, method, execute, setLastResponse]);

  const handleCopyCurl = useCallback(async () => {
    let targetUrl = url;
    const queryEntries = Object.entries(queryParams).filter(([k]) => k.trim());
    if (queryEntries.length > 0) {
      const [baseUrlPart, existingQuery] = targetUrl.split('?');
      const searchParams = new URLSearchParams(existingQuery || '');
      queryEntries.forEach(([k, v]) => searchParams.set(k, v));
      targetUrl = `${baseUrlPart}?${searchParams.toString()}`;
    }

    // Build headers with case-insensitive Content-Type enforcement
    const headers: Record<string, string> = {};
    for (const [k, v] of Object.entries(requestHeaders)) {
      if (k.trim() && k.toLowerCase() !== 'content-type') {
        headers[k.trim()] = v;
      }
    }

    if (requestBodyType === 'json') {
      headers['Content-Type'] = 'application/json';
    } else if (requestBodyType === 'form') {
      headers['Content-Type'] = 'application/x-www-form-urlencoded';
    } else if (requestBodyType === 'raw') {
      const existingCt = Object.entries(requestHeaders).find(([k]) => k.toLowerCase() === 'content-type');
      headers['Content-Type'] = existingCt ? existingCt[1] : 'text/plain';
    }

    // Note: X-Dynamic-Mock-Env excluded from cURL - it's an internal routing header
    const body = requestBodyType !== 'none' && requestBodyContent ? requestBodyContent : undefined;
    const curl = generateCurlCommand({
      method,
      url: targetUrl,
      headers,
      body,
      variables: activeEnvironment?.variables,
      baseOrigin: typeof window !== 'undefined' ? `${window.location.origin}/mock` : undefined
    });

    const success = await copyToClipboard(curl);
    if (success) {
      toast.success('cURL command copied to clipboard');
    } else {
      toast.error('Failed to copy to clipboard');
    }
  }, [url, queryParams, requestHeaders, activeEnvironment, requestBodyType, requestBodyContent, method]);

  // Keyboard shortcuts (Cmd+Enter / Ctrl+Enter = Send, Cmd+S / Ctrl+S = Save)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault();
        handleSend();
      } else if ((e.metaKey || e.ctrlKey) && e.key === 's') {
        e.preventDefault();
        handleSave();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleSend, handleSave]);

  // Populate Test Request from Matchers (smart generator)
  const handlePopulateTestFromMatchers = useCallback(() => {
    const newHeaders: Record<string, string> = {};
    if (matchers.headers) {
      for (const [k, v] of Object.entries(matchers.headers)) {
        newHeaders[k] = v === '.*' ? 'test' : v;
      }
    }
    if (matchers.queryParams) {
      setQueryParams(matchers.queryParams);
    }

    let detectedBodyType: 'none' | 'json' | 'form' | 'raw' = 'none';
    let sampleBody = '';

    if (matchers.bodyMatchType === 'jsonpath') {
      detectedBodyType = 'json';
      const pattern = matchers.bodyMatchPattern || '';
      if (pattern.includes('==')) {
        const parts = pattern.split('==');
        const field = parts[0].trim().replace(/^\$\.?/, '');
        let val = parts[1].trim().replace(/^["']|["']$/g, '');
        sampleBody = JSON.stringify({ [field || 'field']: val }, null, 2);
      } else {
        const field = pattern.trim().replace(/^\$\.?/, '');
        sampleBody = JSON.stringify({ [field || 'head2']: 'hello' }, null, 2);
      }
    } else if (matchers.bodyMatchType === 'xpath') {
      detectedBodyType = 'raw';
      sampleBody = '<root><id>1</id></root>';
    } else if (matchers.bodyMatchType === 'equals' || matchers.bodyMatchType === 'contains') {
      const pattern = matchers.bodyMatchPattern || '';
      sampleBody = pattern;
      if (pattern.trim().startsWith('{') || pattern.trim().startsWith('[')) {
        detectedBodyType = 'json';
      } else {
        detectedBodyType = 'raw';
      }
    }

    if (detectedBodyType === 'json') {
      newHeaders['Content-Type'] = 'application/json';
    } else if ((detectedBodyType as string) === 'form') {
      newHeaders['Content-Type'] = 'application/x-www-form-urlencoded';
    }

    setRequestBodyType(detectedBodyType);
    setRequestBodyContent(sampleBody);
    setRequestHeaders(newHeaders);
    setHeaderRows(
      Object.entries(newHeaders).map(([k, v], i) => ({
        id: `populated-${Date.now()}-${i}`,
        key: k,
        value: v,
        enabled: true
      }))
    );

    setViewMode('test');
    toast.success('Test request populated from route matchers');
  }, [matchers]);

  if (!activeRoute) {
    return (
      <>
        <RichEmptyState
          onOpenSandboxModal={() => setSandboxModalOpen(true)}
          onCreateRoute={async () => {
            const newRoute = await createRoute({
              name: 'New Mock Endpoint',
              path: '/api/v1/mock',
              method: 'GET',
              responseStatus: 200,
              responseTemplate: '{\n  "status": "ok"\n}'
            });
            setActiveRoute(newRoute);
          }}
          onOpenImportModal={() => setImportModalOpen(true)}
        />
        <BankingSandboxModal
          open={sandboxModalOpen}
          onClose={() => setSandboxModalOpen(false)}
        />
        <ImportModal
          open={importModalOpen}
          onClose={() => setImportModalOpen(false)}
        />
      </>
    );
  }

  if (activeRoute.protocol && activeRoute.protocol !== 'HTTP') {
    return <ProtocolEditor key={activeRoute.id} protocol={activeRoute.protocol} endpointId={activeRoute.id} />;
  }

  if (activeRoute.scenarioName) {
    return <ScenarioEditor />;
  }

  return (
    <div className="flex flex-col h-full bg-background">
      {/* Top Bar: Method, URL, Send, Save, Copy cURL */}
      <div className="flex items-center justify-between p-2 border-b bg-muted/20 shrink-0 gap-2">
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <div className="flex items-center bg-background border border-border rounded-md overflow-hidden flex-1 max-w-xl">
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              className="px-3 py-1.5 text-xs font-bold text-blue-500 bg-transparent border-r border-border hover:bg-accent transition-colors appearance-none cursor-pointer outline-none shrink-0"
            >
              {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            <div className="px-2 text-muted-foreground text-xs font-mono shrink-0">/mock</div>
            <input
              value={url}
              onChange={(e) => {
                const val = e.target.value;
                setUrl(val);
                const clean = val.replace(/\{\{BASE_URL\}\}/g, '').split('?')[0].trim();
                setPath(clean.startsWith('/') ? clean : (clean ? '/' + clean : '/'));
                if (val.includes('?')) {
                  setQueryParams(parseQueryParamsFromUrl(val));
                } else {
                  setQueryParams({});
                }
              }}
              placeholder="{{BASE_URL}}/api/example"
              className="px-3 py-1.5 text-xs bg-transparent border-none focus:outline-none flex-1 font-mono min-w-0"
            />
          </div>

          <button
            type="button"
            onClick={handleSend}
            disabled={isExecuting}
            className="bg-primary text-primary-foreground px-4 py-1.5 rounded-md text-xs font-semibold flex items-center gap-1.5 hover:bg-primary/90 transition-colors disabled:opacity-50 cursor-pointer shadow-xs shrink-0"
            title="Execute test request (⌘+Enter)"
          >
            {isExecuting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Play className="h-3.5 w-3.5 fill-current" />}
            <span>Send</span>
          </button>
        </div>

        <div className="flex items-center gap-1.5 shrink-0">
          <button
            type="button"
            onClick={handleCopyCurl}
            className="flex items-center gap-1 px-2.5 py-1.5 text-xs rounded-md bg-secondary text-secondary-foreground hover:bg-accent transition-colors cursor-pointer"
            title="Copy request as cURL command"
          >
            <Terminal className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">cURL</span>
          </button>

          <button
            type="button"
            onClick={handleSave}
            disabled={isSaving}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all cursor-pointer",
              isSuccess ? "bg-green-500 text-white" : "bg-accent hover:bg-accent/80 text-foreground"
            )}
            title="Save route configuration (⌘+S)"
          >
            {isSaving ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : (isSuccess ? <Check className="h-3.5 w-3.5" /> : <Save className="h-3.5 w-3.5" />)}
            <span>{isSuccess ? 'Saved' : 'Save'}</span>
          </button>
        </div>
      </div>

      {/* Segmented View Mode Control */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b bg-muted/10 shrink-0">
        <div className="inline-flex items-center bg-muted/60 p-0.5 rounded-lg border border-border/80">
          <button
            type="button"
            onClick={() => setViewMode('test')}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1 text-xs rounded-md font-medium transition-all cursor-pointer",
              viewMode === 'test'
                ? "bg-background text-foreground shadow-xs"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <PlaySquare className="h-3.5 w-3.5 text-primary" />
            <span>Test Client Runner</span>
          </button>
          <button
            type="button"
            onClick={() => setViewMode('mock')}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1 text-xs rounded-md font-medium transition-all cursor-pointer",
              viewMode === 'mock'
                ? "bg-background text-foreground shadow-xs"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <Settings2 className="h-3.5 w-3.5 text-primary" />
            <span>Mock Stub Definition</span>
          </button>
        </div>
      </div>

      {/* Segmented Body */}
      <div className="flex-1 overflow-hidden">
        {viewMode === 'test' ? (
          <TestClientView
            queryParams={queryParams}
            onQueryParamsChange={(newParams) => {
              setQueryParams(newParams);
              setUrl((prevUrl) => buildUrlWithQueryParams(prevUrl, newParams));
            }}
            requestHeaders={requestHeaders}
            headerRows={headerRows}
            onRequestHeadersChange={(headers, rows) => {
              setRequestHeaders(headers);
              setHeaderRows(rows);
            }}
            requestBodyType={requestBodyType}
            onRequestBodyTypeChange={handleRequestBodyTypeChange}
            requestBodyContent={requestBodyContent}
            onRequestBodyContentChange={setRequestBodyContent}
            onCopyCurl={handleCopyCurl}
          />
        ) : (
          <MockDefinitionView
            responseStatus={responseStatus}
            onResponseStatusChange={setResponseStatus}
            responseHeaders={responseHeaders}
            onResponseHeadersChange={setResponseHeaders}
            responseTemplate={responseTemplate}
            onResponseTemplateChange={setResponseTemplate}
            matchers={matchers}
            onMatchersChange={setMatchers}
            preScript={preScript}
            onPreScriptChange={setPreScript}
            postScript={postScript}
            onPostScriptChange={setPostScript}
            onPopulateTestFromMatchers={handlePopulateTestFromMatchers}
          />
        )}
      </div>
    </div>
  );
}
