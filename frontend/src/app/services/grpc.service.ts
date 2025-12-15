import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface GrpcMethod {
  methodName: string;
  methodType: 'UNARY' | 'SERVER_STREAMING' | 'CLIENT_STREAMING' | 'BIDI_STREAMING';
  responseTemplate: string;
  streamResponses?: string[];
  script?: string;
  scriptLanguage?: string;
  delayMs?: number;
  statusCode?: string;
  errorMessage?: string;
  matchers?: Record<string, any>;
}

export interface GrpcEndpoint {
  id?: string;
  name: string;
  description?: string;
  serviceName: string;
  protoSchema?: string;
  methods: GrpcMethod[];
  port?: number;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class GrpcService {
  private apiUrl = `${environment.apiUrl}/grpc/endpoints`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<GrpcEndpoint[]> {
    return this.http.get<GrpcEndpoint[]>(this.apiUrl);
  }

  getById(id: string): Observable<GrpcEndpoint> {
    return this.http.get<GrpcEndpoint>(`${this.apiUrl}/${id}`);
  }

  create(endpoint: GrpcEndpoint): Observable<GrpcEndpoint> {
    return this.http.post<GrpcEndpoint>(this.apiUrl, endpoint);
  }

  update(id: string, endpoint: GrpcEndpoint): Observable<GrpcEndpoint> {
    return this.http.put<GrpcEndpoint>(`${this.apiUrl}/${id}`, endpoint);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activate(id: string): Observable<GrpcEndpoint> {
    return this.http.post<GrpcEndpoint>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<GrpcEndpoint> {
    return this.http.post<GrpcEndpoint>(`${this.apiUrl}/${id}/deactivate`, {});
  }
}

