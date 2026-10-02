import { renderHook, waitFor } from '@testing-library/react';
import { useCollections } from './useCollections';
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

describe('useCollections', () => {
  let queryClient: QueryClient;
  const userId = 'user-123';

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

  it('should fetch collections', async () => {
    const mockData = [{ id: '1', name: 'Test Collection', items: [] }];
    (apiClient.get as any).mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useCollections(userId), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.collections).toEqual(mockData);
    expect(apiClient.get).toHaveBeenCalledWith(`/collections?userId=${userId}`);
  });

  it('should create a collection', async () => {
    const newCollection = { name: 'New Col', userId };
    const mockData = { id: '2', ...newCollection, items: [] };
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.post as any).mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useCollections(userId), { wrapper });

    await result.current.createCollection(newCollection);

    expect(apiClient.post).toHaveBeenCalledWith('/collections', newCollection);
  });

  it('should update a collection', async () => {
    const updateData = { id: '1', name: 'Renamed Col' };
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.put as any).mockResolvedValueOnce({ data: updateData });

    const { result } = renderHook(() => useCollections(userId), { wrapper });

    await result.current.updateCollection(updateData);

    expect(apiClient.put).toHaveBeenCalledWith('/collections/1', { name: 'Renamed Col' });
  });

  it('should delete a collection', async () => {
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.delete as any).mockResolvedValueOnce({ data: {} });

    const { result } = renderHook(() => useCollections(userId), { wrapper });

    await result.current.deleteCollection('1');

    expect(apiClient.delete).toHaveBeenCalledWith('/collections/1');
  });

  it('should add an item to a collection', async () => {
    const itemData = { collectionId: '1', item: { name: 'Route', protocol: 'HTTP', resourceId: 'route-1' } };
    (apiClient.get as any).mockResolvedValueOnce({ data: [] });
    (apiClient.post as any).mockResolvedValueOnce({ data: { id: '1', items: [itemData.item] } });

    const { result } = renderHook(() => useCollections(userId), { wrapper });

    await result.current.addItemToCollection(itemData);

    expect(apiClient.post).toHaveBeenCalledWith('/collections/1/items', itemData.item);
  });
});
