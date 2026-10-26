import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import {
  HeadersEditor,
  parseRawHeaders,
  formatRawHeaders,
  validateHeaderName,
  validateRawHeaderLines,
  HeaderRow
} from './HeadersEditor';

describe('HeadersEditor', () => {
  describe('validateHeaderName (RFC 7230 / RFC 9110)', () => {
    it('accepts valid HTTP header names', () => {
      expect(validateHeaderName('Content-Type').isValid).toBe(true);
      expect(validateHeaderName('X-Correlation-Id').isValid).toBe(true);
      expect(validateHeaderName('Authorization').isValid).toBe(true);
      expect(validateHeaderName('myheader').isValid).toBe(true);
      expect(validateHeaderName('custom_header_123').isValid).toBe(true);
      expect(validateHeaderName('a!b#c$d%e&f\'g*h+i-j.k^l_m`n|o~p').isValid).toBe(true);
    });

    it('rejects header names containing spaces or whitespace', () => {
      const res = validateHeaderName('my header');
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('spaces or whitespace');

      const tabRes = validateHeaderName('my\theader');
      expect(tabRes.isValid).toBe(false);
    });

    it('rejects header names containing colons', () => {
      const res = validateHeaderName('my:header');
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('colons');
    });

    it('rejects header names containing non-ASCII characters', () => {
      const res = validateHeaderName('héader');
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('ASCII');
    });

    it('rejects header names containing invalid delimiters like /@()<>[]{}?=', () => {
      expect(validateHeaderName('header@domain').isValid).toBe(false);
      expect(validateHeaderName('header[0]').isValid).toBe(false);
      expect(validateHeaderName('header=val').isValid).toBe(false);
      expect(validateHeaderName('header/sub').isValid).toBe(false);
    });

    it('rejects empty header names', () => {
      expect(validateHeaderName('').isValid).toBe(false);
      expect(validateHeaderName('   ').isValid).toBe(false);
    });
  });

  describe('validateRawHeaderLines', () => {
    it('flags lines without a colon separator as info notice', () => {
      const issues = validateRawHeaderLines('myheader\nValid-Header: 123');
      expect(issues).toHaveLength(1);
      expect(issues[0].lineNumber).toBe(1);
      expect(issues[0].message).toContain("Missing ':' separator");
    });

    it('flags lines with spaces in header name as error', () => {
      const issues = validateRawHeaderLines('my header: value');
      expect(issues).toHaveLength(1);
      expect(issues[0].lineNumber).toBe(1);
      expect(issues[0].type).toBe('error');
      expect(issues[0].message).toContain('spaces or whitespace');
    });

    it('flags lines with whitespace before colon as warning', () => {
      const issues = validateRawHeaderLines('Header-Name : value');
      expect(issues.some(i => i.message.includes('Whitespace before colon'))).toBe(true);
    });

    it('handles Postman-style // comments gracefully', () => {
      const issues = validateRawHeaderLines('// Valid-Header: 123\n// Invalid Header: 456');
      expect(issues).toHaveLength(1);
      expect(issues[0].lineNumber).toBe(2);
      expect(issues[0].message).toContain('spaces or whitespace');
    });

    it('allows blank lines without creating issues', () => {
      const issues = validateRawHeaderLines('\n\nContent-Type: application/json\n\n');
      expect(issues).toHaveLength(0);
    });
  });

  describe('parseRawHeaders & formatRawHeaders', () => {
    it('parses multiline key:value pairs with trim', () => {
      const raw = 'Content-Type: application/json\nAuthorization: Bearer token123\nCustom-Header: value with : colon';
      const parsed = parseRawHeaders(raw);

      expect(parsed).toHaveLength(3);
      expect(parsed[0].key).toBe('Content-Type');
      expect(parsed[0].value).toBe('application/json');
      expect(parsed[0].enabled).toBe(true);

      expect(parsed[1].key).toBe('Authorization');
      expect(parsed[1].value).toBe('Bearer token123');

      expect(parsed[2].key).toBe('Custom-Header');
      expect(parsed[2].value).toBe('value with : colon');
    });

    it('treats line without colon as key with empty value', () => {
      const raw = 'StandaloneKey\nKey: Value';
      const parsed = parseRawHeaders(raw);

      expect(parsed).toHaveLength(2);
      expect(parsed[0].key).toBe('StandaloneKey');
      expect(parsed[0].value).toBe('');
      expect(parsed[1].key).toBe('Key');
      expect(parsed[1].value).toBe('Value');
    });

    it('parses lines starting with // as disabled headers (Postman-style)', () => {
      const raw = 'Content-Type: application/json\n// Cache-Control: no-cache';
      const parsed = parseRawHeaders(raw);

      expect(parsed).toHaveLength(2);
      expect(parsed[0].enabled).toBe(true);
      expect(parsed[1].key).toBe('Cache-Control');
      expect(parsed[1].value).toBe('no-cache');
      expect(parsed[1].enabled).toBe(false);
    });

    it('formats only enabled rows with non-empty keys by default', () => {
      const rows: HeaderRow[] = [
        { id: '1', key: 'Content-Type', value: 'application/json', enabled: true },
        { id: '2', key: 'Disabled-Header', value: 'val', enabled: false },
        { id: '3', key: 'Accept', value: '*/*', enabled: true },
        { id: '4', key: '   ', value: 'empty', enabled: true },
      ];

      const formatted = formatRawHeaders(rows);
      expect(formatted).toBe('Content-Type: application/json\nAccept: */*');
    });

    it('formats disabled rows as // comments when includeDisabledAsComments is true', () => {
      const rows: HeaderRow[] = [
        { id: '1', key: 'Content-Type', value: 'application/json', enabled: true },
        { id: '2', key: 'Disabled-Header', value: 'val', enabled: false },
      ];

      const formatted = formatRawHeaders(rows, true);
      expect(formatted).toContain('Content-Type: application/json');
      expect(formatted).toContain('// Disabled-Header: val');
    });
  });

  describe('Component Interactions', () => {
    it('renders initial entries and allows toggling row to disable', () => {
      const onChange = vi.fn();
      render(
        <HeadersEditor
          title="Request Headers"
          entries={{ 'head1': 'val1', 'head2': 'val2' }}
          onChange={onChange}
        />
      );

      expect(screen.getByDisplayValue('head1')).toBeDefined();
      expect(screen.getByDisplayValue('val1')).toBeDefined();
      expect(screen.getByDisplayValue('head2')).toBeDefined();
      expect(screen.getByDisplayValue('val2')).toBeDefined();

      // Disable the first row by clicking its checkbox
      const disableBtn = screen.getByLabelText(/Disable head1/i);
      fireEvent.click(disableBtn);

      expect(onChange).toHaveBeenCalled();
      const lastCall = onChange.mock.calls[onChange.mock.calls.length - 1];
      const emittedEntries = lastCall[0];
      const emittedRows = lastCall[1];

      // head1 should be excluded from enabled entries
      expect(emittedEntries).toEqual({ head2: 'val2' });
      // head1 should still exist in emittedRows with enabled = false
      expect(emittedRows.find((r: HeaderRow) => r.key === 'head1')?.enabled).toBe(false);
    });

    it('adds a new row when Add Row is clicked', () => {
      const onChange = vi.fn();
      render(
        <HeadersEditor
          title="Headers"
          entries={{}}
          onChange={onChange}
        />
      );

      const addBtn = screen.getByRole('button', { name: /Add Row/i });
      fireEvent.click(addBtn);

      const keyInputs = screen.getAllByPlaceholderText('Key');
      expect(keyInputs.length).toBe(1);

      fireEvent.change(keyInputs[0], { target: { value: 'X-New' } });
      const valueInput = screen.getByPlaceholderText('Value');
      fireEvent.change(valueInput, { target: { value: 'New-Value' } });

      expect(onChange).toHaveBeenCalled();
      const lastCall = onChange.mock.calls[onChange.mock.calls.length - 1];
      expect(lastCall[0]).toEqual({ 'X-New': 'New-Value' });
    });

    it('switches to Raw mode and syncs parsed entries', () => {
      const onChange = vi.fn();
      render(
        <HeadersEditor
          title="Headers"
          entries={{ 'h1': 'v1' }}
          onChange={onChange}
        />
      );

      // Switch to Raw mode
      const rawBtn = screen.getByRole('button', { name: /Raw/i });
      fireEvent.click(rawBtn);

      const textarea = screen.getByPlaceholderText(/Content-Type:/i);
      expect((textarea as HTMLTextAreaElement).value).toContain('h1: v1');

      // Update textarea with multiple headers
      fireEvent.change(textarea, { target: { value: 'h1: v1\nh2: v2\nh3: v3' } });

      expect(onChange).toHaveBeenCalled();
      const lastCall = onChange.mock.calls[onChange.mock.calls.length - 1];
      expect(lastCall[0]).toEqual({
        h1: 'v1',
        h2: 'v2',
        h3: 'v3'
      });

      // Switch back to Table mode
      const tableBtn = screen.getByRole('button', { name: /Table/i });
      fireEvent.click(tableBtn);

      expect(screen.getByDisplayValue('h1')).toBeDefined();
      expect(screen.getByDisplayValue('h2')).toBeDefined();
      expect(screen.getByDisplayValue('h3')).toBeDefined();
    });

    it('preserves exact raw typing without inserting premature colon or deleting newlines on enter', () => {
      const onChange = vi.fn();
      const { rerender } = render(
        <HeadersEditor
          title="Mock Response Headers"
          entries={{}}
          onChange={onChange}
        />
      );

      // Switch to Raw mode
      const rawBtn = screen.getByRole('button', { name: /Raw/i });
      fireEvent.click(rawBtn);

      const textarea = screen.getByPlaceholderText(/Content-Type:/i) as HTMLTextAreaElement;

      // 1. Type "myheader" without colon
      fireEvent.change(textarea, { target: { value: 'myheader' } });
      expect(textarea.value).toBe('myheader');

      // Rerender simulating parent passing back the emitted state
      rerender(
        <HeadersEditor
          title="Mock Response Headers"
          entries={{ myheader: '' }}
          onChange={onChange}
        />
      );
      // It must NOT become "m:yheader" or "myheader:"
      expect(textarea.value).toBe('myheader');

      // 2. Press Enter to create a newline
      fireEvent.change(textarea, { target: { value: 'myheader\n' } });
      expect(textarea.value).toBe('myheader\n');

      // 3. Type on the new line
      fireEvent.change(textarea, { target: { value: 'myheader: value\nX-Second: 2' } });
      expect(textarea.value).toBe('myheader: value\nX-Second: 2');
    });

    it('shows inline validation warning on invalid header name in table mode', () => {
      render(
        <HeadersEditor
          title="Mock Response Headers"
          entries={{ 'invalid header with spaces': 'test' }}
        />
      );

      // Look for the warning icon title/aria-label
      const warningIcon = screen.getByLabelText(/Header names cannot contain spaces or whitespace/i);
      expect(warningIcon).toBeDefined();
    });

    it('displays format notice box in raw mode when header syntax is invalid', () => {
      render(
        <HeadersEditor
          title="Mock Response Headers"
          entries={{}}
        />
      );

      // Switch to Raw mode
      const rawBtn = screen.getByRole('button', { name: /Raw/i });
      fireEvent.click(rawBtn);

      const textarea = screen.getByPlaceholderText(/Content-Type:/i);
      fireEvent.change(textarea, { target: { value: 'invalid header with spaces: val' } });

      expect(screen.getByText(/HTTP Header Format Notice/i)).toBeDefined();
      expect(screen.getByText(/Line 1:/i)).toBeDefined();
      expect(screen.getByText(/Header names cannot contain spaces or whitespace/i)).toBeDefined();
    });
  });
});
