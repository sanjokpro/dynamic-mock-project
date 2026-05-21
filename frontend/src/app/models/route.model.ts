export interface CreateRouteRequest {
  path: string;
  method: string;
  matchers?: { [key: string]: any };
  responseTemplate?: string;
  responseStatus?: number;
  responseHeaders?: { [key: string]: string };
  preScript?: string;
  postScript?: string;
  scriptLanguage?: string;
  delayMs?: number;
  version?: number;
  scenarioName?: string;
}

export interface UpdateRouteRequest {
  path?: string;
  method?: string;
  matchers?: { [key: string]: any };
  responseTemplate?: string;
  responseStatus?: number;
  responseHeaders?: { [key: string]: string };
  preScript?: string;
  postScript?: string;
  scriptLanguage?: string;
  delayMs?: number;
  version?: number;
  active?: boolean;
  scenarioName?: string;
}

export interface RouteResponse {
  id: string;
  path: string;
  method: string;
  matchers?: { [key: string]: any };
  responseTemplate?: string;
  responseStatus?: number;
  responseHeaders?: { [key: string]: string };
  preScript?: string;
  postScript?: string;
  scriptLanguage?: string;
  delayMs?: number;
  version?: number;
  active?: boolean;
  scenarioName?: string;
  createdAt?: string;
  updatedAt?: string;
}

// Scenario Models
export interface ScenarioState {
  name: string;
  description?: string;
  responseTemplate?: string;
  responseStatus?: number;
  responseHeaders?: { [key: string]: string };
  delayMs?: number;
  transitions?: StateTransition[];
  preScript?: string;
  postScript?: string;
  scriptLanguage?: string;
}

export interface StateTransition {
  name?: string;
  condition: string;
  targetState: string;
  priority?: number;
}

export interface ScenarioRequest {
  name: string;
  description?: string;
  initialState: string;
  states: ScenarioState[];
  maxExecutions?: number;
  autoReset?: boolean;
  active?: boolean;
}

export interface ScenarioResponse {
  id: string;
  name: string;
  description?: string;
  initialState: string;
  currentState?: string;
  states: ScenarioState[];
  active?: boolean;
  maxExecutions?: number;
  executionCount?: number;
  autoReset?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
