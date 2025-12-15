import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MonacoEditorComponent } from '../../shared/monaco-editor/monaco-editor.component';
import { RouteService } from '../../services/route.service';
import { CreateRouteRequest, UpdateRouteRequest } from '../../models/route.model';

@Component({
  selector: 'app-route-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTabsModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatDividerModule,
    MatTooltipModule,
    MonacoEditorComponent
  ],
  template: `
    <div class="container">
      <div class="form-header">
        <h1>{{ isEditMode ? 'Edit Mock Stub' : 'New Mock Stub' }}</h1>
        <div class="actions">
          <button mat-button type="button" routerLink="/routes">Cancel</button>
          <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="routeForm.invalid">
            <mat-icon>save</mat-icon> Save Stub
          </button>
        </div>
      </div>

      <mat-card class="form-card">
        <form [formGroup]="routeForm">
          <!-- Core Settings -->
          <div class="section core-settings">
            <div class="form-row">
              <mat-form-field appearance="outline" class="method-select">
                <mat-label>Method</mat-label>
                <mat-select formControlName="method">
                  <mat-option value="GET">GET</mat-option>
                  <mat-option value="POST">POST</mat-option>
                  <mat-option value="PUT">PUT</mat-option>
                  <mat-option value="DELETE">DELETE</mat-option>
                  <mat-option value="PATCH">PATCH</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline" class="path-input">
                <mat-label>URL Path</mat-label>
                <input matInput formControlName="path" [placeholder]="pathPlaceholder">
                <mat-hint>Supports path parameters</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline" class="status-input">
                <mat-label>Status</mat-label>
                <input matInput type="number" formControlName="responseStatus">
              </mat-form-field>
              
              <mat-form-field appearance="outline" class="delay-input">
                 <mat-label>Delay (ms)</mat-label>
                 <input matInput type="number" formControlName="delayMs">
              </mat-form-field>
            </div>
          </div>

          <mat-divider></mat-divider>

          <mat-tab-group animationDuration="0ms">
            <mat-tab label="Response Body">
              <div class="tab-content">
                <div class="editor-wrapper">
                  <app-monaco-editor
                    [value]="routeForm.get('responseTemplate')?.value || ''"
                    language="json"
                    [height]="'500px'"
                    (valueChange)="routeForm.patchValue({responseTemplate: $event})">
                  </app-monaco-editor>
                </div>
                <div class="help-text">
                  <small>Supports Handlebars syntax for dynamic responses</small>
                </div>
              </div>
            </mat-tab>

            <mat-tab label="Headers">
              <div class="tab-content">
                <div formArrayName="responseHeaders">
                  <div *ngFor="let header of responseHeadersArray.controls; let i = index" [formGroupName]="i" class="row-item">
                    <mat-form-field appearance="outline" class="dense">
                      <mat-label>Header</mat-label>
                      <input matInput formControlName="key" placeholder="Content-Type">
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="dense flex-grow">
                      <mat-label>Value</mat-label>
                      <input matInput formControlName="value">
                    </mat-form-field>
                    <button mat-icon-button color="warn" (click)="removeHeader(i)">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                </div>
                <button mat-stroked-button (click)="addHeader()">
                  <mat-icon>add</mat-icon> Add Header
                </button>
                
                <div class="quick-headers">
                   <span>Quick Add:</span>
                   <button mat-button (click)="addCommonHeader('Content-Type', 'application/json')">JSON</button>
                   <button mat-button (click)="addCommonHeader('Content-Type', 'application/xml')">XML</button>
                </div>
              </div>
            </mat-tab>

            <mat-tab label="Request Matching">
               <div class="tab-content">
                  <mat-accordion>
                    <mat-expansion-panel>
                      <mat-expansion-panel-header>Header Matchers</mat-expansion-panel-header>
                      <div formArrayName="headerMatchers">
                        <div *ngFor="let m of headerMatchersArray.controls; let i = index" [formGroupName]="i" class="row-item">
                           <mat-form-field appearance="outline" class="dense"><mat-label>Header</mat-label><input matInput formControlName="key"></mat-form-field>
                           <mat-form-field appearance="outline" class="dense flex-grow"><mat-label>Value Pattern</mat-label><input matInput formControlName="value"></mat-form-field>
                           <button mat-icon-button color="warn" (click)="removeHeaderMatcher(i)"><mat-icon>delete</mat-icon></button>
                        </div>
                        <button mat-button (click)="addHeaderMatcher()"><mat-icon>add</mat-icon> Add Matcher</button>
                      </div>
                    </mat-expansion-panel>
                    
                    <mat-expansion-panel>
                      <mat-expansion-panel-header>Query Param Matchers</mat-expansion-panel-header>
                      <div formArrayName="queryMatchers">
                        <div *ngFor="let m of queryMatchersArray.controls; let i = index" [formGroupName]="i" class="row-item">
                           <mat-form-field appearance="outline" class="dense"><mat-label>Param</mat-label><input matInput formControlName="key"></mat-form-field>
                           <mat-form-field appearance="outline" class="dense flex-grow"><mat-label>Value Pattern</mat-label><input matInput formControlName="value"></mat-form-field>
                           <button mat-icon-button color="warn" (click)="removeQueryMatcher(i)"><mat-icon>delete</mat-icon></button>
                        </div>
                        <button mat-button (click)="addQueryMatcher()"><mat-icon>add</mat-icon> Add Matcher</button>
                      </div>
                    </mat-expansion-panel>

                    <mat-expansion-panel>
                       <mat-expansion-panel-header>Body Matcher</mat-expansion-panel-header>
                       <mat-form-field appearance="outline">
                         <mat-label>Match Type</mat-label>
                         <mat-select formControlName="bodyMatchType">
                           <mat-option value="none">None</mat-option>
                           <mat-option value="equals">Exact Match</mat-option>
                           <mat-option value="contains">Contains</mat-option>
                           <mat-option value="jsonPath">JSON Path</mat-option>
                           <mat-option value="regex">Regex</mat-option>
                         </mat-select>
                       </mat-form-field>
                       <mat-form-field appearance="outline" class="full-width" *ngIf="routeForm.get('bodyMatchType')?.value !== 'none'">
                         <mat-label>Pattern</mat-label>
                         <textarea matInput formControlName="bodyMatchPattern" rows="3"></textarea>
                       </mat-form-field>
                    </mat-expansion-panel>
                  </mat-accordion>
               </div>
            </mat-tab>

            <mat-tab label="Scripts">
              <div class="tab-content">
                 <mat-form-field appearance="outline">
                   <mat-label>Script Language</mat-label>
                   <mat-select formControlName="scriptLanguage">
                     <mat-option value="js">JavaScript</mat-option>
                     <mat-option value="groovy">Groovy</mat-option>
                   </mat-select>
                 </mat-form-field>
                 
                 <div class="two-col">
                    <div class="script-col">
                      <h4>Pre-Request</h4>
                      <app-monaco-editor
                        [value]="routeForm.get('preScript')?.value || ''"
                        [language]="getMonacoLanguage()"
                        [height]="'300px'"
                        (valueChange)="routeForm.patchValue({preScript: $event})">
                      </app-monaco-editor>
                    </div>
                    <div class="script-col">
                      <h4>Post-Request</h4>
                      <app-monaco-editor
                        [value]="routeForm.get('postScript')?.value || ''"
                        [language]="getMonacoLanguage()"
                        [height]="'300px'"
                        (valueChange)="routeForm.patchValue({postScript: $event})">
                      </app-monaco-editor>
                    </div>
                 </div>
              </div>
            </mat-tab>
          </mat-tab-group>
        </form>
      </mat-card>
    </div>
  `,
  styles: [`
    .container { max-width: 1200px; margin: 0 auto; padding-bottom: 40px; }
    .form-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .form-header h1 { margin: 0; font-weight: 400; font-size: 24px; }
    .actions { display: flex; gap: 10px; }
    
    .form-card { padding: 0; overflow: hidden; }
    .section { padding: 20px; }
    .form-row { display: flex; gap: 15px; align-items: flex-start; flex-wrap: wrap; }
    .method-select { flex: 0 0 120px; }
    .path-input { flex: 1; min-width: 200px; }
    .status-input, .delay-input { flex: 0 0 100px; }
    
    .tab-content { padding: 20px; overflow: hidden; }
    .editor-wrapper { overflow: hidden; }
    .help-text { margin-top: 8px; color: #666; }
    
    .row-item { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
    .dense { margin-bottom: -1.25em; }
    .flex-grow { flex: 1; min-width: 0; }
    
    .quick-headers { margin-top: 15px; display: flex; align-items: center; gap: 10px; color: #666; font-size: 13px; }
    
    .two-col { display: flex; gap: 20px; width: 100%; overflow: hidden; }
    .script-col { flex: 1; min-width: 0; overflow: hidden; }
    .script-col h4 { margin: 0 0 10px 0; font-weight: 500; }
    .full-width { width: 100%; }
    
    /* Material Overrides for cleaner look */
    ::ng-deep .mat-mdc-form-field-subscript-wrapper { font-size: 11px; }
    ::ng-deep .mat-mdc-tab-body-content { overflow: hidden !important; }
  `]
})
export class RouteFormComponent implements OnInit, AfterViewInit {
  @ViewChild('responseTemplateEditor') responseTemplateEditor!: MonacoEditorComponent;
  @ViewChild('preScriptEditor') preScriptEditor!: MonacoEditorComponent;
  @ViewChild('postScriptEditor') postScriptEditor!: MonacoEditorComponent;

