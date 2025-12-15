import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

declare var monaco: any;

@Component({
  selector: 'app-monaco-editor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="editor-container" [style.height]="height">
      <div #editorEl class="editor-inner"></div>
      <div *ngIf="loading" class="editor-loading">Loading editor...</div>
    </div>
  `,
  styles: [`
    .editor-container {
      position: relative;
      width: 100%;
      min-width: 0;
      border: 1px solid #ddd;
      border-radius: 4px;
      overflow: hidden;
      background: #1e1e1e;
    }
    .editor-inner {
      width: 100%;
      height: 100%;
    }
    .editor-loading {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f5f5;
      color: #666;
      font-size: 14px;
    }
  `]
})
export class MonacoEditorComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {
  @ViewChild('editorEl', { static: false }) editorEl!: ElementRef;
  @Input() value: string = '';
  @Input() language: string = 'javascript';
  @Input() theme: string = 'vs-dark';
  @Input() readOnly: boolean = false;
  @Input() height: string = '300px';
  @Output() valueChange = new EventEmitter<string>();

  private editor: any;
  private _value: string = '';
  private isInitialized = false;
  loading = true;

  ngOnInit() {
    this._value = this.value;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.isInitialized && this.editor) {
      if (changes['value'] && changes['value'].currentValue !== this._value) {
        if (this.editor.getValue() !== changes['value'].currentValue) {
          this.setValue(changes['value'].currentValue);
        }
      }
      if (changes['language']) {
        const langMap: { [key: string]: string } = {
          'js': 'javascript',
          'ts': 'typescript',
          'py': 'python'
        };
        const newLang = langMap[changes['language'].currentValue] || changes['language'].currentValue;
        monaco.editor.setModelLanguage(this.editor.getModel(), newLang);
      }
      if (changes['theme']) {
        monaco.editor.setTheme(changes['theme'].currentValue);
      }
    }
  }

  ngAfterViewInit() {
    this.loadMonaco().then(() => {
      this.initEditor();
    }).catch(err => {
      console.error('Failed to load Monaco:', err);
      this.loading = false;
    });
  }

  ngOnDestroy() {
    if (this.editor) {
      this.editor.dispose();
    }
  }

  private loadMonaco(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof monaco !== 'undefined') {
        resolve();
        return;
      }

      // Load from CDN
      const script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs/loader.js';
      script.onload = () => {
        const win = window as any;
        win.require.config({ 
          paths: { vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs' },
          'vs/nls': { availableLanguages: { '*': '' } }
        });
        
        // Configure worker paths for syntax highlighting
        win.MonacoEnvironment = {
          getWorkerUrl: function(workerId: string, label: string) {
            const base = 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/';
            
            // Return specific workers for different languages
            if (label === 'json') {
              return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
                self.MonacoEnvironment = { baseUrl: '${base}' };
                importScripts('${base}vs/language/json/json.worker.js');
              `)}`;
            }
            if (label === 'css' || label === 'scss' || label === 'less') {
              return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
                self.MonacoEnvironment = { baseUrl: '${base}' };
                importScripts('${base}vs/language/css/css.worker.js');
              `)}`;
            }
            if (label === 'html' || label === 'handlebars' || label === 'razor') {
              return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
                self.MonacoEnvironment = { baseUrl: '${base}' };
                importScripts('${base}vs/language/html/html.worker.js');
              `)}`;
            }
            if (label === 'typescript' || label === 'javascript') {
              return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
                self.MonacoEnvironment = { baseUrl: '${base}' };
                importScripts('${base}vs/language/typescript/ts.worker.js');
              `)}`;
            }
            // Default editor worker
            return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
              self.MonacoEnvironment = { baseUrl: '${base}' };
              importScripts('${base}vs/editor/editor.worker.js');
            `)}`;
          }
        };
        
        win.require(['vs/editor/editor.main'], () => {
          resolve();
        });
      };
      script.onerror = reject;
      document.body.appendChild(script);
    });
  }

  private initEditor() {
    if (!this.editorEl) {
      return;
    }

    // Map common language names
    const langMap: { [key: string]: string } = {
      'js': 'javascript',
      'ts': 'typescript',
      'py': 'python'
    };
    const mappedLang = langMap[this.language] || this.language;

    this.editor = monaco.editor.create(this.editorEl.nativeElement, {
      value: this._value,
      language: mappedLang,
      theme: this.theme,
      readOnly: this.readOnly,
      automaticLayout: true,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      fontSize: 13,
      fontFamily: "Consolas, 'Courier New', monospace",
      lineNumbers: 'on',
      roundedSelection: true,
      cursorStyle: 'line',
      wordWrap: 'on',
      padding: { top: 8, bottom: 8 },
      scrollbar: {
        vertical: 'auto',
        horizontal: 'auto'
      },
      renderLineHighlight: 'all',
      selectOnLineNumbers: true
    });

    this.editor.onDidChangeModelContent(() => {
      this._value = this.editor.getValue();
      this.valueChange.emit(this._value);
    });

    this.isInitialized = true;
    this.loading = false;
  }

  getValue(): string {
    return this._value;
  }

  setValue(value: string) {
    this._value = value;
    if (this.editor) {
      this.editor.setValue(value);
    }
  }
}
