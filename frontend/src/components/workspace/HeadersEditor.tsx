'use client';

import React, { useState, useEffect, useRef, useId, useCallback, useMemo } from 'react';
import { Plus, Trash2, Code2, Table as TableIcon, CheckSquare, Square, AlertTriangle } from 'lucide-react';
import { ContextualTooltip } from './ContextualTooltip';
import { cn } from '@/lib/utils';

export interface HeaderRow {
  id: string;
  key: string;
  value: string;
  enabled: boolean;
}

export interface HeaderValidationResult {
  isValid: boolean;
  error?: string;
}

export interface RawLineValidation {
  lineNumber: number;
  line: string;
  type: 'error' | 'warning' | 'info';
  message: string;
}

export interface HeadersEditorProps {
  title: string;
  entries?: Record<string, string>;
  initialRows?: HeaderRow[];
  onChange?: (entries: Record<string, string>, rows: HeaderRow[]) => void;
  tooltipTitle?: string;
  tooltipDescription?: string;
  tooltipSyntax?: string;
  placeholderKey?: string;
  placeholderValue?: string;
  className?: string;
  validateAs?: 'header' | 'queryParam' | 'none';
}

const COMMON_HTTP_HEADERS = [
  'Accept',
  'Accept-Encoding',
  'Accept-Language',
  'Authorization',
  'Cache-Control',
  'Content-Type',
  'Cookie',
  'Origin',
  'User-Agent',
  'X-Correlation-Id',
  'X-Request-Id',
  'X-Forwarded-For',
  'X-Dynamic-Mock-Env'
];

/**
 * Validates an HTTP Header field name per RFC 7230 / RFC 9110 specification.
 * A field-name is a token consisting of 1*tchar:
 * tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
 */
