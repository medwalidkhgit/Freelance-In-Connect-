import { apiFetch } from "./api";
import type { AuthTokens } from "./auth";

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginOtpRequiredResponse = {
  otpRequired: true;
  challengeId: string;
  email: string;
  message: string;
  expiresInSeconds: number;
};

export type LoginResponse = AuthTokens | LoginOtpRequiredResponse;

export type LoginOtpVerifyRequest = {
  challengeId: string;
  otp: string;
};

export type LoginOtpResendRequest = {
  challengeId: string;
};

export type ForgotPasswordRequest = {
  email: string;
};

export type VerifyOtpRequest = {
  email: string;
  otp: string;
};

export type ResetPasswordRequest = {
  email: string;
  otp: string;
  newPassword: string;
  confirmPassword: string;
};

export type FreelancerRegisterRequest = {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  summary?: string;
  password: string;
  cvUrl?: string;
  pfpUrl?: string;
};

export type FreelancerUpdateRequest = {
  firstName?: string;
  lastName?: string;
  phone?: string;
  summary?: string;
  cvUrl?: string | null;
  pfpUrl?: string | null;
  skills?: string[];
};

export type CompanyRegisterRequest = {
  email: string;
  password: string;
  companyName: string;
  siret: string;
  contactFirstName: string;
  contactLastName: string;
  companyAddress?: string;
  companyPhone?: string;
  domaine?: string;
  pfpUrl?: string;
};

export type CompanyUpdateRequest = {
  companyName?: string;
  contactFirstName?: string;
  contactLastName?: string;
  companyAddress?: string;
  companyPhone?: string;
  domaine?: string;
  pfpUrl?: string;
};

