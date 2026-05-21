import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary" class="app-toolbar">
      <div class="toolbar-brand" routerLink="/">
        <mat-icon>api</mat-icon>
        <span>Dynamic Mock</span>
      </div>
      <span class="spacer"></span>
      <nav class="toolbar-nav">
        <button mat-button routerLink="/routes">
          <mat-icon>list_alt</mat-icon>
          <span>Routes</span>
        </button>
        <button mat-button routerLink="/routes/new">
          <mat-icon>add</mat-icon>
          <span>New Route</span>
        </button>
      </nav>
    </mat-toolbar>
    <main class="main-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 2px 4px rgba(0,0,0,.1);
    }
    
    .toolbar-brand {
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 500;
      font-size: 20px;
      cursor: pointer;
      
      mat-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
      }
    }
    
    .spacer {
      flex: 1 1 auto;
    }
    
    .toolbar-nav {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .main-content {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      background-color: #f5f5f5;
    }
    
    button mat-icon {
      margin-right: 4px;
    }
  `]
})
export class AppComponent {
  title = 'Dynamic Mock API Server';
}
