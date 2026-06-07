type RuntimeConfig = {
  VITE_API_BASE_URL?: string;
  VITE_WS_BASE_URL?: string;
  VITE_MESSAGING_WS_ENDPOINT?: string;
  VITE_STRIPE_PUBLISHABLE_KEY?: string;
};

declare global {
  interface Window {
    __APP_CONFIG__?: RuntimeConfig;
  }
}

export function runtimeConfig(key: keyof RuntimeConfig, fallback?: string) {
  return window.__APP_CONFIG__?.[key] || import.meta.env[key] || fallback;
}
