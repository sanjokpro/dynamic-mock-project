import { renderHook, waitFor } from '@testing-library/react';
import { useScenarios } from './useScenarios';
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

describe('useScenarios', () => {
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

  it('should fetch scenarios', async () => {
    const mockData = [{ id: '1', name: 'Test Scenario' }];
    (apiClient.get as any).mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useScenarios(), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.scenarios).toEqual(mockData);
    expect(apiClient.get).toHaveBeenCalledWith('/scenarios');
  });

  it('should create a scenario', async () => {
    const newScenario = { name: 'New Scenario' };
    const mockData = { id: '2', ...newScenario };
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.post as any).mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useScenarios(), { wrapper });

    await result.current.createScenario(newScenario);

    expect(apiClient.post).toHaveBeenCalledWith('/scenarios', newScenario);
  });
});
