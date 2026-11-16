import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ImportModal } from './ImportModal';
import { ImportReport } from '@/types';

const mockImportPostman = vi.fn();

vi.mock('@/hooks/usePostmanImport', () => ({
  usePostmanImport: () => ({
    importPostman: mockImportPostman,
    isImporting: false,
    error: null,
    reset: vi.fn(),
  }),
}));

describe('ImportModal Report Presentation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  async function triggerImportWithReport(report: ImportReport) {
    mockImportPostman.mockResolvedValueOnce(report);

    const { container } = render(<ImportModal open={true} onClose={() => {}} />);

    // Simulate selecting a valid collection JSON file
    const file = new File(['{"info": {"name": "test"}}'], 'test-collection.json', {
      type: 'application/json',
    });

    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    expect(input).toBeDefined();

    fireEvent.change(input, { target: { files: [file] } });

    // Click Import button
    const importBtn = screen.getByRole('button', { name: /^import$/i });
    fireEvent.click(importBtn);

    // Wait for result screen
    await waitFor(() => {
      expect(screen.getByText('Import complete')).toBeDefined();
    });
  }

  it('renders clean success state when there are 0 warnings, 0 errors, and no notices', async () => {
    const cleanReport: ImportReport = {
      environments: 0,
      collections: 1,
      routes: 3,
      warnings: [],
      errors: [],
    };

    await triggerImportWithReport(cleanReport);

    expect(screen.getByText('Import complete')).toBeDefined();
    expect(screen.getByText('Routes')).toBeDefined();
    expect(screen.getByText('3')).toBeDefined();
    expect(screen.queryByText(/Notices/i)).toBeNull();
    expect(screen.queryByText(/Warnings/i)).toBeNull();
    expect(screen.queryByText(/Errors/i)).toBeNull();
  });

  it('renders notices section in neutral tone when notices are returned', async () => {
    const reportWithNotices: ImportReport = {
      environments: 0,
      collections: 1,
      routes: 2,
      notices: ['Request "Items": Postman test scripts were skipped (assertions do not run in mock server)'],
      warnings: [],
      errors: [],
    };

    await triggerImportWithReport(reportWithNotices);

    expect(screen.getByText('Notices (1)')).toBeDefined();
    expect(
      screen.getByText(/Postman test scripts were skipped/)
    ).toBeDefined();
    expect(screen.queryByText(/Warnings/i)).toBeNull();
  });

  it('renders warnings section when actionable routing conflicts exist', async () => {
    const reportWithWarnings: ImportReport = {
      environments: 0,
      collections: 1,
      routes: 2,
      warnings: [
        'Identical matching conditions for GET /items: "Variant 1" and "Variant 2" specify the same request conditions.',
      ],
      errors: [],
    };

    await triggerImportWithReport(reportWithWarnings);

    expect(screen.getByText('Warnings (1)')).toBeDefined();
    expect(screen.getByText(/Identical matching conditions for GET \/items/)).toBeDefined();
  });

  it('handles backwards-compatibility safely when notices property is undefined', async () => {
    const legacyReport: ImportReport = {
      environments: 1,
      collections: 1,
      routes: 5,
      // notices omitted (undefined)
      warnings: [],
      errors: [],
    };

    await triggerImportWithReport(legacyReport);

    expect(screen.getByText('Import complete')).toBeDefined();
    expect(screen.queryByText(/Notices/i)).toBeNull();
  });
});
