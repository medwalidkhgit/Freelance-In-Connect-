const ACCESS_TOKEN_KEY = "fic_access_token";
const REFRESH_TOKEN_KEY = "fic_refresh_token";

export type UserRole = "ADMIN" | "COMPANY" | "FREELANCER";

export type AuthTokens = {
  accessToken?: string;
  refreshToken?: string;
  access_token?: string;
  refresh_token?: string;
};

type JwtPayload = {
  realm_access?: {
    roles?: string[];
  };
  email?: string;
  preferred_username?: string;
  sub?: string;
  exp?: number;
};

export function saveTokens(tokens: AuthTokens) {
  const accessToken = tokens.accessToken ?? tokens.access_token;
  const refreshToken = tokens.refreshToken ?? tokens.refresh_token;

  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getCurrentUserRoles(): UserRole[] {
  const token = getAccessToken();
  if (!token) return [];

  const payload = decodeJwtPayload(token);
  const roles = payload?.realm_access?.roles ?? [];

  return roles
    .map((role) => role.replace(/^ROLE_/, "").toUpperCase())
    .filter((role): role is UserRole =>
      role === "ADMIN" || role === "COMPANY" || role === "FREELANCER"
    );
}

export function hasRole(role: UserRole) {
  return getCurrentUserRoles().includes(role);
}

export function getDefaultPathForCurrentUser() {
  const roles = getCurrentUserRoles();
  if (roles.includes("ADMIN")) return "/admin";
  if (roles.includes("COMPANY")) return "/company";
  if (roles.includes("FREELANCER")) return "/freelancer";
  return "/sign-in";
}

export function decodeJwtPayload(token: string): JwtPayload | undefined {
  const payload = token.split(".")[1];
  if (!payload) return undefined;

  try {
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "="));
    return JSON.parse(decoded) as JwtPayload;
  } catch {
    return undefined;
  }
}
