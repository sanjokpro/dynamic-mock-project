import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouteService } from '../../services/route.service';
import { RouteResponse } from '../../models/route.model';

@Component({
  selector: 'app-route-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule
  ],
  template: `
    <div class="route-list-container">
      <div class="header-actions">
        <h1>Mock Routes</h1>
        <button mat-raised-button color="primary" routerLink="/routes/new">
          <mat-icon>add</mat-icon>
          Create New Stub
        </button>
      </div>

      <mat-card class="routes-card">
        <div *ngIf="loading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>

        <div *ngIf="!loading && routes.length === 0" class="empty-state">
          <mat-icon>inbox</mat-icon>
          <p>No routes found. Create your first mock route!</p>
        </div>

        <table mat-table [dataSource]="routes" *ngIf="!loading && routes.length > 0" class="routes-table">
          <!-- Method Column -->
          <ng-container matColumnDef="method">
            <th mat-header-cell *matHeaderCellDef> Method </th>
            <td mat-cell *matCellDef="let route">
              <span class="method-badge" [ngClass]="route.method.toLowerCase()">{{ route.method }}</span>
            </td>
          </ng-container>

          <!-- Path Column -->
          <ng-container matColumnDef="path">
            <th mat-header-cell *matHeaderCellDef> URL Pattern </th>
            <td mat-cell *matCellDef="let route">
              <code class="url-pattern">{{ route.path }}</code>
            </td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef> Status </th>
            <td mat-cell *matCellDef="let route">
              <span class="status-badge" [class.active]="route.active" [class.inactive]="!route.active">
                {{ route.active ? 'Active' : 'Disabled' }}
              </span>
            </td>
          </ng-container>

          <!-- Version Column -->
          <ng-container matColumnDef="version">
            <th mat-header-cell *matHeaderCellDef> Ver. </th>
            <td mat-cell *matCellDef="let route"> v{{ route.version || 1 }} </td>
          </ng-container>

          <!-- Actions Column -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef> </th>
            <td mat-cell *matCellDef="let route" class="actions-cell">
              <button mat-icon-button [routerLink]="['/routes', route.id, 'edit']" matTooltip="Edit">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button (click)="toggleActive(route)" [matTooltip]="route.active ? 'Disable' : 'Enable'">
                <mat-icon>{{ route.active ? 'pause' : 'play_arrow' }}</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="deleteRoute(route)" matTooltip="Delete">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card>
    </div>
  `,
  styles: [`
    .route-list-container {
      max-width: 1200px;
      margin: 0 auto;
    }

    .header-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 400;
      color: #333;
    }

    .routes-card {
      padding: 0;
      overflow: hidden;
    }

    .routes-table {
      width: 100%;
    }

    .mat-mdc-header-row {
      background-color: #f9f9f9;
    }

    .method-badge {
      font-weight: 700;
      text-transform: uppercase;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 12px;
      color: #fff;
      
      &.get { background-color: #61affe; }
      &.post { background-color: #49cc90; }
      &.put { background-color: #fca130; }
      &.delete { background-color: #f93e3e; }
      &.patch { background-color: #50e3c2; color: #333; }
    }

    .url-pattern {
      font-family: 'Source Code Pro', monospace;
      font-size: 14px;
      color: #333;
      background: transparent;
      border: none;
    }

    .status-badge {
      font-size: 12px;
      font-weight: 500;
      padding: 2px 8px;
      border-radius: 12px;
      
      &.active { background-color: #e6fffa; color: #00909e; }
      &.inactive { background-color: #fff5f5; color: #e53e3e; }
    }

    .actions-cell {
      text-align: right;
      white-space: nowrap;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 40px;
    }

    .empty-state {
      text-align: center;
      padding: 40px;
      color: #666;
      
      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        opacity: 0.5;
        margin-bottom: 10px;
      }
    }
  `]
})
export class RouteListComponent implements OnInit {
  routes: RouteResponse[] = [];
  loading = true;
  displayedColumns: string[] = ['method', 'path', 'status', 'version', 'actions'];

  constructor(
    private routeService: RouteService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadRoutes();
  }

  loadRoutes() {
    this.loading = true;
    this.routeService.getRoutes().subscribe({
      next: (routes) => {
        this.routes = routes;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading routes:', error);
        this.snackBar.open('Error loading routes', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  toggleActive(route: RouteResponse) {
    const action = route.active ? this.routeService.deactivateRoute(route.id) : this.routeService.activateRoute(route.id);
    action.subscribe({
      next: (updatedRoute) => {
        const index = this.routes.findIndex(r => r.id === route.id);
        if (index !== -1) {
          this.routes[index] = updatedRoute;
        }
        this.snackBar.open(`Route ${updatedRoute.active ? 'activated' : 'deactivated'}`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error toggling route status:', error);
        this.snackBar.open('Error updating route status', 'Close', { duration: 3000 });
      }
    });
  }

  deleteRoute(route: RouteResponse) {
    if (confirm(`Are you sure you want to delete route ${route.method} ${route.path}?`)) {
      this.routeService.deleteRoute(route.id).subscribe({
        next: () => {
          this.routes = this.routes.filter(r => r.id !== route.id);
          this.snackBar.open('Route deleted', 'Close', { duration: 2000 });
        },
        error: (error) => {
          console.error('Error deleting route:', error);
          this.snackBar.open('Error deleting route', 'Close', { duration: 3000 });
        }
      });
    }
  }
}