  routeForm: FormGroup;
  isEditMode = false;
  routeId: string | null = null;
  
  // Use a property for placeholder to avoid template parsing issues with curly braces
  pathPlaceholder = '/api/resource/{id}';
  
  constructor(
    private fb: FormBuilder,
    private routeService: RouteService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.routeForm = this.fb.group({
      method: ['GET', Validators.required],
      path: ['', Validators.required],
      responseTemplate: [''],
      responseStatus: [200],
      delayMs: [0],
      version: [1],
      preScript: [''],
      postScript: [''],
      scriptLanguage: ['js'],
      responseHeaders: this.fb.array([]),
      headerMatchers: this.fb.array([]),
      queryMatchers: this.fb.array([]),
      bodyMatchType: ['none'],
      bodyMatchPattern: ['']
    });
  }

  // Form Array Getters
  get responseHeadersArray(): FormArray { return this.routeForm.get('responseHeaders') as FormArray; }
  get headerMatchersArray(): FormArray { return this.routeForm.get('headerMatchers') as FormArray; }
  get queryMatchersArray(): FormArray { return this.routeForm.get('queryMatchers') as FormArray; }

  // Map form language to Monaco language ID
  getMonacoLanguage(): string {
    const lang = this.routeForm.get('scriptLanguage')?.value;
    const langMap: { [key: string]: string } = {
      'js': 'javascript',
      'javascript': 'javascript',
      'groovy': 'groovy',  // Monaco supports groovy
      'python': 'python'
    };
    return langMap[lang] || 'javascript';
  }

