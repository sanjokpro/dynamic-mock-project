import { Routes } from '@angular/router';
import { RouteListComponent } from './routes/route-list/route-list.component';
import { RouteFormComponent } from './routes/route-form/route-form.component';
import { RouteDetailComponent } from './routes/route-detail/route-detail.component';

export const routes: Routes = [
  { path: '', redirectTo: '/routes', pathMatch: 'full' },
  { path: 'routes', component: RouteListComponent },
  { path: 'routes/new', component: RouteFormComponent },
  { path: 'routes/:id', component: RouteDetailComponent },
  { path: 'routes/:id/edit', component: RouteFormComponent },
];
