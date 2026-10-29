import { describe, it, expect } from 'vitest';
import { getHttpStatusPhrase, formatHttpStatus, formatPayloadSize, formatTimestampWithMs } from './httpStatus';

describe('httpStatus helper', () => {
  it('maps standard status codes to correct reason phrases', () => {
    expect(getHttpStatusPhrase(200)).toBe('OK');
    expect(getHttpStatusPhrase(201)).toBe('Created');
    expect(getHttpStatusPhrase(400)).toBe('Bad Request');
    expect(getHttpStatusPhrase(401)).toBe('Unauthorized');
    expect(getHttpStatusPhrase(404)).toBe('Not Found');
    expect(getHttpStatusPhrase(500)).toBe('Internal Server Error');
  });

  it('provides graceful fallbacks for custom or unmapped status codes', () => {
    expect(getHttpStatusPhrase(299)).toBe('Custom Status');
    expect(getHttpStatusPhrase(499)).toBe('Error');
  });

  it('formats status and phrase into badge label', () => {
    expect(formatHttpStatus(200)).toBe('200 OK');
    expect(formatHttpStatus(404)).toBe('404 Not Found');
    expect(formatHttpStatus(502)).toBe('502 Bad Gateway');
    expect(formatHttpStatus(200, 'Custom Text')).toBe('200 Custom Text');
  });

  it('formats payload sizes correctly in B, KB, and MB', () => {
    expect(formatPayloadSize(500)).toBe('500 B');
    expect(formatPayloadSize(1024)).toBe('1.0 KB');
    expect(formatPayloadSize(2048)).toBe('2.0 KB');
    expect(formatPayloadSize(1048576)).toBe('1.0 MB');
  });

  it('formats dates into "HH:mm:ss.SSS" millisecond precision string consistently', () => {
    const testDate = new Date(2026, 9, 29, 13, 1, 32, 421);
    expect(formatTimestampWithMs(testDate)).toBe('13:01:32.421');

    const testDate2 = new Date(2026, 9, 29, 9, 5, 8, 7);
    expect(formatTimestampWithMs(testDate2)).toBe('09:05:08.007');

    expect(formatTimestampWithMs(null)).toBe('');
    expect(formatTimestampWithMs(undefined)).toBe('');
    expect(formatTimestampWithMs('invalid-date')).toBe('');
  });
});
