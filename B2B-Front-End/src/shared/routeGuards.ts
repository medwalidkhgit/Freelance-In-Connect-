import { redirect, type LoaderFunction } from "react-router";
import { getAccessToken, hasRole, type UserRole } from "./auth";

export function requireRole(role: UserRole): LoaderFunction {
  return () => {
    if (!getAccessToken()) {
      return redirect("/sign-in");
    }

    if (!hasRole(role)) {
      return redirect("/sign-in");
    }

    return null;
  };
}
