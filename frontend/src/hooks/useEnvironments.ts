import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import { Environment } from '@/types';

const API_URL = '/environments';

export function useEnvironments() {
  const queryClient = useQueryClient();

  const environmentsQuery = useQuery({
    queryKey: ['environments'],
    queryFn: async () => {
      const { data } = await apiClient.get<Environment[]>(API_URL);
      return data;
    },
  });

  const createEnvironmentMutation = useMutation({
    mutationFn: async (newEnv: Partial<Environment>) => {
      const { data } = await apiClient.post<Environment>(API_URL, newEnv);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['environments'] });
    },
  });

  const updateEnvironmentMutation = useMutation({
    mutationFn: async ({ id, ...updatedEnv }: Partial<Environment> & { id: string }) => {
      const { data } = await apiClient.put<Environment>(`${API_URL}/${id}`, updatedEnv);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['environments'] });
    },
  });

  const deleteEnvironmentMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`${API_URL}/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['environments'] });
    },
  });

  return {
    environments: environmentsQuery.data || [],
    isLoading: environmentsQuery.isLoading,
    createEnvironment: createEnvironmentMutation.mutate,
    updateEnvironment: updateEnvironmentMutation.mutate,
    deleteEnvironment: deleteEnvironmentMutation.mutate,
  };
}
