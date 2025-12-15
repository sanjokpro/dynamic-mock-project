import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface GraphQLResolver {
  operationType: 'QUERY' | 'MUTATION' | 'SUBSCRIPTION';
  fieldName: string;
  responseTemplate: string;
  script?: string;
  scriptLanguage?: string;
  delayMs?: number;
  matchers?: Record<string, any>;
}

export interface GraphQLEndpoint {
  id?: string;
  name: string;
  description?: string;
  schema: string;
  resolvers: GraphQLResolver[];
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface GraphQLQueryRequest {
  query: string;
  operationName?: string;
  variables?: Record<string, any>;
}

@Injectable({
  providedIn: 'root'
})
export class GraphQLService {
  private apiUrl = `${environment.apiUrl}/graphql/endpoints`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<GraphQLEndpoint[]> {
    return this.http.get<GraphQLEndpoint[]>(this.apiUrl);
  }

  getById(id: string): Observable<GraphQLEndpoint> {
    return this.http.get<GraphQLEndpoint>(`${this.apiUrl}/${id}`);
  }

  create(endpoint: GraphQLEndpoint): Observable<GraphQLEndpoint> {
    return this.http.post<GraphQLEndpoint>(this.apiUrl, endpoint);
  }

  update(id: string, endpoint: GraphQLEndpoint): Observable<GraphQLEndpoint> {
    return this.http.put<GraphQLEndpoint>(`${this.apiUrl}/${id}`, endpoint);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activate(id: string): Observable<GraphQLEndpoint> {
    return this.http.post<GraphQLEndpoint>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<GraphQLEndpoint> {
    return this.http.post<GraphQLEndpoint>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  executeQuery(id: string, request: GraphQLQueryRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/execute`, request);
  }
}

