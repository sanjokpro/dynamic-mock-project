import type { Metadata } from 'next';
import './globals.css';
import { Providers } from './providers';
import { Toaster } from 'sonner';
import {
  BrandingConfig,
  DEFAULT_BRANDING,
  normalizeBrandingConfig,
} from '@/types/branding';
import { buildBrandingCssVariables } from '@/lib/colorUtils';

export const dynamic = 'force-dynamic';

const SSR_FETCH_TIMEOUT_MS = 1500;

async function fetchSsrBranding(): Promise<BrandingConfig> {
  const candidates = Array.from(
    new Set(
      [
        process.env.BACKEND_URL,
        'http://mock-server:8080',
        'http://localhost:8080',
      ].filter((url): url is string => Boolean(url && url.trim()))
    )
  );

  for (const baseUrl of candidates) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), SSR_FETCH_TIMEOUT_MS);
    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/config/branding`, {
        cache: 'no-store',
        signal: controller.signal,
      });
      if (response.ok) {
        const data = (await response.json()) as Partial<BrandingConfig>;
        return normalizeBrandingConfig(data);
      }
    } catch {
      // Try next candidate or fall back to DEFAULT_BRANDING
    } finally {
      clearTimeout(timer);
    }
  }

  return { ...DEFAULT_BRANDING };
}

export async function generateMetadata(): Promise<Metadata> {
  const branding = await fetchSsrBranding();
  return {
    title: `${branding.productName} - API Workspace`,
    description: `${branding.organizationName} — Enterprise Integration Simulation Platform`,
    icons: {
      icon: branding.faviconUrl || '/favicon.ico',
    },
  };
}

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const branding = await fetchSsrBranding();
  const ssrThemeCss = buildBrandingCssVariables(
    branding.primaryColor,
    branding.secondaryColor
  );

  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <link rel="icon" href={branding.faviconUrl || '/favicon.ico'} />
        <style
          id="ssr-branding-theme"
          dangerouslySetInnerHTML={{ __html: ssrThemeCss }}
        />
        <script
          dangerouslySetInnerHTML={{
            __html: `
          try {
            var theme = localStorage.getItem('dynamic-mock-theme');
            if (!theme) {
              theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
            }
            document.documentElement.className = theme;
          } catch(e) {}
        `,
          }}
        />
      </head>
      <body className="antialiased">
        <Providers initialBranding={branding}>
          {children}
          <Toaster position="bottom-right" />
        </Providers>
      </body>
    </html>
  );
}
