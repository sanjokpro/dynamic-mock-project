import { Component, OnInit } from '@angular/core';
import { CommonModule, KeyValuePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatListModule } from '@angular/material/list';
import { MonacoEditorComponent } from '../../shared/monaco-editor/monaco-editor.component';
import { RouteService } from '../../services/route.service';
import { RouteResponse } from '../../models/route.model';

@Component({
  selector: 'app-route-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatExpansionModule,
    MatListModule,
    KeyValuePipe,
    MonacoEditorComponent
  ],
  template: `
    <div class="container" *ngIf="route">
      <div class="detail-header">
        <button mat-button routerLink="/routes">
          <mat-icon>arrow_back</mat-icon> Back to Routes
        </button>
        <div class="actions">
          <button mat-raised-button [color]="route.active ? 'warn' : 'primary'" (click)="toggleActive()">
             <mat-icon>{{ route.active ? 'pause' : 'play_arrow' }}</mat-icon>
             {{ route.active ? 'Stop Stub' : 'Start Stub' }}
          </button>
          <button mat-raised-button color="accent" [routerLink]="['/routes', route.id, 'edit']">
            <mat-icon>edit</mat-icon> Edit
          </button>
          <button mat-icon-button color="warn" (click)="deleteRoute()">
            <mat-icon>delete</mat-icon>
          </button>
        </div>
      </div>

      <mat-card class="route-card">
        <div class="route-overview">
          <span class="method-badge" [ngClass]="route.method.toLowerCase()">{{ route.method }}</span>
          <code class="path-display">{{ route.path }}</code>
          <span class="spacer"></span>
          <span class="status-badge" [class.active]="route.active">
            {{ route.active ? 'Active' : 'Stopped' }}
          </span>
        </div>
        
        <div class="meta-info">
           <span><strong>Status:</strong> {{ route.responseStatus }}</span>
           <span><strong>Delay:</strong> {{ route.delayMs }}ms</span>
           <span *ngIf="route.version"><strong>Version:</strong> {{ route.version }}</span>
        </div>

        <mat-divider></mat-divider>

        <mat-tab-group animationDuration="0ms">
          <mat-tab label="Response Body">
            <div class="tab-content">
              <app-monaco-editor
                [value]="route.responseTemplate || ''"
                language="json"
                [readOnly]="true"
                [height]="'400px'">
              </app-monaco-editor>
            </div>
          </mat-tab>

          <mat-tab label="Headers" *ngIf="hasResponseHeaders()">
            <div class="tab-content">
              <table class="simple-table">
                <thead>
                  <tr>
                    <th>Header</th>
                    <th>Value</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let header of route.responseHeaders | keyvalue">
                    <td><code>{{ header.key }}</code></td>
                    <td>{{ header.value }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </mat-tab>

          <mat-tab label="Matchers" *ngIf="hasMatchers()">
            <div class="tab-content">
               <div *ngIf="route.matchers?.['headers']" class="matcher-group">
                 <h3>Header Matchers</h3>
                 <div *ngFor="let m of getMatcherEntries('headers')" class="matcher-item">
                   <code>{{ m.key }}</code> ≈ <code>{{ m.value }}</code>
                 </div>
               </div>
               <div *ngIf="route.matchers?.['queryParams']" class="matcher-group">
                 <h3>Query Param Matchers</h3>
                 <div *ngFor="let m of getMatcherEntries('queryParams')" class="matcher-item">
                   <code>{{ m.key }}</code> ≈ <code>{{ m.value }}</code>
                 </div>
               </div>
               <div *ngIf="route.matchers?.['bodyMatchType'] && route.matchers?.['bodyMatchType'] !== 'none'" class="matcher-group">
                 <h3>Body Matcher ({{ route.matchers?.['bodyMatchType'] }})</h3>
                 <pre>{{ route.matchers?.['bodyMatchPattern'] }}</pre>
               </div>
            </div>
          </mat-tab>

          <mat-tab label="Scripts" *ngIf="route.preScript || route.postScript">
             <div class="tab-content two-col">
                <div class="script-box" *ngIf="route.preScript">
                  <h4>Pre-Request Script</h4>
                  <app-monaco-editor
                    [value]="route.preScript"
                    [language]="route.scriptLanguage || 'javascript'"
                    [readOnly]="true"
                    [height]="'300px'">
                  </app-monaco-editor>
                </div>
                <div class="script-box" *ngIf="route.postScript">
                  <h4>Post-Request Script</h4>
                  <app-monaco-editor
                    [value]="route.postScript"
                    [language]="route.scriptLanguage || 'javascript'"
                    [readOnly]="true"
                    [height]="'300px'">
                  </app-monaco-editor>
                </div>
             </div>
          </mat-tab>
        </mat-tab-group>

        <div class="footer-url">
          <span>Test Endpoint:</span>
          <code>http://localhost:8080/mock{{ route.path }}</code>
          <button mat-icon-button (click)="copyToClipboard()" matTooltip="Copy">
             <mat-icon>content_copy</mat-icon>
          </button>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1000px;
      margin: 0 auto;
    }
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .actions {
      display: flex;
      gap: 8px;
    }
    .route-card {
      padding: 0;
    }
    .route-overview {
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      background: #fff;
      border-bottom: 1px solid #eee;
    }
    .method-badge {
      font-weight: 700;
      text-transform: uppercase;
      padding: 4px 8px;
      border-radius: 4px;
      color: #fff;
      font-size: 14px;
      &.get { background-color: #61affe; }
      &.post { background-color: #49cc90; }
      &.put { background-color: #fca130; }
      &.delete { background-color: #f93e3e; }
      &.patch { background-color: #50e3c2; color: #333; }
    }
    .path-display {
      font-size: 16px;
      color: #333;
      background: #f0f0f0;
      padding: 4px 8px;
      border-radius: 4px;
    }
    .status-badge {
      font-size: 12px;
      font-weight: 500;
      padding: 2px 8px;
      border-radius: 12px;
      background: #eee;
      color: #666;
      &.active { background: #e6fffa; color: #00909e; }
    }
    .meta-info {
      padding: 10px 20px;
      display: flex;
      gap: 24px;
      background: #fafafa;
      font-size: 13px;
      color: #666;
    }
    .tab-content {
      padding: 0;
    }
    .simple-table {
      width: 100%;
      border-collapse: collapse;
      th { text-align: left; padding: 12px 20px; background: #fafafa; border-bottom: 1px solid #eee; font-size: 12px; text-transform: uppercase; color: #888; }
      td { padding: 12px 20px; border-bottom: 1px solid #eee; }
    }
    .matcher-group {
      padding: 20px;
      border-bottom: 1px solid #eee;
      h3 { font-size: 14px; margin: 0 0 10px 0; color: #333; }
    }
    .matcher-item {
      margin-bottom: 5px;
      font-size: 13px;
    }
    .two-col {
      display: flex;
      gap: 20px;
      padding: 20px;
      .script-box { flex: 1; h4 { margin-top: 0; } }
    }
    .footer-url {
      padding: 15px 20px;
      background: #f5f5f5;
      border-top: 1px solid #eee;
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 13px;
      color: #666;
      code { flex: 1; background: #fff; padding: 6px; border: 1px solid #ddd; border-radius: 4px; }
    }
  `]
})
export class RouteDetailComponent implements OnInit {
  route: RouteResponse | null = null;
  loading = true;