export function validateHeaderName(name: string): HeaderValidationResult {
  if (!name || name.trim() === '') {
    return { isValid: false, error: 'Header name cannot be empty' };
  }

  // Whitespace check (RFC 7230 Section 3.2.4: no whitespace allowed in field-name)
  if (/\s/.test(name)) {
    return { isValid: false, error: 'Header names cannot contain spaces or whitespace (RFC 7230)' };
  }

  // Colon check (RFC 7230 Section 3.2: colon is field delimiter)
  if (name.includes(':')) {
    return { isValid: false, error: 'Header names cannot contain colons' };
  }

  // Non-ASCII check (RFC 7230 Section 3.2.4: only visible US-ASCII characters 0x21-0x7E)
  if (/[^\x21-\x7E]/.test(name)) {
    return { isValid: false, error: 'Header names must contain only visible ASCII characters' };
  }

  // RFC 7230 token regex
  const validTokenRegex = /^[!#$%&'*+\-.^_`|~0-9a-zA-Z]+$/;
  if (!validTokenRegex.test(name)) {
    return {
      isValid: false,
      error: 'Header name contains invalid characters. Disallowed: ( ) < > @ , ; : \\ " / [ ] ? = { }'
    };
  }

  return { isValid: true };
}

/**
 * Validates raw header lines and provides real-time Postman-like feedback.
 */
export function validateRawHeaderLines(rawText: string, isHeader = true): RawLineValidation[] {
  if (!rawText || !rawText.trim()) return [];
  const lines = rawText.split(/\r?\n/);
  const issues: RawLineValidation[] = [];

  lines.forEach((line, idx) => {
    const trimmed = line.trim();
    if (!trimmed) return; // Blank lines are valid while typing

    // Postman-style // comments
    const isComment = trimmed.startsWith('//');
    const content = isComment ? trimmed.replace(/^\/\/\s*/, '') : trimmed;
    if (!content) return;

    const colonIdx = content.indexOf(':');
    if (colonIdx === -1) {
      issues.push({
        lineNumber: idx + 1,
        line: trimmed,
        type: 'info',
        message: `Missing ':' separator. Expected 'Header-Name: Header-Value'`
      });
      return;
    }

    const key = content.substring(0, colonIdx);
    if (!key.trim()) {
      issues.push({
        lineNumber: idx + 1,
        line: trimmed,
        type: 'error',
        message: 'Header name before colon cannot be empty'
      });
      return;
    }

    // RFC 7230: No whitespace between field-name and colon
    if (/\s$/.test(key)) {
      issues.push({
        lineNumber: idx + 1,
        line: trimmed,
        type: 'warning',
        message: `Whitespace before colon is forbidden by RFC 7230 ('${key.trim()}:')`
      });
    }

    if (isHeader) {
      const keyVal = validateHeaderName(key.trim());
      if (!keyVal.isValid && keyVal.error) {
        issues.push({
          lineNumber: idx + 1,
          line: trimmed,
          type: 'error',
          message: keyVal.error
        });
      }
    }
  });

  return issues;
}

/**
 * Parses multiline raw string into HeaderRow items.
 * Supports Postman-style `//` comments for disabled headers.
 */
export function parseRawHeaders(rawText: string): HeaderRow[] {
  const lines = rawText.split(/\r?\n/);
  const rows: HeaderRow[] = [];
  const colonRegex = /^(.*?):[ \t]*(.*)$/;

  lines.forEach((line, index) => {
    const trimmedLine = line.trim();
    if (!trimmedLine) return;

    let isEnabled = true;
    let content = line;

    if (trimmedLine.startsWith('//')) {
      isEnabled = false;
      content = trimmedLine.replace(/^\/\/\s*/, '');
    }

    const match = colonRegex.exec(content);
    if (match) {
      rows.push({
        id: `raw-${Date.now()}-${index}-${Math.random().toString(36).substr(2, 5)}`,
        key: match[1].trim(),
        value: match[2].trim(),
        enabled: isEnabled
      });
    } else {
      rows.push({
        id: `raw-${Date.now()}-${index}-${Math.random().toString(36).substr(2, 5)}`,
        key: content.trim(),
        value: '',
        enabled: isEnabled
      });
    }
  });

  return rows;
}

/**
 * Formats HeaderRow items into multiline raw string.
 * @param includeDisabledAsComments When true, formats disabled rows as `// Key: Value`
 */
export function formatRawHeaders(rows: HeaderRow[], includeDisabledAsComments = false): string {
  if (includeDisabledAsComments) {
    return rows
      .filter(row => row.key.trim() !== '')
      .map(row => {
        const prefix = row.enabled ? '' : '// ';
        const valStr = row.value !== '' ? `: ${row.value}` : ':';
        return `${prefix}${row.key}${valStr}`;
      })
      .join('\n');
  }

  return rows
    .filter(row => row.enabled && row.key.trim() !== '')
    .map(row => `${row.key}: ${row.value}`)
    .join('\n');
}

export function HeadersEditor({
  title,
  entries = {},
  initialRows,
  onChange,
  tooltipTitle,
  tooltipDescription,
  tooltipSyntax,
  placeholderKey = 'Key',
  placeholderValue = 'Value',
  className,
  validateAs = 'header'
}: HeadersEditorProps) {
  const datalistId = useId();
  const [mode, setMode] = useState<'table' | 'raw'>('table');

  const isHeaderValidation = useMemo(() => {
    if (validateAs === 'none') return false;
    if (validateAs === 'queryParam') return false;
    const lowerTitle = (title || '').toLowerCase();
    const lowerKey = (placeholderKey || '').toLowerCase();
    return !lowerTitle.includes('query') && !lowerKey.includes('param');
  }, [validateAs, title, placeholderKey]);

  // Track user typing to prevent props reflection from clobbering active input
  const isUserTypingRef = useRef(false);
  const typingTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Initialize rows from initialRows or entries
  const [rows, setRows] = useState<HeaderRow[]>(() => {
    if (initialRows && Array.isArray(initialRows) && initialRows.length > 0) {
      return initialRows;
    }
    const currentEntries = entries || {};
    return Object.entries(currentEntries).map(([key, value], idx) => ({
      id: `init-${idx}-${key}`,
      key,
      value,
      enabled: true
    }));
  });

  // Raw text state - decoupled from rows during active editing
  const [rawText, setRawText] = useState<string>(() => {
    if (initialRows && Array.isArray(initialRows) && initialRows.length > 0) {
      return formatRawHeaders(initialRows, true);
    }
    return formatRawHeaders(
      Object.entries(entries || {}).map(([key, value], idx) => ({
        id: `init-${idx}-${key}`,
        key,
        value,
        enabled: true
      })),
      true
    );
  });

  const lastEmittedRef = useRef<string>('');

  // Helper to calculate enabled record
  const getEnabledRecord = useCallback((rowList: HeaderRow[]) => {
    const record: Record<string, string> = {};
    rowList.forEach(r => {
      if (r.enabled && r.key.trim() !== '') {
        record[r.key.trim()] = r.value;
      }
    });
    return record;
  }, []);

  // Cleanup typing timer on unmount
  useEffect(() => {
    return () => {
      if (typingTimerRef.current) {
        clearTimeout(typingTimerRef.current);
      }
    };
  }, []);

  // Sync rows from props if external changes happen (e.g. route switched in sidebar)
  useEffect(() => {
    // If user is actively typing, do not let external prop reflection overwrite text
    if (isUserTypingRef.current) {
      return;
    }

    if (initialRows && Array.isArray(initialRows) && initialRows.length > 0) {
      const rowsKey = JSON.stringify(initialRows);
      if (rowsKey !== lastEmittedRef.current) {
        setRows(initialRows);
        if (mode === 'table') {
          setRawText(formatRawHeaders(initialRows, true));
        }
        lastEmittedRef.current = rowsKey;
      }
      return;
    }

    const entriesKey = JSON.stringify(entries || {});
    if (entriesKey !== lastEmittedRef.current) {
      const newRows = Object.entries(entries || {}).map(([key, value], idx) => ({
        id: `ext-${idx}-${key}`,
        key,
        value,
        enabled: true
      }));
      setRows(newRows);
      if (mode === 'table') {
        setRawText(formatRawHeaders(newRows, true));
      }
      lastEmittedRef.current = entriesKey;
    }
  }, [entries, initialRows, mode]);

  const emitChanges = (updatedRows: HeaderRow[]) => {
    const enabledRecord = getEnabledRecord(updatedRows);
    lastEmittedRef.current = JSON.stringify(enabledRecord);
    onChange?.(enabledRecord, updatedRows);
  };

  const markUserTyping = () => {
    isUserTypingRef.current = true;
    if (typingTimerRef.current) {
      clearTimeout(typingTimerRef.current);
    }
    typingTimerRef.current = setTimeout(() => {
      isUserTypingRef.current = false;
    }, 400);
  };

  // Add Row (Table mode)
  const handleAddRow = () => {
    markUserTyping();
    const newRows = [
      ...rows,
      {
        id: `row-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
        key: '',
        value: '',
        enabled: true
      }
    ];
    setRows(newRows);
    setRawText(formatRawHeaders(newRows, true));
    emitChanges(newRows);
  };

  // Update Field (Table mode)
  const handleUpdateRow = (index: number, field: 'key' | 'value', value: string) => {
    markUserTyping();
    const newRows = rows.map((r, i) => (i === index ? { ...r, [field]: value } : r));
    setRows(newRows);
    setRawText(formatRawHeaders(newRows, true));
    emitChanges(newRows);
  };

  // Toggle Checkbox (Table mode)
  const handleToggleRow = (index: number) => {
    markUserTyping();
    const newRows = rows.map((r, i) => (i === index ? { ...r, enabled: !r.enabled } : r));
    setRows(newRows);
    setRawText(formatRawHeaders(newRows, true));
    emitChanges(newRows);
  };

  // Delete Row (Table mode)
  const handleDeleteRow = (index: number) => {
    markUserTyping();
    const newRows = rows.filter((_, i) => i !== index);
    setRows(newRows);
    setRawText(formatRawHeaders(newRows, true));
    emitChanges(newRows);
  };

  // Toggle Table / Raw Mode
  const handleSwitchMode = (newMode: 'table' | 'raw') => {
    if (newMode === 'raw') {
      // Formats table rows into raw text with disabled rows as // comments
      setRawText(formatRawHeaders(rows, true));
    } else {
      // Parses raw text into table rows
      const parsed = parseRawHeaders(rawText);
      setRows(parsed);
      emitChanges(parsed);
    }
    setMode(newMode);
  };

  // Handle Raw Text Change
  // CRITICAL FIX: rawText is updated directly without re-formatting so typing and newlines work seamlessly
  const handleRawChange = (text: string) => {
    markUserTyping();
    setRawText(text);

    const parsed = parseRawHeaders(text);
    setRows(parsed);
    emitChanges(parsed);
  };

  // Real-time validation for raw text mode
  const rawIssues = useMemo(() => {
    if (mode !== 'raw') return [];
    return validateRawHeaderLines(rawText, isHeaderValidation);
  }, [mode, rawText, isHeaderValidation]);

  return (
    <div className={cn("space-y-3", className)}>
      {/* Header Bar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
            {title}
          </span>
          {tooltipTitle && tooltipDescription && (
            <ContextualTooltip
              title={tooltipTitle}
              description={tooltipDescription}
              syntaxExample={tooltipSyntax}
            />
          )}
        </div>

        <div className="flex items-center gap-2">
          {/* View Mode Toggle */}
          <div className="flex items-center bg-muted/40 p-0.5 rounded-md border border-border/60">
            <button
              type="button"
              onClick={() => handleSwitchMode('table')}
              className={cn(
                "flex items-center gap-1 px-2 py-0.5 text-[11px] rounded transition-all cursor-pointer",
                mode === 'table'
                  ? "bg-background text-foreground shadow-xs font-medium"
                  : "text-muted-foreground hover:text-foreground"
              )}
              title="Table view"
            >
              <TableIcon className="h-3 w-3" />
              <span>Table</span>
            </button>
            <button
              type="button"
              onClick={() => handleSwitchMode('raw')}
              className={cn(
                "flex items-center gap-1 px-2 py-0.5 text-[11px] rounded transition-all cursor-pointer",
                mode === 'raw'
                  ? "bg-background text-foreground shadow-xs font-medium"
                  : "text-muted-foreground hover:text-foreground"
              )}
              title="Raw / Bulk text view"
            >
              <Code2 className="h-3 w-3" />
              <span>Raw</span>
            </button>
          </div>

          {/* Add Row Button (in Table mode) */}
          {mode === 'table' && (
            <button
              type="button"
              onClick={handleAddRow}
              className="flex items-center gap-1 text-xs text-primary hover:underline transition-colors cursor-pointer"
            >
              <Plus className="h-3 w-3" />
              <span>Add Row</span>
            </button>
          )}
        </div>
      </div>

      {/* Autocomplete Datalist */}
      <datalist id={datalistId}>
        {COMMON_HTTP_HEADERS.map(header => (
          <option key={header} value={header} />
        ))}
      </datalist>

      {/* Mode Render */}
      {mode === 'table' ? (
        <div className="space-y-2">
          {rows.length === 0 ? (
            <div className="text-[11px] text-muted-foreground italic py-2">
              No entries defined. Click "Add Row" or switch to "Raw" to paste headers.
            </div>
          ) : (
            rows.map((row, index) => {
              const keyValidation = isHeaderValidation && row.key.trim()
                ? validateHeaderName(row.key)
                : { isValid: true };

              return (
                <div
                  key={row.id}
                  className={cn(
                    "flex items-center gap-2 group transition-opacity",
                    !row.enabled && "opacity-50"
                  )}
                >
                  {/* Enable / Disable Checkbox */}
                  <button
                    type="button"
                    onClick={() => handleToggleRow(index)}
                    className="text-muted-foreground hover:text-foreground p-0.5 transition-colors cursor-pointer"
                    title={row.enabled ? "Disable this entry" : "Enable this entry"}
                    aria-label={row.enabled ? `Disable ${row.key || 'row'}` : `Enable ${row.key || 'row'}`}
                  >
                    {row.enabled ? (
                      <CheckSquare className="h-3.5 w-3.5 text-primary" />
                    ) : (
                      <Square className="h-3.5 w-3.5 text-muted-foreground" />
                    )}
                  </button>

                  {/* Key Input Container with RFC Validation Highlight */}
                  <div className="relative flex-1 flex items-center">
                    <input
                      list={datalistId}
                      placeholder={placeholderKey}
                      value={row.key}
                      onChange={(e) => handleUpdateRow(index, 'key', e.target.value)}
                      className={cn(
                        "w-full bg-background border rounded px-2 py-1 text-xs font-mono focus:ring-1 outline-none transition-colors",
                        !keyValidation.isValid
                          ? "border-amber-500/80 focus:border-amber-500 focus:ring-amber-500/30 text-amber-900 dark:text-amber-200 pr-7"
                          : "border-border focus:ring-primary focus:border-primary"
                      )}
                    />
                    {!keyValidation.isValid && (
                      <span
                        className="absolute right-2 text-amber-500 cursor-help"
                        title={keyValidation.error}
                        aria-label={keyValidation.error}
                      >
                        <AlertTriangle className="h-3.5 w-3.5" />
                      </span>
                    )}
                  </div>

                  {/* Value Input */}
                  <input
                    placeholder={placeholderValue}
                    value={row.value}
                    onChange={(e) => handleUpdateRow(index, 'value', e.target.value)}
                    className="flex-1 bg-background border border-border rounded px-2 py-1 text-xs font-mono focus:ring-1 focus:ring-primary focus:border-primary outline-none"
                  />

                  {/* Delete Button */}
                  <button
                    type="button"
                    onClick={() => handleDeleteRow(index)}
                    className="p-1 text-muted-foreground opacity-60 hover:opacity-100 hover:text-destructive transition-all cursor-pointer"
                    title="Delete row"
                    aria-label="Delete row"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              );
            })
          )}
        </div>
      ) : (
        /* Raw Multiline Text Mode */
        <div className="space-y-2">
          <textarea
            value={rawText}
            onChange={(e) => handleRawChange(e.target.value)}
            placeholder={`Content-Type: application/json\nAuthorization: Bearer token\n// X-Disabled-Header: test`}
            rows={Math.max(5, rawText.split('\n').length + 1)}
            className={cn(
              "w-full bg-background border rounded-md p-2.5 text-xs font-mono focus:ring-1 outline-none custom-scrollbar leading-relaxed transition-colors",
              rawIssues.some(i => i.type === 'error')
                ? "border-amber-500/70 focus:border-amber-500 focus:ring-amber-500/30"
                : "border-border focus:ring-primary focus:border-primary"
            )}
          />

          {/* Real-time Postman-style Validation Feedback */}
          {rawIssues.length > 0 ? (
            <div className="bg-amber-500/10 border border-amber-500/30 rounded-md p-2.5 space-y-1 text-xs">
              <div className="flex items-center gap-1.5 font-semibold text-amber-700 dark:text-amber-300 text-[11px]">
                <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                <span>HTTP Header Format Notice ({rawIssues.length})</span>
              </div>
              <ul className="space-y-0.5 text-[11px] text-amber-800 dark:text-amber-200/90 font-mono">
                {rawIssues.slice(0, 5).map((issue, idx) => (
                  <li key={idx} className="flex items-start gap-1.5">
                    <span className="font-semibold text-amber-600 dark:text-amber-400">Line {issue.lineNumber}:</span>
                    <span>{issue.message}</span>
                  </li>
                ))}
                {rawIssues.length > 5 && (
                  <li className="text-[10px] text-muted-foreground italic">
                    ...and {rawIssues.length - 5} more issues
                  </li>
                )}
              </ul>
            </div>
          ) : (
            <p className="text-[10px] text-muted-foreground">
              Enter one header per line in standard <code className="text-foreground">Key: Value</code> format. Prefix with <code className="text-foreground">//</code> to comment out / disable.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
