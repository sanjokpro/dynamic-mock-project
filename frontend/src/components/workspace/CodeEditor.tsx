'use client';

import React, { useCallback } from 'react';
import Editor, { BeforeMount } from '@monaco-editor/react';
import { useTheme } from '@/context/ThemeContext';

interface CodeEditorProps {
  value: string;
  language?: string;
  onChange?: (value: string | undefined) => void;
  readOnly?: boolean;
  height?: string;
  enableValidation?: boolean;
}

export function CodeEditor({ 
  value, 
  language = 'json', 
  onChange, 
  readOnly = false,
  height = '100%',
  enableValidation = true
}: CodeEditorProps) {
  const { theme } = useTheme();
  const monacoTheme = theme === 'dark' ? 'vs-dark' : 'light';

  const handleBeforeMount: BeforeMount = useCallback((monaco) => {
    if (!enableValidation) return;

    // Enable JavaScript/TypeScript validation diagnostics
    if (language === 'javascript' || language === 'typescript') {
      monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
        noSemanticValidation: false,
        noSyntaxValidation: false,
      });
      monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
        target: monaco.languages.typescript.ScriptTarget.ES2020,
        allowNonTsExtensions: true,
        checkJs: true,
        strict: false,
      });
    }

    // Enable JSON schema validation
    if (language === 'json') {
      monaco.languages.json?.jsonDefaults?.setDiagnosticsOptions?.({
        validate: true,
        allowComments: true,
        enableSchemaRequest: false,
      });
    }
  }, [language, enableValidation]);

  return (
    <Editor
      height={height}
      language={language}
      theme={monacoTheme}
      value={value}
      onChange={onChange}
      beforeMount={handleBeforeMount}
      options={{
        readOnly,
        minimap: { enabled: false },
        fontSize: 14,
        fontFamily: "var(--font-geist-mono)",
        scrollBeyondLastLine: false,
        automaticLayout: true,
        padding: { top: 10, bottom: 10 },
        quickSuggestions: language === 'javascript',
        wordBasedSuggestions: language === 'javascript' ? 'currentDocument' : 'off',
      }}
    />
  );
}
