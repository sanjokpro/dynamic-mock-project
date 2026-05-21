import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouteService } from './route.service';
import { RouteResponse, CreateRouteRequest, UpdateRouteRequest } from '../models/route.model';

describe('RouteService', () => {
  let service: RouteService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api/routes';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RouteService]
    });
    service = TestBed.inject(RouteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getRoutes', () => {
    it('should fetch all routes', () => {
      const mockRoutes: RouteResponse[] = [
        { id: '1', path: '/users', method: 'GET', active: true },
        { id: '2', path: '/posts', method: 'POST', active: false }
      ];

      service.getRoutes().subscribe(routes => {
        expect(routes).toEqual(mockRoutes);
        expect(routes.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockRoutes);
    });
  });

  describe('getRoute', () => {
    it('should fetch a single route by id', () => {
      const mockRoute: RouteResponse = {
        id: '1',
        path: '/users/{id}',
        method: 'GET',
        responseTemplate: '{"id": "{{$randomUUID}}"}',
        responseStatus: 200,
        active: true
      };

      service.getRoute('1').subscribe(route => {
        expect(route).toEqual(mockRoute);
        expect(route.id).toBe('1');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockRoute);
    });
  });

  describe('createRoute', () => {
    it('should create a new route', () => {
      const createRequest: CreateRouteRequest = {
        path: '/new-route',
        method: 'POST',
        responseTemplate: '{"message": "created"}',
        responseStatus: 201
      };

      const mockResponse: RouteResponse = {
        id: 'new-id',
        ...createRequest,
        active: false
      };

      service.createRoute(createRequest).subscribe(route => {
        expect(route).toEqual(mockResponse);
        expect(route.id).toBe('new-id');
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockResponse);
    });
  });

  describe('updateRoute', () => {
    it('should update an existing route', () => {
      const updateRequest: UpdateRouteRequest = {
        path: '/updated-path',
        responseStatus: 204
      };

      const mockResponse: RouteResponse = {
        id: '1',
        path: '/updated-path',
        method: 'GET',
        responseStatus: 204,
        active: true
      };

      service.updateRoute('1', updateRequest).subscribe(route => {
        expect(route).toEqual(mockResponse);
        expect(route.path).toBe('/updated-path');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(mockResponse);
    });
  });

  describe('deleteRoute', () => {
    it('should delete a route', () => {
      service.deleteRoute('1').subscribe(() => {
        // Success - no error thrown
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('activateRoute', () => {
    it('should activate a route', () => {
      const mockResponse: RouteResponse = {
        id: '1',
        path: '/users',
        method: 'GET',
        active: true
      };

      service.activateRoute('1').subscribe(route => {
        expect(route.active).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/activate`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('deactivateRoute', () => {
    it('should deactivate a route', () => {
      const mockResponse: RouteResponse = {
        id: '1',
        path: '/users',
        method: 'GET',
        active: false
      };

      service.deactivateRoute('1').subscribe(route => {
        expect(route.active).toBe(false);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/deactivate`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });
});

