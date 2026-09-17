import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import { Protocol } from '@/types';

const BASE_URL = '';

export function useProtocolEndpoints(protocol: Protocol | undefined) {
  const queryClient = useQueryClient();
  const endpointKey = protocol ? protocol.toLowerCase() : '';
  const API_URL = endpointKey ? `${BASE_URL}/${endpointKey}/endpoints` : '';

  const endpointsQuery = useQuery({
    queryKey: ['endpoints', protocol],
    queryFn: async () => {
      if (!API_URL) return [];
      const { data } = await apiClient.get<any[]>(API_URL);
      return data;
    },
    enabled: !!protocol,
  });

  const createMutation = useMutation({
    mutationFn: async (newEndpoint: any) => {
      const { data } = await apiClient.post<any>(API_URL, newEndpoint);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['endpoints', protocol] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, ...updatedEndpoint }: any) => {
      const { data } = await apiClient.put<any>(`${API_URL}/${id}`, updatedEndpoint);
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['endpoints', protocol] });
      queryClient.invalidateQueries({ queryKey: ['endpoint', protocol, data.id] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`${API_URL}/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['endpoints', protocol] });
    },
  });

  return {
    endpoints: endpointsQuery.data || [],
    isLoading: endpointsQuery.isLoading,
    createEndpoint: createMutation.mutateAsync,
    updateEndpoint: updateMutation.mutateAsync,
    deleteEndpoint: deleteMutation.mutateAsync,
  };
}
