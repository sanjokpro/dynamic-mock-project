import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ExecutionEvent } from '@/types';

export function useTraffic() {
  const [events, setEvents] = useState<ExecutionEvent[]>([]);

  useEffect(() => {
    let wsUrl = process.env.NEXT_PUBLIC_WS_URL;
    if (!wsUrl) {
      if (process.env.NODE_ENV === 'production' && typeof window !== 'undefined') {
        wsUrl = window.location.origin + '/ws-traffic';
      } else {
        wsUrl = 'http://localhost:8080/ws-traffic';
      }
    }
    const socket = new SockJS(wsUrl);
    const client = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        client.subscribe('/topic/traffic', (message) => {
          const event = JSON.parse(message.body) as ExecutionEvent;
          setEvents((prev) => [event, ...prev].slice(0, 50)); // Keep last 50 events
        });
      },
      debug: (str) => {
        console.log(str);
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, []);

  const clearEvents = () => setEvents([]);

  return { events, clearEvents };
}
