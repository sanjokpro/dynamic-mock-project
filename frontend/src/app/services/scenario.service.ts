import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScenarioResponse, ScenarioRequest } from '../models/route.model';

@Injectable({
  providedIn: 'root'
})
export class ScenarioService {
  private apiUrl = 'http://localhost:8080/api/scenarios';

  constructor(private http: HttpClient) {}

  getScenarios(): Observable<ScenarioResponse[]> {
    return this.http.get<ScenarioResponse[]>(this.apiUrl);
  }

  getScenario(id: string): Observable<ScenarioResponse> {
    return this.http.get<ScenarioResponse>(`${this.apiUrl}/${id}`);
  }

  createScenario(scenario: ScenarioRequest): Observable<ScenarioResponse> {
    return this.http.post<ScenarioResponse>(this.apiUrl, scenario);
  }

  updateScenario(id: string, scenario: Partial<ScenarioRequest>): Observable<ScenarioResponse> {
    return this.http.put<ScenarioResponse>(`${this.apiUrl}/${id}`, scenario);
  }

  deleteScenario(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activateScenario(id: string): Observable<ScenarioResponse> {
    return this.http.post<ScenarioResponse>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivateScenario(id: string): Observable<ScenarioResponse> {
    return this.http.post<ScenarioResponse>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  resetScenario(id: string): Observable<ScenarioResponse> {
    return this.http.post<ScenarioResponse>(`${this.apiUrl}/${id}/reset`, {});
  }

  getCurrentState(id: string): Observable<{ scenarioName: string; currentState: string }> {
    return this.http.get<{ scenarioName: string; currentState: string }>(`${this.apiUrl}/${id}/state`);
  }

  triggerTransition(id: string, context?: any): Observable<{ scenarioName: string; previousState: string; currentState: string }> {
    return this.http.post<{ scenarioName: string; previousState: string; currentState: string }>(
      `${this.apiUrl}/${id}/transition`, 
      context || {}
    );
  }
}

