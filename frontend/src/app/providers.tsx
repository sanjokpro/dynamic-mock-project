'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

import { AuthProvider } from '@/context/AuthContext';
import { BrandingProvider } from '@/context/BrandingProvider';
import { WorkspaceProvider } from '@/context/WorkspaceContext';
import { ThemeProvider } from '@/context/ThemeContext';
import { DialogProvider } from '@/context/DialogContext';
import { BrandingConfig } from '@/types/branding';

export interface ProvidersProps {
  children: React.ReactNode;
  initialBranding?: Partial<BrandingConfig> | null;
}

export function Providers({ children, initialBranding }: ProvidersProps) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
      },
    },
  }));

  return (
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <BrandingProvider initialBranding={initialBranding}>
            <DialogProvider>
              <WorkspaceProvider>
                {children}
              </WorkspaceProvider>
            </DialogProvider>
          </BrandingProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </AuthProvider>
  );
}
