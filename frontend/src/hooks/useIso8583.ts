import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/apiClient';

// ===================== Simulator Types =====================

export interface SimulateRequest {
  mti: string;
  fields: Record<string, string>;
  timeoutMs?: number;
}

export interface SimulateResponse {
  success: boolean;
  responseMti?: string;
  responseFields?: Record<string, string>;
  requestHex?: string;
  responseHex?: string;
  latencyMs?: number;
  errorMessage?: string;
  errorType?: string;
}

// ===================== Simulator Hook =====================

export function useIso8583Simulator(endpointId: string) {
  const simulateMutation = useMutation({
    mutationFn: async (request: SimulateRequest): Promise<SimulateResponse> => {
      const { data } = await apiClient.post<SimulateResponse>(
        `/iso8583/endpoints/${endpointId}/simulate`,
        request
      );
      return data;
    },
  });

  return {
    simulate: simulateMutation.mutateAsync,
    isSimulating: simulateMutation.isPending,
    result: simulateMutation.data,
    error: simulateMutation.error,
    reset: simulateMutation.reset,
  };
}

// ===================== Scenario State Details Types =====================

export interface ScenarioStateDetails {
  scenarioId: string;
  scenarioName: string;
  currentState: string;
  initialState: string;
  executionCount: number;
  stateVariables: Record<string, unknown>;
  active: boolean;
}

// ===================== Scenario State Inspector Hook =====================

export function useScenarioStateDetails(scenarioId: string | null) {
  return useQuery<ScenarioStateDetails>({
    queryKey: ['scenario-state-details', scenarioId],
    queryFn: async () => {
      const { data } = await apiClient.get<ScenarioStateDetails>(
        `/scenarios/${scenarioId}/state/details`
      );
      return data;
    },
    enabled: !!scenarioId,
    refetchInterval: 5000, // auto-refresh every 5 seconds
    staleTime: 1000,
  });
}
