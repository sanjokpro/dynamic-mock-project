import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';

const API_URL = '/scenarios';

export function useScenarios() {
  const queryClient = useQueryClient();

  const scenariosQuery = useQuery({
    queryKey: ['scenarios'],
    queryFn: async () => {
      const { data } = await apiClient.get<any[]>(API_URL);
      return data;
    },
  });

  const createScenarioMutation = useMutation({
    mutationFn: async (newScenario: any) => {
      const { data } = await apiClient.post<any>(API_URL, newScenario);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scenarios'] });
    },
  });

  const updateScenarioMutation = useMutation({
    mutationFn: async ({ id, ...updatedScenario }: any) => {
      const { data } = await apiClient.put<any>(`${API_URL}/${id}`, updatedScenario);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scenarios'] });
    },
  });

  const deleteScenarioMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`${API_URL}/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scenarios'] });
    },
  });

  return {
    scenarios: scenariosQuery.data || [],
    isLoading: scenariosQuery.isLoading,
    createScenario: createScenarioMutation.mutateAsync,
    updateScenario: updateScenarioMutation.mutateAsync,
    deleteScenario: deleteScenarioMutation.mutateAsync,
  };
}
