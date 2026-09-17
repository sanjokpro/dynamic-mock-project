import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import { Collection } from '@/types';

const API_URL = '/collections';

export function useCollections(userId: string) {
  const queryClient = useQueryClient();

  const collectionsQuery = useQuery({
    queryKey: ['collections', userId],
    queryFn: async () => {
      const { data } = await apiClient.get<Collection[]>(`${API_URL}?userId=${userId}`);
      return data;
    },
    enabled: !!userId,
  });

  const createCollectionMutation = useMutation({
    mutationFn: async (newCollection: { name: string; userId: string; description?: string }) => {
      const { data } = await apiClient.post<Collection>(API_URL, newCollection);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collections', userId] });
    },
  });

  const updateCollectionMutation = useMutation({
    mutationFn: async ({ id, ...updatedCollection }: Partial<Collection> & { id: string }) => {
      const { data } = await apiClient.put<Collection>(`${API_URL}/${id}`, updatedCollection);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collections', userId] });
    },
  });

  const deleteCollectionMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`${API_URL}/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collections', userId] });
    },
  });

  const addItemMutation = useMutation({
    mutationFn: async ({ collectionId, item }: { collectionId: string; item: any }) => {
      const { data } = await apiClient.post<Collection>(`${API_URL}/${collectionId}/items`, item);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collections', userId] });
    },
  });

  return {
    collections: collectionsQuery.data || [],
    isLoading: collectionsQuery.isLoading,
    error: collectionsQuery.error,
    createCollection: createCollectionMutation.mutate,
    updateCollection: updateCollectionMutation.mutate,
    deleteCollection: deleteCollectionMutation.mutate,
    addItemToCollection: addItemMutation.mutateAsync,
  };
}
