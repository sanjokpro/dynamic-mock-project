import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import {
  BrandingConfig,
  BrandingUpdateRequest,
  DEFAULT_BRANDING,
  normalizeBrandingConfig,
} from '@/types/branding';

export const BRANDING_QUERY_KEY = ['config', 'branding'] as const;

export interface UseBrandingOptions {
  initialData?: BrandingConfig;
}

export function useBranding(options?: UseBrandingOptions) {
  const queryClient = useQueryClient();

  const query = useQuery<BrandingConfig>({
    queryKey: BRANDING_QUERY_KEY,
    queryFn: async () => {
      const response = await apiClient.get<BrandingConfig>('/config/branding');
      return normalizeBrandingConfig(response.data);
    },
    initialData: options?.initialData,
    staleTime: 60 * 1000,
    retry: 1,
  });

  const updateMutation = useMutation<BrandingConfig, Error, BrandingUpdateRequest>({
    mutationFn: async (request: BrandingUpdateRequest) => {
      const response = await apiClient.put<BrandingConfig>('/config/branding', request);
      return normalizeBrandingConfig(response.data);
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(BRANDING_QUERY_KEY, updated);
      queryClient.invalidateQueries({ queryKey: BRANDING_QUERY_KEY });
    },
  });

  const resetMutation = useMutation<BrandingConfig, Error, void>({
    mutationFn: async () => {
      const response = await apiClient.post<BrandingConfig>('/config/branding/reset');
      return normalizeBrandingConfig(response.data);
    },
    onSuccess: (resetConfig) => {
      queryClient.setQueryData(BRANDING_QUERY_KEY, resetConfig);
      queryClient.invalidateQueries({ queryKey: BRANDING_QUERY_KEY });
    },
  });

  return {
    branding: query.data ?? options?.initialData ?? DEFAULT_BRANDING,
    isLoading: query.isLoading,
    error: query.error,
    updateBranding: updateMutation.mutateAsync,
    resetBranding: resetMutation.mutateAsync,
    isUpdating: updateMutation.isPending || resetMutation.isPending,
    isResetting: resetMutation.isPending,
  };
}
