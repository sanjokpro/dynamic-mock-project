export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS';
export type Protocol = 'HTTP' | 'GRAPHQL' | 'GRPC' | 'ISO8583';

export interface PlatformResource {
  id: string;
  name: string;
  protocol: Protocol;
  createdAt: string;
  updatedAt: string;
}

export interface MockRoute extends PlatformResource {
  protocol: 'HTTP';
  path: string;
  method: HttpMethod;
  matchers?: {
    headers?: Record<string, string>;
    queryParams?: Record<string, string>;
    bodyMatchType?: string;
    bodyMatchPattern?: string;
  };
  responseTemplate?: string;
  responseStatus?: number;
  queryParams?: Record<string, string>;
  responseHeaders?: Record<string, string>;
  preScript?: string;
  postScript?: string;
  scriptLanguage?: string;
  delayMs?: number;
  version?: number;
  active?: boolean;
  scenarioName?: string;
}

export interface CollectionItem {
  id: string;
  protocol: Protocol;
  resourceId: string;
  name: string;
}

export interface Collection {
  id: string;
  name: string;
  userId: string;
  description?: string;
  items: CollectionItem[];
  createdAt: string;
  updatedAt: string;
}

export interface Environment {
  id: string;
  name: string;
  variables: Record<string, string>;
  isDefault?: boolean;
}

export interface TestResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: any;
  durationMs: number;
}

export interface ExecutionEvent {
  id: string;
  timestamp: string;
  protocol: Protocol;
  resourceId?: string;
  method?: string;
  path?: string;
  status: number;
  durationMs: number;
  matchName?: string;
  requestBody?: string;
  responseBody?: string;
}
