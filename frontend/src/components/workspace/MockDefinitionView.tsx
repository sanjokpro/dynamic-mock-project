'use client';

import React, { useState } from 'react';
import { ArrowRightLeft, Sparkles, Trash2, Code2 } from 'lucide-react';
import { HeadersEditor, HeaderRow } from './HeadersEditor';
import { CodeEditor } from './CodeEditor';
import { ContextualTooltip } from './ContextualTooltip';
import { cn } from '@/lib/utils';

export interface MockDefinitionViewProps {
  responseStatus: number;
  onResponseStatusChange: (status: number) => void;
  responseHeaders: Record<string, string>;
  onResponseHeadersChange: (headers: Record<string, string>) => void;
  responseTemplate: string;
  onResponseTemplateChange: (val: string) => void;
  matchers: {
    headers?: Record<string, string>;
    queryParams?: Record<string, string>;
    bodyMatchType?: string;
    bodyMatchPattern?: string;
  };
  onMatchersChange: (matchers: any) => void;
  preScript: string;
  onPreScriptChange: (val: string) => void;
  postScript: string;
  onPostScriptChange: (val: string) => void;
  onPopulateTestFromMatchers?: () => void;
}

type MockTab = 'matchers' | 'response' | 'body' | 'scripts';

const TEMPLATE_SNIPPETS = [
  { label: 'UUID', tag: '{{random.uuid}}' },
  { label: 'Timestamp', tag: '{{now}}' },
  { label: 'Req Param', tag: '{{request.queryParams.id}}' },
  { label: 'Req Header', tag: '{{request.headers.authorization}}' },
  { label: 'Req Body', tag: '{{request.body.field}}' },
];

