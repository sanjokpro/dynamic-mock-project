'use client';

import React, { useState } from 'react';
import { Terminal, Copy } from 'lucide-react';
import { HeadersEditor, HeaderRow } from './HeadersEditor';
import { CodeEditor } from './CodeEditor';
import { cn } from '@/lib/utils';

export interface TestClientViewProps {
  queryParams: Record<string, string>;
  onQueryParamsChange: (params: Record<string, string>) => void;
  requestHeaders: Record<string, string>;
  onRequestHeadersChange: (headers: Record<string, string>, rows: HeaderRow[]) => void;
  headerRows?: HeaderRow[];
  requestBodyType: 'none' | 'json' | 'form' | 'raw';
  onRequestBodyTypeChange: (type: 'none' | 'json' | 'form' | 'raw') => void;
  requestBodyContent: string;
  onRequestBodyContentChange: (content: string) => void;
  onCopyCurl?: () => void;
}

type TestTab = 'params' | 'headers' | 'body';

export function TestClientView({
  queryParams,
  onQueryParamsChange,
  requestHeaders,
  onRequestHeadersChange,
  headerRows,
  requestBodyType,
  onRequestBodyTypeChange,
  requestBodyContent,
  onRequestBodyContentChange,
  onCopyCurl,
}: TestClientViewProps) {
  const [activeTab, setActiveTab] = useState<TestTab>('headers');

  return (
    <div className="flex flex-col h-full overflow-hidden bg-background">
      {/* Test Tabs Bar */}
      <div className="flex items-center justify-between border-b text-xs px-2 bg-muted/10 shrink-0">
        <div className="flex">
          {[
            { id: 'headers', label: `Headers (${Object.keys(requestHeaders || {}).length})` },
            { id: 'params', label: `Params (${Object.keys(queryParams || {}).length})` },
            { id: 'body', label: `Body ${requestBodyType !== 'none' ? `(${requestBodyType})` : ''}` },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as TestTab)}
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

        {/* Action: Copy cURL */}
        {onCopyCurl && (
          <button
            type="button"
            onClick={onCopyCurl}
            className="flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-medium text-muted-foreground hover:text-foreground hover:bg-accent rounded transition-colors cursor-pointer"
            title="Copy test request as executable cURL command"
          >
            <Terminal className="h-3 w-3" />
            <span>Copy as cURL</span>
          </button>
        )}
      </div>

      {/* Tab Panels */}
      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
        {/* Outgoing Request Headers */}
        {activeTab === 'headers' && (
          <div className="max-w-4xl">
            <HeadersEditor
              title="Request Headers (Outgoing)"
              tooltipTitle="Outgoing Test Request Headers"
              tooltipDescription="Headers transmitted along with your test request execution. Use checkboxes to quickly enable/disable headers without deleting them."
              entries={requestHeaders}
              initialRows={headerRows}
              onChange={onRequestHeadersChange}
            />
          </div>
        )}

        {/* Query Params */}
        {activeTab === 'params' && (
          <div className="max-w-4xl">
            <HeadersEditor
              title="Query Parameters"
              tooltipTitle="URL Query Parameters"
              tooltipDescription="Key-value pairs appended as query string parameters (?key=value) to the outgoing test request URL."
              placeholderKey="Parameter"
              placeholderValue="Value"
              entries={queryParams}
              onChange={(params) => onQueryParamsChange(params)}
            />
          </div>
        )}

        {/* Request Body */}
        {activeTab === 'body' && (
          <div className="space-y-4 max-w-4xl h-full flex flex-col">
            <div className="flex items-center gap-3 shrink-0">
              <span className="text-xs font-bold text-muted-foreground uppercase">Body Type:</span>
              <select
                value={requestBodyType}
                onChange={(e) => onRequestBodyTypeChange(e.target.value as any)}
                className="bg-background border border-border rounded px-2.5 py-1 text-xs focus:ring-1 focus:ring-primary outline-none cursor-pointer"
              >
                <option value="none">No Body</option>
                <option value="json">JSON (application/json)</option>
                <option value="form">Form Data (application/x-www-form-urlencoded)</option>
                <option value="raw">Raw (text/plain)</option>
              </select>
            </div>

            {requestBodyType !== 'none' ? (
              <div className="flex-1 min-h-[240px] border border-border rounded-md overflow-hidden">
                <CodeEditor
                  value={requestBodyContent}
                  onChange={(val) => onRequestBodyContentChange(val || '')}
                  language={requestBodyType === 'json' ? 'json' : 'javascript'}
                  enableValidation={requestBodyType === 'json'}
                />
              </div>
            ) : (
              <div className="text-xs text-muted-foreground italic py-8 text-center border border-dashed rounded-lg">
                This request has no body payload. Select JSON, Form Data, or Raw above to supply one.
              </div>
            )}

            {requestBodyType === 'form' && (
              <div className="text-[11px] text-muted-foreground italic shrink-0">
                Tip: Enter form data as key=value pairs, one per line.
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
