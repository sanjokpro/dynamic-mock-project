import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteResponse, CreateRouteRequest, UpdateRouteRequest } from '../models/route.model';

@Injectable({
  providedIn: 'root'
})
export class RouteService {
  private apiUrl = 'http://localhost:8080/api/routes';

  constructor(private http: HttpClient) {}

  getRoutes(): Observable<RouteResponse[]> {
    return this.http.get<RouteResponse[]>(this.apiUrl);
  }

  getRoute(id: string): Observable<RouteResponse> {
    return this.http.get<RouteResponse>(`${this.apiUrl}/${id}`);
  }

  createRoute(route: CreateRouteRequest): Observable<RouteResponse> {
    return this.http.post<RouteResponse>(this.apiUrl, route);
  }

  updateRoute(id: string, route: UpdateRouteRequest): Observable<RouteResponse> {
    return this.http.put<RouteResponse>(`${this.apiUrl}/${id}`, route);
  }

  deleteRoute(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activateRoute(id: string): Observable<RouteResponse> {
    return this.http.post<RouteResponse>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivateRoute(id: string): Observable<RouteResponse> {
    return this.http.post<RouteResponse>(`${this.apiUrl}/${id}/deactivate`, {});
  }
}
