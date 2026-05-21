import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { RouteListComponent } from './route-list.component';
import { RouteService } from '../../services/route.service';
import { RouteResponse } from '../../models/route.model';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('RouteListComponent', () => {
  let component: RouteListComponent;
  let fixture: ComponentFixture<RouteListComponent>;
  let routeService: jasmine.SpyObj<RouteService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockRoutes: RouteResponse[] = [
    { id: '1', path: '/users', method: 'GET', active: true, version: 1 },
    { id: '2', path: '/posts', method: 'POST', active: false, version: 2 },
    { id: '3', path: '/comments/{id}', method: 'DELETE', active: true, version: 1 }
  ];

  beforeEach(async () => {
    const routeServiceSpy = jasmine.createSpyObj('RouteService', [
      'getRoutes', 'activateRoute', 'deactivateRoute', 'deleteRoute'
    ]);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [
        RouteListComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule
      ],
      providers: [
        { provide: RouteService, useValue: routeServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    routeService = TestBed.inject(RouteService) as jasmine.SpyObj<RouteService>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
  });

  beforeEach(() => {
    routeService.getRoutes.and.returnValue(of(mockRoutes));
    fixture = TestBed.createComponent(RouteListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load routes on init', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(routeService.getRoutes).toHaveBeenCalled();
    expect(component.routes).toEqual(mockRoutes);
    expect(component.loading).toBeFalse();
  }));

  it('should display correct number of routes', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(component.routes.length).toBe(3);
  }));

  it('should return correct method color', () => {
    expect(component.getMethodColor('GET')).toBe('primary');
    expect(component.getMethodColor('POST')).toBe('accent');
    expect(component.getMethodColor('DELETE')).toBe('warn');
    expect(component.getMethodColor('PUT')).toBe('accent');
    expect(component.getMethodColor('UNKNOWN')).toBe('primary');
  });

  describe('toggleActive', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should deactivate an active route', fakeAsync(() => {
      const activeRoute = mockRoutes[0];
      const deactivatedRoute = { ...activeRoute, active: false };
      routeService.deactivateRoute.and.returnValue(of(deactivatedRoute));

      component.toggleActive(activeRoute);
      tick();

      expect(routeService.deactivateRoute).toHaveBeenCalledWith('1');
      expect(snackBar.open).toHaveBeenCalledWith('Route deactivated', 'Close', { duration: 2000 });
    }));

    it('should activate an inactive route', fakeAsync(() => {
      const inactiveRoute = mockRoutes[1];
      const activatedRoute = { ...inactiveRoute, active: true };
      routeService.activateRoute.and.returnValue(of(activatedRoute));

      component.toggleActive(inactiveRoute);
      tick();

      expect(routeService.activateRoute).toHaveBeenCalledWith('2');
      expect(snackBar.open).toHaveBeenCalledWith('Route activated', 'Close', { duration: 2000 });
    }));
  });

  describe('deleteRoute', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should delete route after confirmation', fakeAsync(() => {
      spyOn(window, 'confirm').and.returnValue(true);
      routeService.deleteRoute.and.returnValue(of(void 0));

      const routeToDelete = mockRoutes[0];
      component.deleteRoute(routeToDelete);
      tick();

      expect(routeService.deleteRoute).toHaveBeenCalledWith('1');
      expect(component.routes.find(r => r.id === '1')).toBeUndefined();
      expect(snackBar.open).toHaveBeenCalledWith('Route deleted', 'Close', { duration: 2000 });
    }));

    it('should not delete route if user cancels', fakeAsync(() => {
      spyOn(window, 'confirm').and.returnValue(false);

      const routeToDelete = mockRoutes[0];
      component.deleteRoute(routeToDelete);
      tick();

      expect(routeService.deleteRoute).not.toHaveBeenCalled();
    }));
  });

  describe('error handling', () => {
    it('should show error snackbar when loading fails', fakeAsync(() => {
      routeService.getRoutes.and.returnValue(throwError(() => new Error('Network error')));
      
      fixture.detectChanges();
      tick();

      expect(snackBar.open).toHaveBeenCalledWith('Error loading routes', 'Close', { duration: 3000 });
      expect(component.loading).toBeFalse();
    }));
  });
});

