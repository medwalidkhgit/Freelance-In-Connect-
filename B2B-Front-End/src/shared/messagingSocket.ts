import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_BASE_URL } from "./api";
import { getAccessToken } from "./auth";
import { runtimeConfig } from "./runtimeConfig";
import type { ChatMessage } from "./services";

const WS_BASE_URL = runtimeConfig("VITE_WS_BASE_URL", API_BASE_URL);
const WS_ENDPOINT = runtimeConfig("VITE_MESSAGING_WS_ENDPOINT", "/api/messaging/v1/ws");

type MessageHandler = (message: ChatMessage) => void;

export class MessagingSocket {
  private client?: Client;
  private subscription?: StompSubscription;

  connect(conversationId: string, onMessage: MessageHandler) {
    this.disconnect();

    const token = getAccessToken();
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}${WS_ENDPOINT}`),
      reconnectDelay: 5000,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: () => undefined,
      onConnect: () => {
        this.subscription = client.subscribe(`/topic/conversations/${conversationId}`, (frame: IMessage) => {
          onMessage(JSON.parse(frame.body) as ChatMessage);
        });
      },
    });

    this.client = client;
    client.activate();
  }

  send(conversationId: string, content: string) {
    if (!this.client?.connected) return false;

    this.client.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({ conversationId, content }),
    });
    return true;
  }

  disconnect() {
    this.subscription?.unsubscribe();
    this.subscription = undefined;

    if (this.client?.active) {
      void this.client.deactivate();
    }
    this.client = undefined;
  }
}