  constructor(
    private routeService: RouteService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    const id = this.activatedRoute.snapshot.paramMap.get('id');
    if (id) {
      this.loadRoute(id);
    }
  }

  loadRoute(id: string) {
    this.loading = true;
    this.routeService.getRoute(id).subscribe({
      next: (route) => {
        this.route = route;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading route:', error);
        this.snackBar.open('Error loading route', 'Close', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/routes']);
      }
    });
  }

  toggleActive() {
    if (!this.route) return;
    const action = this.route.active 
      ? this.routeService.deactivateRoute(this.route.id) 
      : this.routeService.activateRoute(this.route.id);
    
    action.subscribe({
      next: (updatedRoute) => {
        this.route = updatedRoute;
        this.snackBar.open(`Route ${updatedRoute.active ? 'activated' : 'deactivated'}`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error toggling route status:', error);
        this.snackBar.open('Error updating route status', 'Close', { duration: 3000 });
      }
    });
  }

  deleteRoute() {
    if (!this.route) return;
    if (confirm(`Are you sure you want to delete route ${this.route.method} ${this.route.path}?`)) {
      this.routeService.deleteRoute(this.route.id).subscribe({
        next: () => {
          this.snackBar.open('Route deleted', 'Close', { duration: 2000 });
          this.router.navigate(['/routes']);
        },
        error: (error) => {
          console.error('Error deleting route:', error);
          this.snackBar.open('Error deleting route', 'Close', { duration: 3000 });
        }
      });
    }
  }

  copyToClipboard() {
    if (!this.route) return;
    const url = `http://localhost:8080/mock${this.route.path}`;
    navigator.clipboard.writeText(url).then(() => {
      this.snackBar.open('URL copied', 'Close', { duration: 2000 });
    });
  }

  hasResponseHeaders(): boolean {
    return !!(this.route?.responseHeaders && Object.keys(this.route.responseHeaders).length > 0);
  }

  hasMatchers(): boolean {
    if (!this.route?.matchers) return false;
    return Object.keys(this.route.matchers).length > 0;
  }

  getMatcherEntries(type: string): { key: string; value: string }[] {
    const matchers = this.route?.matchers?.[type];
    if (!matchers || typeof matchers !== 'object') return [];
    return Object.entries(matchers).map(([key, value]) => ({ key, value: String(value) }));
  }
}