  // Response Headers Methods
  addHeader(): void {
    this.responseHeadersArray.push(this.fb.group({ key: [''], value: [''] }));
  }
  removeHeader(index: number): void {
    this.responseHeadersArray.removeAt(index);
  }
  addCommonHeader(key: string, value: string): void {
    const exists = this.responseHeadersArray.controls.some(c => c.get('key')?.value === key);
    if (!exists) this.responseHeadersArray.push(this.fb.group({ key, value }));
  }

  // Matchers Methods
  addHeaderMatcher(): void { this.headerMatchersArray.push(this.fb.group({ key: [''], value: [''] })); }
  removeHeaderMatcher(index: number): void { this.headerMatchersArray.removeAt(index); }
  addQueryMatcher(): void { this.queryMatchersArray.push(this.fb.group({ key: [''], value: [''] })); }
  removeQueryMatcher(index: number): void { this.queryMatchersArray.removeAt(index); }

  // Helper to convert FormArray to object
  private formArrayToObject(formArray: FormArray): { [key: string]: string } {
    const result: { [key: string]: string } = {};
    formArray.controls.forEach(control => {
      const key = control.get('key')?.value;
      const value = control.get('value')?.value;
      if (key && key.trim()) result[key] = value || '';
    });
    return result;
  }

  // Helper to load object into FormArray
  private objectToFormArray(obj: { [key: string]: string } | undefined, formArray: FormArray): void {
    formArray.clear();
    if (obj) {
      Object.entries(obj).forEach(([key, value]) => {
        formArray.push(this.fb.group({ key, value }));
      });
    }
  }

