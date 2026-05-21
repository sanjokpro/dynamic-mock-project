import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Iso8583Mock {
  id?: string;
  name: string;
  description?: string;
  mti: string;
  responseMti?: string;
  matchers?: Record<string, string>;
  priority?: number;
  responseFields?: Record<number, string>;
  responseCode?: string;
  script?: string;
  scriptLanguage?: string;
  scriptEnabled?: boolean;
  delayMs?: number;
  enabled?: boolean;
}

export interface Iso8583Endpoint {
  id?: string;
  name: string;
  description?: string;
  port: number;
  isolatedPort?: boolean;
  mocks: Iso8583Mock[];
  
  // Interceptor (disabled by default)
  interceptorScript?: string;
  interceptorScriptLanguage?: string;
  interceptorEnabled?: boolean;
  
  // Advanced jPOS XML (disabled by default)
  customServerXml?: string;
  customXmlEnabled?: boolean;
  
  headerLengthType?: '2BYTE' | '4BYTE' | 'NONE';
  encoding?: 'ASCII' | 'EBCDIC';
  packagerConfig?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Common ISO8583 response codes
export const ISO8583_RESPONSE_CODES = [
  { code: '00', description: 'Approved' },
  { code: '05', description: 'Do not honor' },
  { code: '12', description: 'Invalid transaction' },
  { code: '13', description: 'Invalid amount' },
  { code: '14', description: 'Invalid card number' },
  { code: '30', description: 'Format error' },
  { code: '41', description: 'Lost card' },
  { code: '43', description: 'Stolen card' },
  { code: '51', description: 'Insufficient funds' },
  { code: '54', description: 'Expired card' },
  { code: '55', description: 'Incorrect PIN' },
  { code: '57', description: 'Transaction not permitted' },
  { code: '61', description: 'Exceeds withdrawal limit' },
  { code: '65', description: 'Exceeds withdrawal frequency' },
  { code: '91', description: 'Issuer unavailable' },
  { code: '96', description: 'System malfunction' },
];

// Common ISO8583 MTIs
export const ISO8583_MTIS = [
  { mti: '0100', description: 'Authorization Request' },
  { mti: '0110', description: 'Authorization Response' },
  { mti: '0200', description: 'Financial Request' },
  { mti: '0210', description: 'Financial Response' },
  { mti: '0220', description: 'Financial Advice' },
  { mti: '0230', description: 'Financial Advice Response' },
  { mti: '0400', description: 'Reversal Request' },
  { mti: '0410', description: 'Reversal Response' },
  { mti: '0420', description: 'Reversal Advice' },
  { mti: '0430', description: 'Reversal Advice Response' },
  { mti: '0800', description: 'Network Management Request' },
  { mti: '0810', description: 'Network Management Response' },
];

// Common ISO8583 field numbers
export const ISO8583_FIELDS = [
  { field: 2, name: 'Primary Account Number (PAN)' },
  { field: 3, name: 'Processing Code' },
  { field: 4, name: 'Transaction Amount' },
  { field: 7, name: 'Transmission Date/Time' },
  { field: 11, name: 'System Trace Audit Number (STAN)' },
  { field: 12, name: 'Local Transaction Time' },
  { field: 13, name: 'Local Transaction Date' },
  { field: 14, name: 'Expiration Date' },
  { field: 18, name: 'Merchant Category Code (MCC)' },
  { field: 22, name: 'POS Entry Mode' },
  { field: 23, name: 'Card Sequence Number' },
  { field: 32, name: 'Acquiring Institution ID' },
  { field: 35, name: 'Track 2 Data' },
  { field: 37, name: 'Retrieval Reference Number (RRN)' },
  { field: 38, name: 'Authorization Code' },
  { field: 39, name: 'Response Code' },
  { field: 41, name: 'Card Acceptor Terminal ID' },
  { field: 42, name: 'Card Acceptor Merchant ID' },
  { field: 43, name: 'Card Acceptor Name/Location' },
  { field: 49, name: 'Currency Code' },
  { field: 52, name: 'PIN Data' },
  { field: 55, name: 'ICC Data (EMV)' },
  { field: 70, name: 'Network Management Code' },
];

@Injectable({
  providedIn: 'root'
})
export class Iso8583Service {
  private apiUrl = `${environment.apiUrl}/iso8583/endpoints`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Iso8583Endpoint[]> {
    return this.http.get<Iso8583Endpoint[]>(this.apiUrl);
  }

  getById(id: string): Observable<Iso8583Endpoint> {
    return this.http.get<Iso8583Endpoint>(`${this.apiUrl}/${id}`);
  }

  create(endpoint: Iso8583Endpoint): Observable<Iso8583Endpoint> {
    return this.http.post<Iso8583Endpoint>(this.apiUrl, endpoint);
  }

  update(id: string, endpoint: Iso8583Endpoint): Observable<Iso8583Endpoint> {
    return this.http.put<Iso8583Endpoint>(`${this.apiUrl}/${id}`, endpoint);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activate(id: string): Observable<Iso8583Endpoint> {
    return this.http.post<Iso8583Endpoint>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<Iso8583Endpoint> {
    return this.http.post<Iso8583Endpoint>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  // Helper methods
  getResponseCodes() {
    return ISO8583_RESPONSE_CODES;
  }

  getMtis() {
    return ISO8583_MTIS;
  }

  getFields() {
    return ISO8583_FIELDS;
  }
}

