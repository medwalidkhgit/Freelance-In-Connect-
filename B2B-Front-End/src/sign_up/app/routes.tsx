import { createBrowserRouter } from "react-router";
import { Layout } from "./components/Layout";
import { SelectType } from "./components/SelectType";
import { FreelancerForm } from "./components/FreelancerForm";
import { CompanyForm } from "./components/CompanyForm";
import { FreelancerSuccess } from "./components/FreelancerSuccess";
import { CompanySuccess } from "./components/CompanySuccess";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Layout,
    children: [
      { index: true, Component: SelectType },
      { path: "signup/freelancer", Component: FreelancerForm },
      { path: "signup/company", Component: CompanyForm },
      { path: "success/freelancer", Component: FreelancerSuccess },
      { path: "success/company", Component: CompanySuccess },
    ],
  },
]);
