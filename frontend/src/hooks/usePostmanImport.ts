import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';
import { ImportReport } from '@/types';

export interface ImportPayload {
  collection: File;
  environments: File[];
}

export function usePostmanImport() {
  const queryClient = useQueryClient();

  const importMutation = useMutation({
    mutationFn: async ({ collection, environments }: ImportPayload): Promise<ImportReport> => {
      const formData = new FormData();
      formData.append('file', collection);
      for (const env of environments) {
        formData.append('files', env);
      }
      const { data } = await apiClient.post<ImportReport>('/import/postman', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return data;
    },
    onSuccess: () => {
      // refresh sidebar data so imported routes/collections appear immediately
      queryClient.invalidateQueries({ queryKey: ['routes'] });
      queryClient.invalidateQueries({ queryKey: ['collections'] });
      queryClient.invalidateQueries({ queryKey: ['environments'] });
    },
  });

  return {
    importPostman: importMutation.mutateAsync,
    isImporting: importMutation.isPending,
    error: importMutation.error as AxiosErrorLike | null,
    reset: importMutation.reset,
  };
}

interface AxiosErrorLike {
  message: string;
  response?: {
    status?: number;
    data?: { errors?: string[] } | string;
  };
}