export function MockDefinitionView({
  responseStatus,
  onResponseStatusChange,
  responseHeaders,
  onResponseHeadersChange,
  responseTemplate,
  onResponseTemplateChange,
  matchers,
  onMatchersChange,
  preScript,
  onPreScriptChange,
  postScript,
  onPostScriptChange,
  onPopulateTestFromMatchers,
}: MockDefinitionViewProps) {
  const [activeTab, setActiveTab] = useState<MockTab>('matchers');

  const bodyMatchType = matchers?.bodyMatchType || 'none';
  const bodyMatchPattern = matchers?.bodyMatchPattern || '';

  const updateMatchers = (patch: Record<string, any>) => {
    onMatchersChange({ ...matchers, ...patch });
  };

  const insertSnippet = (snippet: string) => {
    onResponseTemplateChange(responseTemplate + (responseTemplate ? ' ' : '') + snippet);
  };

  return (
    <div className="flex flex-col h-full overflow-hidden bg-background">
      {/* Sub-Tabs for Mock Definition */}
      <div className="flex items-center justify-between border-b text-xs px-2 bg-muted/10 shrink-0">
        <div className="flex">
          {[
            { id: 'matchers', label: 'Request Matchers' },
            { id: 'response', label: 'Response Status & Headers' },
            { id: 'body', label: 'Response Template' },
            { id: 'scripts', label: 'Polyglot Scripts' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as MockTab)}
              className={cn(
                "px-3 py-2 transition-colors cursor-pointer text-xs font-medium border-b-2",
                activeTab === tab.id
                  ? "border-primary text-foreground"
                  : "border-transparent text-muted-foreground hover:text-foreground"
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Populate Test Client Shortcut Button */}
        {onPopulateTestFromMatchers && (
          <button
            type="button"
            onClick={onPopulateTestFromMatchers}
            className="flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-medium text-primary bg-primary/10 hover:bg-primary/20 rounded transition-colors cursor-pointer"
            title="Populate outgoing test request with current matcher criteria"
          >
            <ArrowRightLeft className="h-3 w-3" />
            <span>Fill Test from Matchers</span>
          </button>
        )}
      </div>

      {/* Tab Panels */}
      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
        {/* Matchers Tab */}
        {activeTab === 'matchers' && (
          <div className="space-y-6 max-w-4xl">
            {/* Headers Matcher */}
            <div className="border border-border/80 rounded-lg p-3 bg-card">
              <HeadersEditor
                title="Header Matchers"
                tooltipTitle="Header Matchers"
                tooltipDescription="Matches incoming HTTP request headers against regex patterns or exact values. All specified matchers must be satisfied."
                tooltipSyntax="X-Signature: ^[a-f0-9]{32}$"
                placeholderKey="Header Name"
                placeholderValue="Regex or Exact Value"
                entries={matchers?.headers || {}}
                onChange={(headers) => {
                  updateMatchers({ headers: Object.keys(headers).length > 0 ? headers : undefined });
                }}
              />
            </div>

            {/* Query Params Matcher */}
            <div className="border border-border/80 rounded-lg p-3 bg-card">
              <HeadersEditor
                title="Query Param Matchers"
                tooltipTitle="Query Param Matchers"
                tooltipDescription="Matches URL query string parameters against regex patterns or exact values."
                tooltipSyntax="status: (active|pending)"
                placeholderKey="Param Name"
                placeholderValue="Regex or Exact Value"
                entries={matchers?.queryParams || {}}
                onChange={(queryParams) => {
                  updateMatchers({ queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined });
                }}
              />
            </div>

            {/* Body Matchers */}
            <div className="border border-border/80 rounded-lg p-3 bg-card space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
                    Body Matchers
                  </span>
                  <ContextualTooltip
                    title="Body Matcher Strategy"
                    description="Validates incoming payload against content rules (Equals, Contains, Regex, JSONPath, or XPath)."
                    syntaxExample="$.account.balance > 100"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <select
                  value={bodyMatchType}
                  onChange={(e) => updateMatchers({ bodyMatchType: e.target.value || 'none' })}
                  className="w-full bg-background border border-border rounded px-2.5 py-1.5 text-xs focus:ring-1 focus:ring-primary focus:border-primary outline-none cursor-pointer"
                >
                  <option value="none">No body matching (Accept any body)</option>
                  <option value="equals">Equals (Exact string match)</option>
                  <option value="contains">Contains (Substring match)</option>
                  <option value="regex">Regex (Regular expression pattern)</option>
                  <option value="jsonpath">JSONPath (Evaluate expression against JSON payload)</option>
                  <option value="xpath">XPath (Evaluate expression against XML payload)</option>
                </select>

                {bodyMatchType !== 'none' && (
                  <div className="flex items-center gap-2 mt-2">
                    <input
                      placeholder={
                        bodyMatchType === 'jsonpath'
                          ? '$.account.id == 12345'
                          : bodyMatchType === 'xpath'
                          ? '//*[local-name()=\'stan\'] == 987654'
                          : bodyMatchType === 'regex'
                          ? '^[0-9]{16}$'
                          : 'Expected body value'
                      }
                      value={bodyMatchPattern}
                      onChange={(e) => updateMatchers({ bodyMatchPattern: e.target.value })}
                      className="flex-1 bg-background border border-border rounded px-2.5 py-1.5 text-xs font-mono focus:ring-1 focus:ring-primary focus:border-primary outline-none"
                    />
                    {bodyMatchPattern && (
                      <button
                        type="button"
                        onClick={() => updateMatchers({ bodyMatchPattern: '' })}
                        className="p-1 text-muted-foreground hover:text-destructive transition-colors cursor-pointer"
                        title="Clear pattern"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Response Status & Headers Tab */}
        {activeTab === 'response' && (
          <div className="space-y-6 max-w-4xl">
            <div className="border border-border/80 rounded-lg p-3 bg-card space-y-3">
              <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider block">
                HTTP Response Status
              </span>
              <div className="flex items-center gap-3">
                <input
                  type="number"
                  value={responseStatus}
                  onChange={(e) => onResponseStatusChange(parseInt(e.target.value) || 200)}
                  className="w-24 bg-background border border-border rounded px-3 py-1.5 text-sm font-semibold outline-none focus:ring-1 focus:ring-primary"
                  min={100}
                  max={599}
                />
                <span className="text-xs text-muted-foreground">
                  Standard response status returned when matchers evaluate true.
                </span>
              </div>
            </div>

            <div className="border border-border/80 rounded-lg p-3 bg-card">
              <HeadersEditor
                title="Mock Response Headers"
                tooltipTitle="Response Headers"
                tooltipDescription="Custom HTTP headers returned to client callers alongside the mock response payload."
                entries={responseHeaders}
                onChange={(headers) => onResponseHeadersChange(headers)}
              />
            </div>
          </div>
        )}

        {/* Response Template Tab */}
        {activeTab === 'body' && (
          <div className="h-full flex flex-col gap-2">
            <div className="flex items-center justify-between pb-1 shrink-0">
              <span className="text-xs font-medium text-muted-foreground">
                Handlebars Dynamic Payload Template
              </span>
              <div className="flex items-center gap-1.5">
                <Sparkles className="h-3 w-3 text-primary" />
                <span className="text-[11px] text-muted-foreground font-medium">Quick Insert:</span>
                {TEMPLATE_SNIPPETS.map((snippet) => (
                  <button
                    key={snippet.label}
                    type="button"
                    onClick={() => insertSnippet(snippet.tag)}
                    className="px-2 py-0.5 text-[10px] bg-muted hover:bg-accent rounded text-foreground border border-border/50 font-mono transition-colors cursor-pointer"
                  >
                    {snippet.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex-1 min-h-[300px] border border-border rounded-md overflow-hidden">
              <CodeEditor
                value={responseTemplate}
                onChange={(val) => onResponseTemplateChange(val || '')}
                language="json"
              />
            </div>
          </div>
        )}

        {/* Scripts Tab */}
        {activeTab === 'scripts' && (
          <div className="h-full flex flex-col gap-4">
            <div className="flex-1 flex flex-col gap-2 min-h-[180px]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-muted-foreground uppercase">
                  Pre-Request Script (JavaScript / GraalVM)
                </span>
                <span className="text-[10px] text-muted-foreground">
                  Available globals: <code className="text-foreground">context</code>, <code className="text-foreground">request</code>
                </span>
              </div>
              <div className="flex-1 border border-border rounded-md overflow-hidden min-h-[140px]">
                <CodeEditor
                  value={preScript}
                  onChange={(val) => onPreScriptChange(val || '')}
                  language="javascript"
                />
              </div>
            </div>

            <div className="flex-1 flex flex-col gap-2 min-h-[180px]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-muted-foreground uppercase">
                  Post-Response Script (JavaScript / GraalVM)
                </span>
                <span className="text-[10px] text-muted-foreground">
                  Available globals: <code className="text-foreground">context</code>, <code className="text-foreground">response</code>
                </span>
              </div>
              <div className="flex-1 border border-border rounded-md overflow-hidden min-h-[140px]">
                <CodeEditor
                  value={postScript}
                  onChange={(val) => onPostScriptChange(val || '')}
                  language="javascript"
                />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
