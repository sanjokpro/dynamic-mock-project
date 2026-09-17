'use client';

import React, { useState, useCallback } from 'react';
import { Clock, Database, Ghost, Copy, Download, Check } from 'lucide-react';
import { useWorkspace } from '@/context/WorkspaceContext';
import { useTheme } from '@/context/ThemeContext';
import { cn } from '@/lib/utils';

type ResponseTab = 'body' | 'headers';

export function ResponsePanel() {
  const { lastResponse } = useWorkspace();
  const { theme } = useTheme();
  const [activeTab, setActiveTab] = useState<ResponseTab>('body');
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(() => {
    if (!lastResponse) return;
    const text = typeof lastResponse.body === 'string' 
      ? lastResponse.body 
      : JSON.stringify(lastResponse.body, null, 2);
    try {
      navigator.clipboard.writeText(text).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }).catch(() => {
        // Fallback for non-HTTPS contexts
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      });
    } catch {
      // Clipboard unavailable
    }
  }, [lastResponse]);

  const handleDownload = useCallback(() => {
    if (!lastResponse) return;
    const text = typeof lastResponse.body === 'string'
      ? lastResponse.body
      : JSON.stringify(lastResponse.body, null, 2);
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'response.json';
    a.click();
    URL.revokeObjectURL(url);
  }, [lastResponse]);

  if (!lastResponse) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-3 bg-panel-bg">
        <Ghost className="h-14 w-14 opacity-[0.08]" strokeWidth={1} />
        <p className="text-sm">Send a request to see the response</p>
      </div>
    );
  }

  const isError = lastResponse.status >= 400;
  const bodyText = typeof lastResponse.body === 'string'
    ? lastResponse.body
    : JSON.stringify(lastResponse.body, null, 2);

  return (
    <div className="flex flex-col h-full bg-panel-bg">
      {/* Header bar */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b bg-panel-header-bg">
        <div className="flex items-center gap-3 text-xs">
          {/* Status badge */}
          <span className={cn(
            "inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md font-semibold text-[11px]",
            isError 
              ? "bg-red-500/10 text-red-500" 
              : "bg-green-500/10 text-green-500"
          )}>
            <span className={cn("w-1.5 h-1.5 rounded-full", isError ? "bg-red-500" : "bg-green-500")} />
            {lastResponse.status} {lastResponse.statusText}
          </span>

          {/* Duration */}
          <span className="flex items-center gap-1 text-muted-foreground">
            <Clock className="h-3 w-3" />
            <span className="tabular-nums">{lastResponse.durationMs}ms</span>
          </span>

          {/* Size */}
          {bodyText && (
            <span className="flex items-center gap-1 text-muted-foreground">
              <Database className="h-3 w-3" />
              <span className="tabular-nums">{(new Blob([bodyText]).size / 1024).toFixed(1)} KB</span>
            </span>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1">
          <button
            onClick={handleCopy}
            className={cn(
              "flex items-center gap-1 px-2 py-1 text-xs rounded transition-colors",
              copied 
                ? "bg-green-500/10 text-green-500" 
                : "text-muted-foreground hover:text-foreground hover:bg-accent"
            )}
            title="Copy response"
          >
            {copied ? <Check className="h-3 w-3" /> : <Copy className="h-3 w-3" />}
            {copied ? 'Copied' : 'Copy'}
          </button>
          <button
            onClick={handleDownload}
            className="flex items-center gap-1 px-2 py-1 text-xs rounded text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
            title="Download response"
          >
            <Download className="h-3 w-3" />
            Download
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b text-xs">
        <button
          onClick={() => setActiveTab('body')}
          className={cn(
            "px-4 py-1.5 transition-colors",
            activeTab === 'body' 
              ? "border-b-2 border-primary font-medium text-foreground" 
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          Body
        </button>
        <button
          onClick={() => setActiveTab('headers')}
          className={cn(
            "px-4 py-1.5 transition-colors",
            activeTab === 'headers' 
              ? "border-b-2 border-primary font-medium text-foreground" 
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          Headers ({Object.keys(lastResponse.headers || {}).length})
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'body' ? (
          <div className="h-full overflow-auto p-3 custom-scrollbar">
            <JsonHighlighter json={bodyText} theme={theme} />
          </div>
        ) : (
          <div className="h-full overflow-auto p-3 custom-scrollbar">
            <div className="space-y-0.5">
              {Object.entries(lastResponse.headers || {}).map(([key, value]) => (
                <div key={key} className="flex gap-3 text-xs py-1 px-2 rounded hover:bg-accent/30">
                  <span className="font-medium text-foreground shrink-0 min-w-[160px]">{key}</span>
                  <span className="text-muted-foreground break-all">{value as string}</span>
                </div>
              ))}
              {(!lastResponse.headers || Object.keys(lastResponse.headers).length === 0) && (
                <div className="text-xs text-muted-foreground italic py-4 text-center">No headers</div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Simple JSON syntax highlighter that renders color-coded tokens.
 * Uses dangerouslySetInnerHTML for the highlighting markup.
 */
function JsonHighlighter({ json, theme }: { json: string; theme: 'light' | 'dark' }) {
  const highlighted = highlightJson(json, theme);

  return (
    <pre className={cn(
      "text-xs leading-relaxed font-mono whitespace-pre-wrap break-all m-0",
      theme === 'dark' ? 'text-slate-200' : 'text-slate-800'
    )}>
      <code dangerouslySetInnerHTML={{ __html: highlighted }} />
    </pre>
  );
}

function highlightJson(json: string, theme: 'light' | 'dark'): string {
  // Colors for dark theme
  const colors = theme === 'dark'
    ? {
        key: '#7dd3fc',      // sky-300
        string: '#a5d6a7',   // green-300
        number: '#ffb74d',   // orange-300
        boolean: '#ce93d8',  // purple-300
        null: '#ef5350',     // red-400
        bracket: '#94a3b8',  // slate-400
        punctuation: '#64748b', // slate-500
      }
    : {
        key: '#0284c7',      // sky-600
        string: '#16a34a',   // green-600
        number: '#ea580c',   // orange-600
        boolean: '#9333ea',  // purple-600
        null: '#dc2626',     // red-600
        bracket: '#475569',  // slate-600
        punctuation: '#94a3b8', // slate-400
      };

  const escapeHtml = (str: string) =>
    str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');

  // Tokenize and highlight
  const tokens: string[] = [];
  const regex = /("(?:\\.|[^"\\])*")\s*(:)|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(\btrue\b|\bfalse\b)|(\bnull\b)|([{}[\]])|([,:])|([^\s{}[\]",:]+)/g;
  let lastIndex = 0;

  let match: RegExpExecArray | null;

  while ((match = regex.exec(json)) !== null) {
    // Append whitespace before this token
    if (match.index > lastIndex) {
      tokens.push(escapeHtml(json.slice(lastIndex, match.index)));
    }

    if (match[1]) {
      // String (key or value)
      const isKey = match[2] === ':';
      tokens.push(`<span style="color:${isKey ? colors.key : colors.string}">${escapeHtml(match[1])}</span>`);
      if (match[2]) {
        tokens.push(`<span style="color:${colors.punctuation}">:</span>`);
      }
    } else if (match[3]) {
      // Number
      tokens.push(`<span style="color:${colors.number}">${escapeHtml(match[3])}</span>`);
    } else if (match[4]) {
      // Boolean
      tokens.push(`<span style="color:${colors.boolean}">${escapeHtml(match[4])}</span>`);
    } else if (match[5]) {
      // Null
      tokens.push(`<span style="color:${colors.null}">${escapeHtml(match[5])}</span>`);
    } else if (match[6]) {
      // Brackets
      tokens.push(`<span style="color:${colors.bracket}">${escapeHtml(match[6])}</span>`);
    } else if (match[7]) {
      // Punctuation
      tokens.push(`<span style="color:${colors.punctuation}">${escapeHtml(match[7])}</span>`);
    }

    lastIndex = regex.lastIndex;
  }

  // Append remaining text
  if (lastIndex < json.length) {
    tokens.push(escapeHtml(json.slice(lastIndex)));
  }

  return tokens.join('');
}