export type CompanyProfile = {
  id: number;
  keycloakId: string;
  companyEmail: string;
  companyName: string;
  siret: string;
  contactFirstName?: string;
  contactLastName?: string;
  companyAddress?: string;
  companyPhone?: string;
  companyFax?: string;
  domaine?: string;
  pfpUrl?: string;
  status: "Pending" | "Validated" | "Rejected" | "Suspended" | string;
  rejectionReason?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type CompanyPublicProfile = {
  id: number;
  companyName: string;
  domaine?: string;
  pfpUrl?: string;
};

export type MediaUploadResponse = {
  container: string;
  blobName: string;
  url: string;
  signedUrl: string;
  contentType: string;
  size: number;
};

export type SignedUrlResponse = {
  container: string;
  blobName: string;
  url: string;
  signedUrl: string;
  expiresInMinutes: number;
};

export type FreelancerProfile = {
  id: number;
  keycloakUserId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  summary?: string;
  cvUrl?: string;
  pfpUrl?: string;
  skills?: string[];
  experiences?: string[];
  projects?: string[];
  suspended?: boolean;
};

export type Mission = {
  id: number;
  companyId: number;
  title: string;
  description: string;
  requiredSkills?: string[];
  durationDays?: number;
  budget?: number;
  workMode?: "REMOTE" | "PRESENTIEL" | "HYBRIDE";
  status?: "BROUILLON" | "PUBLIEE" | "EN_COURS" | "CLOTUREE";
  createdAt?: string;
  updatedAt?: string;
};

export type ApplicationRequest = {
  missionId: number;
  coverLetter: string;
};

export type ApplicationResponse = {
  id: number;
  missionId: number;
  missionCompanyId: number;
  freelancerId?: number;
  freelancerKeycloakId?: string;
  freelancerFullname: string;
  coverLetter: string;
  compatibilityScore: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
};

export type MissionRequest = {
  companyId?: number;
  title: string;
  description: string;
  requiredSkills: string[];
  durationDays: number;
  budget: number;
  workMode: "REMOTE" | "PRESENTIEL" | "HYBRIDE";
};

export type AdminProfile = {
  id: number;
  username: string;
  email?: string;
};

export type AdminCompany = {
  id: number;
  keycloakId: string;
  companyEmail: string;
  companyName: string;
  companyAddress?: string;
  companyPhone?: string;
  companyFax?: string;
  domaine?: string;
  status?: string;
  rejectionReason?: string;
};

export type AdminFreelancer = {
  id: number;
  keycloakId: string;
  fullname: string;
  email: string;
  suspended: boolean;
};

export type AdminMission = Mission;

export type AuditLog = {
  id: number;
  adminUsername: string;
  action: string;
  targetAccountType: string;
  targetAccountId: number;
  reason?: string;
  timestamp?: string;
};

export type Conversation = {
  id: string;
  missionId: number;
  companyId: number;
  freelancerId: number;
  companyKeycloakId?: string;
  freelancerKeycloakId?: string;
  createdAt?: string;
};

export type CreateConversationRequest = {
  missionId: number;
  companyId: number;
  freelancerId: number;
  companyKeycloakId?: string;
  freelancerKeycloakId?: string;
};

export type ChatMessage = {
  id: string;
  conversationId: string;
  senderId?: number;
  senderKeycloakId?: string;
  senderRole: "COMPANY" | "FREELANCER" | "ADMIN" | string;
  content: string;
  sentAt?: string;
};

export type CreatePaymentRequest = {
  companyId: string;
  freelancerId: string;
  amountCents: number;
  currency?: string;
};

export type PaymentResponse = {
  id: string;
  status: string;
  clientSecret?: string;
  amountCents: number;
  platformFeeCents: number;
  netAmountCents: number;
  currency: string;
  stripePaymentIntentId?: string;
};

export type StripeAccountResponse = {
  accountId: string;
  onboardingUrl: string;
};

export function login(payload: LoginRequest) {
  return apiFetch<LoginResponse>("/api/auth/v1/login", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function verifyLoginOtp(payload: LoginOtpVerifyRequest) {
  return apiFetch<AuthTokens>("/api/auth/v1/login/verify-otp", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function resendLoginOtp(payload: LoginOtpResendRequest) {
  return apiFetch<string>("/api/auth/v1/login/resend-otp", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function requestPasswordReset(payload: ForgotPasswordRequest) {
  return apiFetch<string>("/api/auth/v1/password/forgot", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function verifyPasswordResetOtp(payload: VerifyOtpRequest) {
  return apiFetch<string>("/api/auth/v1/password/verify-otp", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function resetPassword(payload: ResetPasswordRequest) {
  return apiFetch<string>("/api/auth/v1/password/reset", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function registerFreelancer(payload: FreelancerRegisterRequest) {
  return apiFetch<string>("/api/auth/v1/freelancer/register", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function registerCompany(payload: CompanyRegisterRequest) {
  return apiFetch<string>("/api/auth/v1/company/register", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function updateMyFreelancerProfile(payload: FreelancerUpdateRequest) {
  return apiFetch<unknown>("/api/freelancers/v1/me", {
    method: "PUT",
    body: payload,
  });
}

export function getMyFreelancerProfile() {
  return apiFetch<FreelancerProfile>("/api/freelancers/v1/me");
}

export function getFreelancerPublicProfile(id: number) {
  return apiFetch<FreelancerProfile>(`/api/freelancers/v1/${id}`);
}

export function updateMyCompanyProfile(payload: CompanyUpdateRequest) {
  return apiFetch<unknown>("/api/companies/v1/me", {
    method: "PUT",
    body: payload,
  });
}

export function getMyCompanyProfile() {
  return apiFetch<CompanyProfile>("/api/companies/v1/me");
}

export function getCompanyPublicProfile(id: number) {
  return apiFetch<CompanyPublicProfile>(`/api/companies/v1/${id}`, {
    auth: false,
  });
}

export function listPublicCompanies() {
  return apiFetch<CompanyPublicProfile[]>("/api/companies/v1", {
    auth: false,
  });
}

export function uploadProfilePicture(file: File) {
  return uploadMedia("/api/media/v1/profile-pictures", file);
}

export function uploadCompanyLogo(file: File) {
  return uploadMedia("/api/media/v1/company-logos", file);
}

export function uploadCv(file: File) {
  return uploadMedia("/api/media/v1/cvs", file);
}

export function uploadRegistrationProfilePicture(file: File) {
  return uploadMedia("/api/media/v1/profile-pictures", file, false);
}

export function uploadRegistrationCompanyLogo(file: File) {
  return uploadMedia("/api/media/v1/company-logos", file, false);
}

export function uploadRegistrationCv(file: File) {
  return uploadMedia("/api/media/v1/cvs", file, false);
}

export function createMediaReadUrl(url: string) {
  return apiFetch<SignedUrlResponse>(`/api/media/v1/read-url?url=${encodeURIComponent(url)}`);
}

export function listPublishedMissions() {
  return apiFetch<Mission[]>("/api/missions/v1/publiees", {
    auth: false,
  });
}

export function searchMissions(params: {
  skill?: string;
  keyword?: string;
  workMode?: string;
  minBudget?: number;
  maxBudget?: number;
}) {
  const searchParams = new URLSearchParams();
  if (params.skill) searchParams.set("skill", params.skill);
  if (params.keyword) searchParams.set("keyword", params.keyword);
  if (params.workMode) searchParams.set("workMode", params.workMode);
  if (params.minBudget !== undefined) searchParams.set("minBudget", String(params.minBudget));
  if (params.maxBudget !== undefined) searchParams.set("maxBudget", String(params.maxBudget));

  const suffix = searchParams.toString();
  return apiFetch<Mission[]>(`/api/missions/v1/search${suffix ? `?${suffix}` : ""}`, {
    auth: false,
  });
}

export function applyToMission(payload: ApplicationRequest) {
  return apiFetch<ApplicationResponse>("/api/applications/v1", {
    method: "POST",
    body: payload,
  });
}

export function getMyApplications() {
  return apiFetch<ApplicationResponse[]>("/api/applications/v1/me");
}

export function listMissionsByCompany(companyId: number) {
  return apiFetch<Mission[]>(`/api/missions/v1/company/${companyId}`);
}

export function listMyCompanyMissions() {
  return apiFetch<Mission[]>("/api/companies/v1/missions");
}

export function createCompanyMission(payload: MissionRequest) {
  return apiFetch<Mission>("/api/companies/v1/missions", {
    method: "POST",
    body: payload,
  });
}

export function updateCompanyMission(id: number, payload: MissionRequest) {
  return apiFetch<Mission>(`/api/companies/v1/missions/${id}`, {
    method: "PUT",
    body: payload,
  });
}

export function deleteCompanyMission(id: number) {
  return apiFetch<void>(`/api/companies/v1/missions/${id}`, {
    method: "DELETE",
  });
}

export function publishCompanyMission(id: number) {
  return apiFetch<Mission>(`/api/companies/v1/missions/${id}/publier`, {
    method: "POST",
  });
}

export function startCompanyMission(id: number) {
  return apiFetch<Mission>(`/api/companies/v1/missions/${id}/demarrer`, {
    method: "POST",
  });
}

export function closeCompanyMission(id: number) {
  return apiFetch<Mission>(`/api/companies/v1/missions/${id}/cloturer`, {
    method: "POST",
  });
}

export function getApplicationsForCurrentCompany() {
  return apiFetch<ApplicationResponse[]>("/api/applications/v1/company/me");
}

export function getApplicationsForMission(missionId: number) {
  return apiFetch<ApplicationResponse[]>(`/api/applications/v1/mission/${missionId}`);
}

export function updateApplicationStatus(id: number, status: "ACCEPTED" | "REJECTED" | "WAITLISTED" | "PENDING") {
  return apiFetch<ApplicationResponse>(`/api/applications/v1/${id}/status`, {
    method: "PUT",
    body: { status },
  });
}

export function getAdminProfile() {
  return apiFetch<AdminProfile>("/api/admin/v1");
}

export function updateAdminProfile(payload: AdminProfile) {
  return apiFetch<AdminProfile>("/api/admin/v1", {
    method: "PUT",
    body: payload,
  });
}

export function getPendingCompaniesForAdmin(all = false) {
  return apiFetch<AdminCompany[]>(`/api/admin/v1/companies/pending${all ? "?all=true" : ""}`);
}

export function getCompaniesForAdmin() {
  return apiFetch<AdminCompany[]>("/api/admin/v1/companies");
}

export function approveCompany(companyId: number) {
  return apiFetch<void>(`/api/admin/v1/companies/${companyId}/approve`, {
    method: "POST",
  });
}

export function rejectCompany(companyId: number, reason: string) {
  return apiFetch<AdminCompany>(`/api/admin/v1/companies/${companyId}/reject?reason=${encodeURIComponent(reason)}`, {
    method: "POST",
  });
}

export function suspendCompany(companyId: number, reason: string) {
  return apiFetch<AdminCompany>(`/api/admin/v1/companies/${companyId}/suspend?reason=${encodeURIComponent(reason)}`, {
    method: "POST",
  });
}

export function deleteCompanyAdmin(companyId: number, reason: string) {
  return apiFetch<void>(`/api/admin/v1/companies/${companyId}?reason=${encodeURIComponent(reason)}`, {
    method: "DELETE",
  });
}

export function getFreelancersForAdmin() {
  return apiFetch<AdminFreelancer[]>("/api/admin/v1/freelancers");
}

export function getMissionsForAdmin() {
  return apiFetch<AdminMission[]>("/api/admin/v1/missions");
}

export function getCompanyMissionsForAdmin(companyId: number) {
  return apiFetch<AdminMission[]>(`/api/admin/v1/companies/${companyId}/missions`);
}

export function suspendFreelancer(freelancerId: number, reason: string) {
  return apiFetch<void>(`/api/admin/v1/freelancers/${freelancerId}/suspend?reason=${encodeURIComponent(reason)}`, {
    method: "POST",
  });
}

export function deleteFreelancerAdmin(freelancerId: number, reason: string) {
  return apiFetch<void>(`/api/admin/v1/freelancers/${freelancerId}?reason=${encodeURIComponent(reason)}`, {
    method: "DELETE",
  });
}

export function getAuditLogs() {
  return apiFetch<AuditLog[]>("/api/admin/v1/audit");
}

export function createMissionPayment(missionId: number, payload: CreatePaymentRequest) {
  return apiFetch<PaymentResponse>(`/api/payments/v1/payments/mission/${missionId}`, {
    method: "POST",
    body: payload,
  });
}

export function getPayment(paymentId: string) {
  return apiFetch<PaymentResponse>(`/api/payments/v1/payments/${paymentId}`);
}

export function createFreelancerStripeAccount(freelancerKeycloakId: string, email?: string) {
  return apiFetch<StripeAccountResponse>(`/api/payments/v1/stripe/accounts/freelancers/${freelancerKeycloakId}`, {
    method: "POST",
    body: { email },
  });
}

export function listConversations() {
  return apiFetch<Conversation[]>("/api/messaging/v1/conversations");
}

export function createConversation(payload: CreateConversationRequest) {
  return apiFetch<Conversation>("/api/messaging/v1/conversations", {
    method: "POST",
    body: payload,
  });
}

export function getConversationMessages(conversationId: string) {
  return apiFetch<ChatMessage[]>(`/api/messaging/v1/messages/conversations/${conversationId}`);
}

export function sendChatMessage(conversationId: string, content: string) {
  return apiFetch<ChatMessage>("/api/messaging/v1/messages", {
    method: "POST",
    body: { conversationId, content },
  });
}

function uploadMedia(path: string, file: File, auth = true) {
  const formData = new FormData();
  formData.append("file", file);

  return apiFetch<MediaUploadResponse>(path, {
    method: "POST",
    auth,
    body: formData,
  });
}
