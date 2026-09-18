'use client';

import React, { useCallback, useRef, useState } from 'react';
import { Upload, FileJson, X, CheckCircle2, AlertTriangle, Loader2, FileUp } from 'lucide-react';
import { usePostmanImport } from '@/hooks/usePostmanImport';
import { ImportReport } from '@/types';
import { cn } from '@/lib/utils';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // mirrors backend spring.servlet.multipart.max-file-size
const MAX_TOTAL_SIZE = 25 * 1024 * 1024; // mirrors backend max-request-size

interface ImportModalProps {
  open: boolean;
  onClose: () => void;
}

type Step = 'select' | 'importing' | 'result';

export function ImportModal({ open, onClose }: ImportModalProps) {
  const [collectionFile, setCollectionFile] = useState<File | null>(null);
  const [environmentFiles, setEnvironmentFiles] = useState<File[]>([]);
  const [step, setStep] = useState<Step>('select');
  const [report, setReport] = useState<ImportReport | null>(null);
  const [failureMessage, setFailureMessage] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const { importPostman, reset } = usePostmanImport();

  const totalSize = (collectionFile ? collectionFile.size : 0) +
    environmentFiles.reduce((sum, f) => sum + f.size, 0);

  const validateAndAdd = useCallback((files: FileList | File[]) => {
    const errors: string[] = [];
    // Track running totals locally: state updates won't be visible within this batch
    let runningTotal = totalSize;
    let firstCollection = collectionFile;
    const newEnvironments: File[] = [];

    for (const file of Array.from(files)) {
      if (!file.name.toLowerCase().endsWith('.json')) {
        errors.push(`"${file.name}" is not a JSON file`);
        continue;
      }
      if (file.size > MAX_FILE_SIZE) {
        errors.push(`"${file.name}" exceeds the 10MB per-file limit`);
        continue;
      }
      if (runningTotal + file.size > MAX_TOTAL_SIZE) {
        errors.push(`"${file.name}" would exceed the 25MB total upload limit`);
        continue;
      }
      runningTotal += file.size;
      if (!firstCollection) {
        firstCollection = file;
      } else {
        newEnvironments.push(file);
      }
    }

    if (firstCollection !== collectionFile) {
      setCollectionFile(firstCollection);
    }
    if (newEnvironments.length > 0) {
      setEnvironmentFiles(prev => [...prev, ...newEnvironments]);
    }
    // Set once at the end - setting per-file then clearing would batch away the errors
    setFailureMessage(errors.length > 0 ? errors.join('\n') : null);
  }, [collectionFile, totalSize]);

  const clearAll = () => {
    setCollectionFile(null);
    setEnvironmentFiles([]);
    setReport(null);
    setFailureMessage(null);
    setStep('select');
    reset();
  };

  const handleClose = () => {
    if (step === 'importing') return; // don't close mid-upload
    clearAll();
    onClose();
  };

  const handleImport = async () => {
    if (!collectionFile) return;
    setStep('importing');
    try {
      const result = await importPostman({ collection: collectionFile, environments: environmentFiles });
      setReport(result);
      setStep('result');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { errors?: string[] } }; message?: string };
      const serverErrors = axiosErr?.response?.data?.errors;
      setFailureMessage(
        Array.isArray(serverErrors) && serverErrors.length > 0
          ? serverErrors.join('; ')
          : axiosErr?.message || 'Import failed. Is the backend running?'
      );
      setStep('result');
    }
  };

  if (!open) return null;

  const canImport = collectionFile !== null && step === 'select';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50" onClick={handleClose} />

      <div className="relative bg-background border border-border rounded-lg shadow-xl w-full max-w-lg mx-4 flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-border">
          <div className="flex items-center gap-2">
            <FileUp className="h-4 w-4 text-primary" />
            <h2 className="text-sm font-semibold text-foreground">Import from Postman</h2>
          </div>
          <button
            onClick={handleClose}
            className="p-1 rounded-md hover:bg-accent text-muted-foreground hover:text-foreground transition-colors"
            aria-label="Close import dialog"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-4 py-3">
          {step === 'select' && (
            <>
              <div
                onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={(e) => { e.preventDefault(); setIsDragging(false); validateAndAdd(e.dataTransfer.files); }}
                onClick={() => inputRef.current?.click()}
                className={cn(
                  'border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors',
                  isDragging ? 'border-primary bg-primary/5' : 'border-border hover:bg-accent/30'
                )}
              >
                <Upload className="h-6 w-6 mx-auto mb-2 text-muted-foreground" />
                <p className="text-xs text-foreground font-medium">
                  Drop Postman export here, or click to browse
                </p>
                <p className="text-[10px] text-muted-foreground mt-1">
                  One collection JSON + optional environment JSON files (v2.1 export)
                </p>
                <input
                  ref={inputRef}
                  type="file"
                  accept=".json,application/json"
                  multiple
                  className="hidden"
                  onChange={(e) => {
                    if (e.target.files) validateAndAdd(e.target.files);
                    e.target.value = '';
                  }}
                />
              </div>

              {failureMessage && (
                <div className="mt-3 flex items-start gap-2 text-xs text-destructive bg-destructive/10 border border-destructive/30 rounded-md px-3 py-2">
                  <AlertTriangle className="h-3.5 w-3.5 mt-0.5 shrink-0" />
                  <span className="whitespace-pre-line">{failureMessage}</span>
                </div>
              )}

              {(collectionFile || environmentFiles.length > 0) && (
                <div className="mt-3 space-y-1.5">
                  <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">
                    Selected files ({(totalSize / 1024).toFixed(0)} KB total)
                  </p>
                  {collectionFile && (
                    <FileRow
                      key="collection"
                      name={collectionFile.name}
                      size={collectionFile.size}
                      tag="Collection"
                      onRemove={() => setCollectionFile(null)}
                    />
                  )}
                  {environmentFiles.map((file, idx) => (
                    <FileRow
                      key={`${file.name}-${idx}`}
                      name={file.name}
                      size={file.size}
                      tag="Environment"
                      onRemove={() => setEnvironmentFiles(prev => prev.filter((_, i) => i !== idx))}
                    />
                  ))}
                </div>
              )}
            </>
          )}

          {step === 'importing' && (
            <div className="py-10 text-center">
              <Loader2 className="h-6 w-6 mx-auto mb-3 animate-spin text-primary" />
              <p className="text-xs text-muted-foreground">Importing Postman collection...</p>
            </div>
          )}

          {step === 'result' && report && (
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm">
                {report.errors.length === 0 ? (
                  <CheckCircle2 className="h-4 w-4 text-green-500" />
                ) : (
                  <AlertTriangle className="h-4 w-4 text-yellow-500" />
                )}
                <span className="font-medium text-foreground">
                  {report.errors.length === 0 ? 'Import complete' : 'Import finished with errors'}
                </span>
              </div>

              <div className="grid grid-cols-3 gap-2">
                <StatCard label="Environments" value={report.environments} />
                <StatCard label="Collections" value={report.collections} />
                <StatCard label="Routes" value={report.routes} />
              </div>

              {report.warnings.length > 0 && (
                <ReportList title="Warnings" items={report.warnings} tone="warning" />
              )}
              {report.errors.length > 0 && (
                <ReportList title="Errors" items={report.errors} tone="error" />
              )}
            </div>
          )}

          {step === 'result' && !report && failureMessage && (
            <div className="flex items-start gap-2 text-xs text-destructive bg-destructive/10 border border-destructive/30 rounded-md px-3 py-2">
              <AlertTriangle className="h-3.5 w-3.5 mt-0.5 shrink-0" />
              <span>{failureMessage}</span>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 px-4 py-3 border-t border-border">
          {step === 'select' && (
            <>
              <button
                onClick={handleClose}
                className="px-3 h-7 text-xs text-muted-foreground hover:text-foreground hover:bg-accent rounded-md transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleImport}
                disabled={!canImport}
                className={cn(
                  'px-3 h-7 text-xs font-medium rounded-md transition-colors flex items-center gap-1.5',
                  canImport
                    ? 'bg-primary text-primary-foreground hover:bg-primary/90'
                    : 'bg-muted text-muted-foreground cursor-not-allowed'
                )}
              >
                <FileJson className="h-3.5 w-3.5" />
                Import
              </button>
            </>
          )}
          {step === 'result' && (
            <button
              onClick={handleClose}
              className="px-3 h-7 text-xs font-medium bg-primary text-primary-foreground hover:bg-primary/90 rounded-md transition-colors"
            >
              Done
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function FileRow({ name, size, tag, onRemove }: {
  name: string;
  size: number;
  tag: string;
  onRemove: () => void;
}) {
  return (
    <div className="flex items-center gap-2 px-2.5 py-1.5 bg-accent/20 border border-border rounded-md text-xs">
      <FileJson className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
      <span className="flex-1 truncate text-foreground">{name}</span>
      <span className="text-[9px] text-muted-foreground uppercase px-1.5 py-0.5 bg-muted rounded shrink-0">{tag}</span>
      <span className="text-[10px] text-muted-foreground tabular-nums shrink-0">{(size / 1024).toFixed(1)} KB</span>
      <button
        onClick={(e) => { e.stopPropagation(); onRemove(); }}
        className="p-0.5 rounded hover:bg-accent text-muted-foreground hover:text-foreground shrink-0"
        aria-label={`Remove ${name}`}
      >
        <X className="h-3 w-3" />
      </button>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-accent/20 border border-border rounded-md px-2 py-2 text-center">
      <div className="text-base font-semibold text-foreground tabular-nums">{value}</div>
      <div className="text-[9px] text-muted-foreground uppercase tracking-wider">{label}</div>
    </div>
  );
}

function ReportList({ title, items, tone }: { title: string; items: string[]; tone: 'warning' | 'error' }) {
  return (
    <div className={cn(
      'border rounded-md px-3 py-2',
      tone === 'warning' ? 'border-yellow-500/30 bg-yellow-500/5' : 'border-destructive/30 bg-destructive/5'
    )}>
      <p className={cn(
        'text-[10px] font-bold uppercase tracking-wider mb-1',
        tone === 'warning' ? 'text-yellow-600 dark:text-yellow-500' : 'text-destructive'
      )}>
        {title} ({items.length})
      </p>
      <ul className="space-y-0.5 max-h-32 overflow-y-auto">
        {items.map((item, idx) => (
          <li key={idx} className="text-[11px] text-muted-foreground leading-snug">• {item}</li>
        ))}
      </ul>
    </div>
  );
}
