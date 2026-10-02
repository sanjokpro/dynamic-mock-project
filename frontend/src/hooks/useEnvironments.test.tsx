import { renderHook, waitFor } from '@testing-library/react';
import { useEnvironments } from './useEnvironments';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import React from 'react';

vi.mock('@/lib/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  }
}));

describe('useEnvironments', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    vi.clearAllMocks();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  it('should fetch environments', async () => {
    const mockData = [{ id: '1', name: 'Dev Env', variables: {} }];
    (apiClient.get as any).mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useEnvironments(), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.environments).toEqual(mockData);
    expect(apiClient.get).toHaveBeenCalledWith('/environments');
  });

  it('should create an environment', async () => {
    const newEnv = { name: 'Prod Env', variables: {} };
    const mockData = { id: '2', ...newEnv };
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.post as any).mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useEnvironments(), { wrapper });

    await result.current.createEnvironment(newEnv);

    expect(apiClient.post).toHaveBeenCalledWith('/environments', newEnv);
  });

  it('should update an environment', async () => {
    const updateData = { id: '1', name: 'Dev Updated', variables: { API_KEY: '123' } };
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.put as any).mockResolvedValueOnce({ data: updateData });

    const { result } = renderHook(() => useEnvironments(), { wrapper });

    await result.current.updateEnvironment(updateData);

    expect(apiClient.put).toHaveBeenCalledWith('/environments/1', { name: 'Dev Updated', variables: { API_KEY: '123' } });
  });

  it('should delete an environment', async () => {
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.delete as any).mockResolvedValueOnce({ data: {} });

    const { result } = renderHook(() => useEnvironments(), { wrapper });

    await result.current.deleteEnvironment('1');

    expect(apiClient.delete).toHaveBeenCalledWith('/environments/1');
  });
});
