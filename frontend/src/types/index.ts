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

export interface RedirectHop {
  step: number;
  method: string;
  url: string;
  status: number;
  location?: string;
  durationMs: number;
}

export interface WebhookExecutionDetail {
  sourceRouteId?: string;
  parentEventId?: string;
  targetUrl?: string;
  finalUrl?: string;
  method?: string;
  delayMs?: number;
  requestHeaders?: Record<string, string>;
  requestBody?: string;
  responseStatus?: number;
  responseHeaders?: Record<string, string>;
  responseBody?: string;
  errorMessage?: string;
}

export interface TestResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: any;
  durationMs: number;
  redirectChain?: RedirectHop[];
}

export interface ExecutionEvent {
  id: string;
  timestamp: string;
  protocol: Protocol | 'WEBHOOK';
  resourceId?: string;
  parentEventId?: string;
  method?: string;
  path?: string;
  queryString?: string;
  queryParams?: Record<string, string>;
  requestHeaders?: Record<string, string>;
  requestBody?: string;
  status: number;
  responseHeaders?: Record<string, string>;
  responseBody?: string;
  durationMs: number;
  matchName?: string;
  redirectChain?: RedirectHop[];
  webhookDetail?: WebhookExecutionDetail;
  payloadSizeBytes?: number;
  bodyTruncated?: boolean;
}

export interface ImportReport {
  environments: number;
  collections: number;
  routes: number;
  warnings: string[];
  errors: string[];
}
