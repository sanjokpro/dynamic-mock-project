import { WorkspaceLayout } from '@/components/workspace/WorkspaceLayout';
import { AuthGuard } from '@/components/ui/AuthGuard';

export default function Home() {
  return (
    <main>
      <AuthGuard>
        <WorkspaceLayout />
      </AuthGuard>
    </main>
  );
}
