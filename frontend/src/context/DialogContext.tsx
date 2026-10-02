import React, { createContext, useContext, useState, ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface DialogOptions {
  title: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
}

interface PromptOptions extends DialogOptions {
  defaultValue?: string;
  placeholder?: string;
}

interface DialogContextType {
  confirm: (options: DialogOptions) => Promise<boolean>;
  prompt: (options: PromptOptions) => Promise<string | null>;
}

const DialogContext = createContext<DialogContextType | undefined>(undefined);

export function DialogProvider({ children }: { children: ReactNode }) {
  const [confirmState, setConfirmState] = useState<{ options: DialogOptions; resolve: (val: boolean) => void } | null>(null);
  const [promptState, setPromptState] = useState<{ options: PromptOptions; resolve: (val: string | null) => void } | null>(null);
  const [promptValue, setPromptValue] = useState('');
  const [error, setError] = useState('');

  const confirm = (options: DialogOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      setConfirmState({ options, resolve });
    });
  };

  const prompt = (options: PromptOptions): Promise<string | null> => {
    return new Promise((resolve) => {
      setPromptValue(options.defaultValue || '');
      setError('');
      setPromptState({ options, resolve });
    });
  };

  const handleConfirmClose = (result: boolean) => {
    if (confirmState) {
      confirmState.resolve(result);
      setConfirmState(null);
    }
  };

  const handlePromptSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const val = promptValue.trim();
    if (!val) {
      setError('This field is required.');
      return;
    }
    if (promptState) {
      promptState.resolve(val);
      setPromptState(null);
    }
  };

  const handlePromptClose = () => {
    if (promptState) {
      promptState.resolve(null);
      setPromptState(null);
    }
  };

  return (
    <DialogContext.Provider value={{ confirm, prompt }}>
      {children}

      {/* Confirm Modal */}
      {confirmState && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center bg-background/80 backdrop-blur-sm">
          <div className="bg-background border border-border rounded-xl shadow-xl w-[400px] overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="p-4 border-b border-border flex justify-between items-center bg-muted/20">
              <h3 className="font-semibold text-foreground">{confirmState.options.title}</h3>
              <button onClick={() => handleConfirmClose(false)} className="text-muted-foreground hover:text-foreground">
                <X className="h-4 w-4" />
              </button>
            </div>
            {confirmState.options.message && (
              <div className="p-4 text-sm text-muted-foreground">
                {confirmState.options.message}
              </div>
            )}
            <div className="p-4 bg-muted/10 border-t border-border flex justify-end gap-2">
              <button onClick={() => handleConfirmClose(false)} className="px-4 py-2 text-sm font-medium hover:bg-accent rounded-md transition-colors">
                {confirmState.options.cancelText || 'Cancel'}
              </button>
              <button onClick={() => handleConfirmClose(true)} className="px-4 py-2 text-sm font-medium bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors">
                {confirmState.options.confirmText || 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Prompt Modal */}
      {promptState && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center bg-background/80 backdrop-blur-sm">
          <div className="bg-background border border-border rounded-xl shadow-xl w-[400px] overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="p-4 border-b border-border flex justify-between items-center bg-muted/20">
              <h3 className="font-semibold text-foreground">{promptState.options.title}</h3>
              <button onClick={handlePromptClose} className="text-muted-foreground hover:text-foreground">
                <X className="h-4 w-4" />
              </button>
            </div>
            <form onSubmit={handlePromptSubmit}>
              <div className="p-4">
                {promptState.options.message && (
                  <p className="text-sm text-muted-foreground mb-3">{promptState.options.message}</p>
                )}
                <input
                  type="text"
                  autoFocus
                  value={promptValue}
                  onChange={(e) => { setPromptValue(e.target.value); setError(''); }}
                  placeholder={promptState.options.placeholder || 'Enter value...'}
                  className={cn(
                    "w-full px-3 py-2 text-sm bg-background border rounded-md focus:outline-none focus:ring-2 focus:ring-primary/50 transition-shadow",
                    error ? "border-destructive" : "border-input"
                  )}
                />
                {error && <p className="text-xs text-destructive mt-1">{error}</p>}
              </div>
              <div className="p-4 bg-muted/10 border-t border-border flex justify-end gap-2">
                <button type="button" onClick={handlePromptClose} className="px-4 py-2 text-sm font-medium hover:bg-accent rounded-md transition-colors">
                  {promptState.options.cancelText || 'Cancel'}
                </button>
                <button type="submit" className="px-4 py-2 text-sm font-medium bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors">
                  {promptState.options.confirmText || 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DialogContext.Provider>
  );
}

export function useDialogs() {
  const context = useContext(DialogContext);
  if (!context) {
    throw new Error('useDialogs must be used within DialogProvider');
  }
  return context;
}
