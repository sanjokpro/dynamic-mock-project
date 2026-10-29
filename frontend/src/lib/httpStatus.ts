export const HTTP_STATUS_PHRASES: Record<number, string> = {
  100: 'Continue',
  101: 'Switching Protocols',
  102: 'Processing',
  200: 'OK',
  201: 'Created',
  202: 'Accepted',
  204: 'No Content',
  206: 'Partial Content',
  301: 'Moved Permanently',
  302: 'Found',
  304: 'Not Modified',
  307: 'Temporary Redirect',
  308: 'Permanent Redirect',
  400: 'Bad Request',
  401: 'Unauthorized',
  403: 'Forbidden',
  404: 'Not Found',
  405: 'Method Not Allowed',
  408: 'Request Timeout',
  409: 'Conflict',
  410: 'Gone',
  413: 'Payload Too Large',
  415: 'Unsupported Media Type',
  422: 'Unprocessable Entity',
  429: 'Too Many Requests',
  500: 'Internal Server Error',
  501: 'Not Implemented',
  502: 'Bad Gateway',
  503: 'Service Unavailable',
  504: 'Gateway Timeout'
};

/**
 * Returns standard HTTP reason phrase for a given status code.
 */
export function getHttpStatusPhrase(statusCode: number): string {
  return HTTP_STATUS_PHRASES[statusCode] || (statusCode >= 400 ? 'Error' : 'Custom Status');
}

/**
 * Formats status code with reason phrase, e.g. "200 OK" or "404 Not Found".
 */
export function formatHttpStatus(statusCode: number, customStatusText?: string): string {
  if (customStatusText && customStatusText.trim() && customStatusText !== 'OK' && customStatusText !== 'OK ') {
    return `${statusCode} ${customStatusText}`;
  }
  const phrase = getHttpStatusPhrase(statusCode);
  return `${statusCode} ${phrase}`;
}

/**
 * Formats byte size into human-readable B, KB, or MB string.
 */
export function formatPayloadSize(byteLength: number): string {
  if (byteLength < 1024) {
    return `${byteLength} B`;
  }
  if (byteLength < 1024 * 1024) {
    return `${(byteLength / 1024).toFixed(1)} KB`;
  }
  return `${(byteLength / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Formats a date/timestamp into "HH:mm:ss.SSS" string with millisecond precision.
 * Consistently used in the Console Explorer for high-fidelity event telemetry.
 */
export function formatTimestampWithMs(timestamp?: string | number | Date | null): string {
  if (!timestamp) return '';
  try {
    const d = new Date(timestamp);
    if (isNaN(d.getTime())) return '';
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    const millis = String(d.getMilliseconds()).padStart(3, '0');
    return `${hours}:${minutes}:${seconds}.${millis}`;
  } catch {
    return '';
  }
}

