import { getAccessToken } from "./auth";
import { runtimeConfig } from "./runtimeConfig";

export const API_BASE_URL = runtimeConfig("VITE_API_BASE_URL", "http://localhost:8280");

export class ApiError extends Error {
  status: number;
  payload: unknown;

  constructor(status: number, message: string, payload: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

type ApiBody = BodyInit | Record<string, unknown> | unknown[];

type ApiFetchOptions = Omit<RequestInit, "body"> & {
  auth?: boolean;
  body?: ApiBody;
};

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const { auth = true, body, headers, ...requestOptions } = options;
  const requestHeaders = new Headers(headers);
  let requestBody: BodyInit | undefined;

  if (body instanceof FormData || body instanceof Blob || typeof body === "string") {
    requestBody = body;
  } else if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    requestBody = JSON.stringify(body);
  }

  if (auth) {
    const token = getAccessToken();
    if (token) {
      requestHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestOptions,
    headers: requestHeaders,
    body: requestBody,
  });

  const contentType = response.headers.get("content-type") ?? "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message =
      typeof payload === "string"
        ? payload
        : getPayloadMessage(payload) ?? `HTTP ${response.status}`;
    throw new ApiError(response.status, message, payload);
  }

  return payload as T;
}

function getPayloadMessage(payload: unknown) {
  if (!payload || typeof payload !== "object") return undefined;
  if ("message" in payload && typeof payload.message === "string") return payload.message;
  if ("error" in payload && typeof payload.error === "string") return payload.error;
  if ("description" in payload && typeof payload.description === "string") return payload.description;
  return undefined;
}
