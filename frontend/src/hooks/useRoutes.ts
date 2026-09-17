import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import { MockRoute } from '@/types';

const API_URL = '/routes';

export function useRoutes() {
  const queryClient = useQueryClient();

  const routesQuery = useQuery({
    queryKey: ['routes'],
    queryFn: async () => {
      const { data } = await apiClient.get<MockRoute[]>(API_URL);
      return data;
    },
  });

  const createRouteMutation = useMutation({
    mutationFn: async (newRoute: Partial<MockRoute>) => {
      const { data } = await apiClient.post<MockRoute>(API_URL, newRoute);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
  });

  const updateRouteMutation = useMutation({
    mutationFn: async ({ id, ...updatedRoute }: Partial<MockRoute> & { id: string }) => {
      const { data } = await apiClient.put<MockRoute>(`${API_URL}/${id}`, updatedRoute);
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
      queryClient.invalidateQueries({ queryKey: ['route', data.id] });
    },
  });

  const deleteRouteMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`${API_URL}/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
  });

  const activateRouteMutation = useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.post<MockRoute>(`${API_URL}/${id}/activate`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
  });

  const deactivateRouteMutation = useMutation({
    mutationFn: async (id: string) => {
      const { data } = await apiClient.post<MockRoute>(`${API_URL}/${id}/deactivate`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
  });

  return {
    routes: routesQuery.data || [],
    isLoading: routesQuery.isLoading,
    createRoute: createRouteMutation.mutateAsync,
    updateRoute: updateRouteMutation.mutateAsync,
    deleteRoute: deleteRouteMutation.mutateAsync,
    activateRoute: activateRouteMutation.mutateAsync,
    deactivateRoute: deactivateRouteMutation.mutateAsync,
  };
}

export function useRoute(id: string | null) {
  return useQuery({
    queryKey: ['route', id],
    queryFn: async () => {
      if (!id) return null;
      const { data } = await apiClient.get<MockRoute>(`${API_URL}/${id}`);
      return data;
    },
    enabled: !!id,
  });
}
