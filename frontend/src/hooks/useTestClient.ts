import { useMutation } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';

const API_URL = '/test-client/execute';

export interface TestRequest {
  url: string;
  method: string;
  headers?: Record<string, string>;
  body?: string;
}

export interface TestResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: string;
  durationMs: number;
}

export function useTestClient() {
  const executeMutation = useMutation({
    mutationFn: async (request: TestRequest) => {
      const { data } = await apiClient.post<TestResponse>(API_URL, request);
      return data;
    },
  });

  return {
    execute: executeMutation.mutate,
    isLoading: executeMutation.isPending,
    response: executeMutation.data,
    error: executeMutation.error,
  };
}