  ngOnInit() {
    this.routeId = this.route.snapshot.paramMap.get('id');
    if (this.routeId) {
      this.isEditMode = true;
      this.loadRoute();
    }
  }

  ngAfterViewInit() {}

  loadRoute() {
    if (!this.routeId) return;
    this.routeService.getRoute(this.routeId).subscribe({
      next: (route) => {
        this.routeForm.patchValue({
          method: route.method,
          path: route.path,
          responseTemplate: route.responseTemplate || '',
          responseStatus: route.responseStatus || 200,
          delayMs: route.delayMs || 0,
          version: route.version || 1,
          preScript: route.preScript || '',
          postScript: route.postScript || '',
          scriptLanguage: route.scriptLanguage || 'js',
          bodyMatchType: route.matchers?.['bodyMatchType'] || 'none',
          bodyMatchPattern: route.matchers?.['bodyMatchPattern'] || ''
        });
        this.objectToFormArray(route.responseHeaders, this.responseHeadersArray);
        if (route.matchers) {
          this.objectToFormArray(route.matchers['headers'] as { [key: string]: string }, this.headerMatchersArray);
          this.objectToFormArray(route.matchers['queryParams'] as { [key: string]: string }, this.queryMatchersArray);
        }
      },
      error: (error) => {
        console.error('Error loading route:', error);
        this.snackBar.open('Error loading route', 'Close', { duration: 3000 });
        this.router.navigate(['/routes']);
      }
    });
  }

  onSubmit() {
    if (this.routeForm.invalid) return;

    const formValue = this.routeForm.value;
    const responseHeaders = this.formArrayToObject(this.responseHeadersArray);
    const hasResponseHeaders = Object.keys(responseHeaders).length > 0;

    const matchers: { [key: string]: any } = {};
    const headerMatchers = this.formArrayToObject(this.headerMatchersArray);
    const queryMatchers = this.formArrayToObject(this.queryMatchersArray);
    
    if (Object.keys(headerMatchers).length > 0) matchers['headers'] = headerMatchers;
    if (Object.keys(queryMatchers).length > 0) matchers['queryParams'] = queryMatchers;
    if (formValue.bodyMatchType !== 'none' && formValue.bodyMatchPattern) {
      matchers['bodyMatchType'] = formValue.bodyMatchType;
      matchers['bodyMatchPattern'] = formValue.bodyMatchPattern;
    }
    const hasMatchers = Object.keys(matchers).length > 0;

    const requestData: any = {
      method: formValue.method,
      path: formValue.path,
      responseTemplate: formValue.responseTemplate || undefined,
      responseStatus: formValue.responseStatus || undefined,
      responseHeaders: hasResponseHeaders ? responseHeaders : undefined,
      matchers: hasMatchers ? matchers : undefined,
      delayMs: formValue.delayMs || undefined,
      version: formValue.version || undefined,
      preScript: formValue.preScript || undefined,
      postScript: formValue.postScript || undefined,
      scriptLanguage: formValue.scriptLanguage || undefined
    };

    const request = this.isEditMode && this.routeId 
      ? this.routeService.updateRoute(this.routeId, requestData)
      : this.routeService.createRoute(requestData);

    request.subscribe({
      next: () => {
        this.snackBar.open(`Route ${this.isEditMode ? 'updated' : 'created'} successfully`, 'Close', { duration: 2000 });
        this.router.navigate(['/routes']);
      },
      error: (error) => {
        console.error('Error saving route:', error);
        this.snackBar.open('Error saving route', 'Close', { duration: 3000 });
      }
    });
  }
}
