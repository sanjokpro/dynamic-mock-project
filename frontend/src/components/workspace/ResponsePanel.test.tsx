import { describe, it, expect } from 'vitest';
import { highlightJson } from './ResponsePanel';

describe('ResponsePanel - highlightJson', () => {
  it('correctly preserves string values and does not swallow them (BUG 1 fix)', () => {
    const input = '{"reached":"head2"}';
    const highlighted = highlightJson(input, 'dark');

    // Must contain "head2" as a rendered string value
    expect(highlighted).toContain('head2');
    expect(highlighted).not.toContain('&quot;&quot;');
    expect(highlighted).toContain('<span style="color:#7dd3fc">&quot;reached&quot;</span>');
    expect(highlighted).toContain('<span style="color:#a5d6a7">&quot;head2&quot;</span>');
  });

  it('correctly tokenizes complex JSON structures', () => {
    const json = JSON.stringify({
      message: 'success',
      count: 42,
      active: true,
      data: null,
      items: ['one', 'two']
    });

    const highlighted = highlightJson(json, 'dark');
    expect(highlighted).toContain('success');
    expect(highlighted).toContain('42');
    expect(highlighted).toContain('true');
    expect(highlighted).toContain('null');
    expect(highlighted).toContain('one');
    expect(highlighted).toContain('two');
  });

  it('handles empty string value correctly without dropping keys', () => {
    const input = '{"key":""}';
    const highlighted = highlightJson(input, 'dark');
    expect(highlighted).toContain('key');
    expect(highlighted).toContain('&quot;&quot;');
  });
});
