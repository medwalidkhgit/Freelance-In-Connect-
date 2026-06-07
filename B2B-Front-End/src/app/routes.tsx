import { createBrowserRouter, redirect } from "react-router";
import { requireRole } from "../shared/routeGuards";

export const router = createBrowserRouter([
  {
    path: "/",
    lazy: async () => ({
      Component: (await import("../landing/App")).default,
    }),
  },
  {
    path: "/sign-in",
    lazy: async () => ({
      Component: (await import("../sign_in/app/App")).default,
    }),
  },
  {
    path: "/forgot-password",
    lazy: async () => ({
      Component: (await import("../sign_in/app/ForgotPassword")).default,
    }),
  },
  {
    path: "/sign-up",
    lazy: async () => ({
      Component: (await import("../sign_up/app/components/Layout")).Layout,
    }),
    children: [
      {
        index: true,
        lazy: async () => ({
          Component: (await import("../sign_up/app/components/SelectType")).SelectType,
        }),
      },
      {
        path: "signup/freelancer",
        lazy: async () => ({
          Component: (await import("../sign_up/app/components/FreelancerForm")).FreelancerForm,
        }),
      },
      {
        path: "signup/company",
        lazy: async () => ({
          Component: (await import("../sign_up/app/components/CompanyForm")).CompanyForm,
        }),
      },
      {
        path: "success/freelancer",
        lazy: async () => ({
          Component: (await import("../sign_up/app/components/FreelancerSuccess")).FreelancerSuccess,
        }),
      },
      {
        path: "success/company",
        lazy: async () => ({
          Component: (await import("../sign_up/app/components/CompanySuccess")).CompanySuccess,
        }),
      },
    ],
  },
  {
    path: "/admin",
    loader: requireRole("ADMIN"),
    lazy: async () => ({
      Component: (await import("../admin/app/App")).default,
    }),
  },
  {
    path: "/company",
    loader: requireRole("COMPANY"),
    lazy: async () => ({
      Component: (await import("../company/app/App")).default,
    }),
  },
  {
    path: "/freelancer",
    loader: requireRole("FREELANCER"),
    lazy: async () => ({
      Component: (await import("../freelancer/components/Layout")).Layout,
    }),
    children: [
      { index: true, loader: () => redirect("/freelancer/missions") },
      {
        path: "missions",
        lazy: async () => ({
          Component: (await import("../freelancer/pages/Missions")).Missions,
        }),
      },
      {
        path: "messages",
        lazy: async () => ({
          Component: (await import("../freelancer/pages/Messages")).Messages,
        }),
      },
      {
        path: "messages/:chatId",
        lazy: async () => ({
          Component: (await import("../freelancer/pages/Messages")).Messages,
        }),
      },
      {
        path: "profile",
        lazy: async () => ({
          Component: (await import("../freelancer/pages/Profile")).Profile,
        }),
      },
    ],
  },
]);
