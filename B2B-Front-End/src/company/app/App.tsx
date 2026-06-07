import { useEffect, useMemo, useRef, useState } from "react";
import "../styles/index.css";
import logoImage from "../../assets/PFS_Logo.jpg";
import {
  Bell,
  Briefcase,
  Building,
  CheckCheck,
  CheckCircle2,
  Clock,
  CreditCard,
  FileText,
  LayoutDashboard,
  Loader2,
  LogOut,
  MessageSquare,
  Plus,
  Save,
  Search,
  Send,
  Settings,
  Trash2,
  Users,
  X,
} from "lucide-react";
import { ApiError } from "../../shared/api";
import { clearTokens, getAccessToken } from "../../shared/auth";
import { MessagingSocket } from "../../shared/messagingSocket";
import {
  closeCompanyMission,
  createConversation,
  createCompanyMission,
  createMediaReadUrl,
  createMissionPayment,
  deleteCompanyMission,
  getApplicationsForCurrentCompany,
  getConversationMessages,
  getFreelancerPublicProfile,
  getMyCompanyProfile,
  listConversations,
  listMyCompanyMissions,
  publishCompanyMission,
  sendChatMessage,
  startCompanyMission,
  updateApplicationStatus,
  updateCompanyMission,
  updateMyCompanyProfile,
  uploadCompanyLogo,
  type ApplicationResponse,
  type ChatMessage,
  type CompanyProfile,
  type Conversation,
  type FreelancerProfile,
  type Mission,
  type MissionRequest,
  type PaymentResponse,
} from "../../shared/services";

type Page = "Dashboard" | "Missions" | "Candidatures" | "Messages" | "Company Profile";

type MissionForm = {
  title: string;
  description: string;
  requiredSkills: string;
  durationDays: string;
  budget: string;
  workMode: "REMOTE" | "PRESENTIEL" | "HYBRIDE";
  initialStatus: "BROUILLON" | "PUBLIEE";
};

type ProfileForm = {
  companyName: string;
  contactFirstName: string;
  contactLastName: string;
  companyAddress: string;
  companyPhone: string;
  domaine: string;
};

type StripeCardElement = {
  mount: (target: HTMLElement) => void;
  destroy: () => void;
};

type StripeElements = {
  create: (type: "card", options?: Record<string, unknown>) => StripeCardElement;
};

type StripeClient = {
  elements: () => StripeElements;
  confirmCardPayment: (
    clientSecret: string,
    options: { payment_method: { card: StripeCardElement } }
  ) => Promise<{ error?: { message?: string }; paymentIntent?: { status?: string } }>;
};

declare global {
  interface Window {
    Stripe?: (publishableKey: string) => StripeClient;
  }
}

const emptyMissionForm: MissionForm = {
  title: "",
  description: "",
  requiredSkills: "",
  durationDays: "30",
  budget: "1000",
  workMode: "REMOTE",
  initialStatus: "BROUILLON",
};

export default function App() {
  const [activePage, setActivePage] = useState<Page>("Dashboard");
  const [profile, setProfile] = useState<CompanyProfile | null>(null);
  const [logoUrl, setLogoUrl] = useState<string | undefined>();
  const [missions, setMissions] = useState<Mission[]>([]);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [freelancersById, setFreelancersById] = useState<Record<number, FreelancerProfile>>({});
  const [missionForm, setMissionForm] = useState<MissionForm>(emptyMissionForm);
  const [profileForm, setProfileForm] = useState<ProfileForm>({
    companyName: "",
    contactFirstName: "",
    contactLastName: "",
    companyAddress: "",
    companyPhone: "",
    domaine: "",
  });
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [selectedMissionId, setSelectedMissionId] = useState<number | "all">("all");
  const [paymentsByApplication, setPaymentsByApplication] = useState<Record<number, PaymentResponse>>({});
  const [paymentDialog, setPaymentDialog] = useState<{
    application: ApplicationResponse;
    mission: Mission;
    payment: PaymentResponse;
  } | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [missionFormMessage, setMissionFormMessage] = useState("");

  useEffect(() => {
    void loadCompanySpace();
  }, []);

  const filteredApplications = useMemo(() => {
    if (selectedMissionId === "all") return applications;
    return applications.filter((item) => item.missionId === selectedMissionId);
  }, [applications, selectedMissionId]);

  const stats = useMemo(() => {
    return {
      total: missions.length,
      published: missions.filter((item) => item.status === "PUBLIEE").length,
      active: missions.filter((item) => item.status === "EN_COURS").length,
      closed: missions.filter((item) => item.status === "CLOTUREE").length,
      applications: applications.length,
    };
  }, [applications.length, missions]);

  const loadCompanySpace = async () => {
    setIsLoading(true);
    setMessage("");

    try {
      const company = await getMyCompanyProfile();
      setProfile(company);
      if (company.pfpUrl) {
        const signedLogo = await createMediaReadUrl(company.pfpUrl).catch(() => undefined);
        setLogoUrl(signedLogo?.signedUrl ?? company.pfpUrl);
      } else {
        setLogoUrl(undefined);
      }
      setProfileForm({
        companyName: company.companyName ?? "",
        contactFirstName: company.contactFirstName ?? "",
        contactLastName: company.contactLastName ?? "",
        companyAddress: company.companyAddress ?? "",
        companyPhone: company.companyPhone ?? "",
        domaine: company.domaine ?? "",
      });

      const [companyMissions, companyApplications] = await Promise.all([
        listMyCompanyMissions(),
        getApplicationsForCurrentCompany(),
      ]);
      setMissions(companyMissions);
      setApplications(companyApplications);
      await loadFreelancerProfiles(companyApplications);
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Impossible de charger l'espace entreprise.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    clearTokens();
    window.location.href = "/sign-in";
  };

  const handleCreateMission = async () => {
    const missionRequest = toMissionRequest(missionForm);
    const validationMessage = validateMissionRequest(missionRequest);
    if (validationMessage) {
      setMessage(validationMessage);
      setMissionFormMessage(validationMessage);
      return;
    }

    setIsSaving(true);
    setMessage("");
    setMissionFormMessage("");

    try {
      const createdMission = await createCompanyMission(missionRequest);
      setMissions((current) => [createdMission, ...current.filter((item) => item.id !== createdMission.id)]);

      if (missionForm.initialStatus === "PUBLIEE") {
        try {
          const publishedMission = await publishCompanyMission(createdMission.id);
          setMissions((current) => [publishedMission, ...current.filter((item) => item.id !== publishedMission.id)]);
          setMessage("Mission creee et publiee.");
          setMissionFormMessage("Mission creee et publiee.");
        } catch (err) {
          const errorMessage = getApiErrorMessage(err, "Publication de la mission impossible.");
          const partialMessage = `Mission creee en brouillon, mais publication impossible : ${errorMessage}`;
          setMessage(partialMessage);
          setMissionFormMessage(partialMessage);
        }
      } else {
        setMessage("Mission creee.");
        setMissionFormMessage("Mission creee.");
      }

      setMissionForm(emptyMissionForm);
    } catch (err) {
      const errorMessage = getApiErrorMessage(err, "Creation de mission impossible.");
      setMessage(errorMessage);
      setMissionFormMessage(errorMessage);
    } finally {
      setIsSaving(false);
    }
  };

  const runMissionAction = async (action: () => Promise<unknown>, successMessage: string) => {
    setIsSaving(true);
    setMessage("");

    try {
      await action();
      setMessage(successMessage);
      await loadCompanySpace();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Action impossible pour le moment.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveProfile = async () => {
    setIsSaving(true);
    setMessage("");

    try {
      const logoUpload = logoFile ? await uploadCompanyLogo(logoFile) : undefined;
      await updateMyCompanyProfile({
        companyName: profileForm.companyName,
        contactFirstName: profileForm.contactFirstName,
        contactLastName: profileForm.contactLastName,
        companyAddress: profileForm.companyAddress,
        companyPhone: profileForm.companyPhone,
        domaine: profileForm.domaine,
        pfpUrl: logoUpload?.url,
      });
      setLogoFile(null);
      setMessage("Profil entreprise mis a jour.");
      await loadCompanySpace();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Mise a jour du profil impossible.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleCreatePayment = async (application: ApplicationResponse) => {
    if (!profile?.keycloakId) {
      setMessage("Identifiant Keycloak company introuvable.");
      return;
    }
    if (!application.freelancerKeycloakId) {
      setMessage("Identifiant Keycloak freelancer introuvable sur cette candidature.");
      return;
    }

    const mission = missions.find((item) => item.id === application.missionId);
    if (!mission?.budget) {
      setMessage("Budget mission introuvable pour initialiser le paiement.");
      return;
    }

    const existingPayment = paymentsByApplication[application.id];
    if (isSuccessfulPayment(existingPayment?.status)) {
      setMessage("Cette candidature est deja payee.");
      return;
    }
    if (existingPayment?.clientSecret) {
      setPaymentDialog({ application, mission, payment: existingPayment });
      setMessage("");
      return;
    }

    setIsSaving(true);
    setMessage("");

    try {
      const payment = await createMissionPayment(application.missionId, {
        companyId: profile.keycloakId,
        freelancerId: application.freelancerKeycloakId,
        amountCents: Math.round(mission.budget * 100),
        currency: "mad",
      });
      setPaymentsByApplication((current) => ({ ...current, [application.id]: payment }));
      if (!payment.clientSecret) {
        setMessage("Paiement initialise, mais Stripe n'a pas retourne de client secret.");
        return;
      }
      setPaymentDialog({ application, mission, payment });
      setMessage("");
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Initialisation du paiement impossible.");
    } finally {
      setIsSaving(false);
    }
  };

  const loadFreelancerProfiles = async (applicationList: ApplicationResponse[]) => {
    const ids = Array.from(
      new Set(applicationList.map((application) => application.freelancerId).filter((id): id is number => typeof id === "number"))
    );
    if (ids.length === 0) return;

    const entries = await Promise.all(
      ids.map(async (freelancerId) => {
        try {
          const freelancer = await getFreelancerPublicProfile(freelancerId);
          const [signedPhoto, signedCv] = await Promise.all([
            freelancer.pfpUrl ? createMediaReadUrl(freelancer.pfpUrl).catch(() => undefined) : Promise.resolve(undefined),
            freelancer.cvUrl ? createMediaReadUrl(freelancer.cvUrl).catch(() => undefined) : Promise.resolve(undefined),
          ]);
          return [
            freelancerId,
            {
              ...freelancer,
              pfpUrl: signedPhoto?.signedUrl ?? freelancer.pfpUrl,
              cvUrl: signedCv?.signedUrl ?? freelancer.cvUrl,
            },
          ] as const;
        } catch {
          return undefined;
        }
      })
    );

    setFreelancersById((current) => ({
      ...current,
      ...Object.fromEntries(entries.filter(Boolean) as Array<readonly [number, FreelancerProfile]>),
    }));
  };

  if (isLoading) {
    return (
      <Shell activePage={activePage} setActivePage={setActivePage} profile={profile} onLogout={handleLogout}>
        <div className="flex h-full items-center justify-center text-slate-500">
          <Loader2 className="mr-2 animate-spin" size={20} />
          Chargement de l'espace entreprise...
        </div>
      </Shell>
    );
  }

  return (
    <Shell activePage={activePage} setActivePage={setActivePage} profile={profile} onLogout={handleLogout}>
      <div className="max-w-7xl mx-auto w-full pb-8 space-y-6">
        {message && (
          <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
            {message}
          </div>
        )}

        {activePage === "Dashboard" && (
          <>
            <HeaderCard profile={profile} logoUrl={logoUrl} />
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
              <Kpi label="Total Missions" value={stats.total} />
              <Kpi label="Published" value={stats.published} />
              <Kpi label="In Progress" value={stats.active} />
              <Kpi label="Closed" value={stats.closed} />
              <Kpi label="Applications" value={stats.applications} highlight />
            </div>
            <MissionList missions={missions.slice(0, 5)} applications={applications} onAction={runMissionAction} />
          </>
        )}

        {activePage === "Missions" && (
          <div className="grid grid-cols-1 xl:grid-cols-[420px_1fr] gap-6">
            <MissionCreateForm
              form={missionForm}
              setForm={setMissionForm}
              onSubmit={handleCreateMission}
              isSaving={isSaving}
              statusMessage={missionFormMessage}
            />
            <MissionList missions={missions} applications={applications} onAction={runMissionAction} />
          </div>
        )}

        {activePage === "Candidatures" && (
          <ApplicationsView
            missions={missions}
            applications={filteredApplications}
            freelancersById={freelancersById}
            selectedMissionId={selectedMissionId}
            setSelectedMissionId={setSelectedMissionId}
            paymentsByApplication={paymentsByApplication}
            onStatusChange={(id, status) =>
              runMissionAction(() => updateApplicationStatus(id, status), "Statut de candidature mis a jour.")
            }
            onPay={(application) => void handleCreatePayment(application)}
          />
        )}

        {activePage === "Messages" && (
          <CompanyMessagesView
            profile={profile}
            missions={missions}
            applications={applications}
          />
        )}

          {activePage === "Company Profile" && (
            <ProfileView
              profile={profile}
            logoUrl={logoUrl}
            form={profileForm}
            setForm={setProfileForm}
            setLogoFile={setLogoFile}
            onSave={handleSaveProfile}
            isSaving={isSaving}
            />
          )}

          {paymentDialog && (
            <PaymentDialog
              payment={paymentDialog.payment}
              mission={paymentDialog.mission}
              freelancerName={paymentDialog.application.freelancerFullname}
              onClose={() => setPaymentDialog(null)}
              onConfirmed={(status) => {
                setPaymentsByApplication((current) => ({
                  ...current,
                  [paymentDialog.application.id]: {
                    ...paymentDialog.payment,
                    status,
                  },
                }));
                setPaymentDialog(null);
                setMessage(`Paiement Stripe confirme: ${formatMoney(paymentDialog.payment.amountCents, paymentDialog.payment.currency)}.`);
              }}
            />
          )}
        </div>
      </Shell>
    );
  }

type ShellProps = {
  activePage: Page;
  setActivePage: (page: Page) => void;
  profile: CompanyProfile | null;
  onLogout: () => void;
  children: React.ReactNode;
};

function Shell({ activePage, setActivePage, profile, onLogout, children }: ShellProps) {
  const navItems: Array<{ page: Page; icon: typeof LayoutDashboard; label: string }> = [
    { page: "Dashboard", icon: LayoutDashboard, label: "Dashboard" },
    { page: "Missions", icon: Briefcase, label: "Missions" },
    { page: "Candidatures", icon: Users, label: "Candidatures" },
    { page: "Messages", icon: MessageSquare, label: "Messages" },
    { page: "Company Profile", icon: Building, label: "Company Profile" },
  ];

  return (
    <div className="flex h-screen bg-[#CFEFF3] font-sans selection:bg-[#3B82F6] selection:text-white overflow-hidden">
      <aside className="w-[280px] bg-white border-r border-slate-100 flex flex-col h-screen sticky top-0 shrink-0 shadow-[4px_0_24px_rgb(0,0,0,0.02)] z-40">
        <div className="h-20 flex items-center px-6 border-b border-slate-100/60 shrink-0">
          <div className="flex items-center gap-2.5">
            <img src={logoImage} alt="FIC Logo" className="w-10 h-10 rounded-xl object-contain" />
            <span className="font-extrabold text-2xl tracking-tight text-slate-900">FIC</span>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto py-8 px-4 flex flex-col gap-2">
          {navItems.map(({ page, icon: Icon, label }) => (
            <button
              key={page}
              onClick={() => setActivePage(page)}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all ${
                activePage === page
                  ? "bg-[#3B82F6] text-white shadow-md shadow-blue-500/20"
                  : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
              }`}
            >
              <Icon size={20} />
              {label}
            </button>
          ))}
        </nav>

        <div className="p-4 border-t border-slate-100/60 shrink-0">
          <button
            onClick={onLogout}
            className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm font-semibold text-red-600 transition-colors hover:bg-red-100"
          >
            <LogOut size={16} />
            Déconnexion
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col h-full min-w-0 relative">
        <header className="bg-white sticky top-0 z-30 border-b border-slate-100 px-8 h-20 flex items-center justify-between shadow-[0_4px_24px_rgb(0,0,0,0.02)] shrink-0">
          <span className="text-xl font-bold text-slate-800">{activePage}</span>
          <div className="flex items-center gap-6">
            <div className="hidden sm:flex items-center gap-3 pr-6 border-r border-slate-200">
              <div className="w-9 h-9 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-center text-slate-600 shadow-sm">
                <Building size={18} />
              </div>
              <div className="flex flex-col">
                <span className="text-xs font-medium text-slate-400 leading-none mb-1">Company</span>
                <span className="font-bold text-sm text-slate-800 leading-none">{profile?.companyName ?? "Company"}</span>
              </div>
            </div>
            <button className="text-slate-400 hover:text-slate-700 relative p-2.5 transition-colors rounded-full hover:bg-slate-50">
              <Bell size={22} />
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6 md:p-8 relative">{children}</main>
      </div>
    </div>
  );
}

function HeaderCard({ profile, logoUrl }: { profile: CompanyProfile | null; logoUrl?: string }) {
  return (
    <div className="bg-white rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] p-6 md:p-8 flex flex-col md:flex-row md:items-center justify-between gap-6">
      <div className="flex items-center gap-5">
        {logoUrl ? (
          <img src={logoUrl} alt={profile?.companyName ?? "Company"} className="w-16 h-16 rounded-2xl object-cover bg-slate-100" />
        ) : (
          <div className="w-16 h-16 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xl">
            {profile?.companyName?.slice(0, 2).toUpperCase() ?? "CO"}
          </div>
        )}
        <div>
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-3xl font-bold text-slate-900 tracking-tight">{profile?.companyName ?? "Company"}</h1>
            <span className="px-3 py-1 bg-emerald-50 text-emerald-600 text-xs font-bold rounded-full border border-emerald-200 uppercase tracking-wide">
              {profile?.status ?? "Pending"}
            </span>
          </div>
          <p className="text-sm text-slate-500 font-medium">{profile?.companyEmail}</p>
        </div>
      </div>
    </div>
  );
}

function Kpi({ label, value, highlight = false }: { label: string; value: number; highlight?: boolean }) {
  return (
    <div className="bg-white rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] p-5 flex flex-col justify-center gap-1">
      <span className="text-sm font-semibold text-slate-500">{label}</span>
      <span className={`text-3xl font-bold tracking-tight ${highlight ? "text-[#8B5CF6]" : "text-slate-800"}`}>{value}</span>
    </div>
  );
}

function MissionCreateForm({
  form,
  setForm,
  onSubmit,
  isSaving,
  statusMessage,
}: {
  form: MissionForm;
  setForm: (form: MissionForm) => void;
  onSubmit: () => Promise<void>;
  isSaving: boolean;
  statusMessage: string;
}) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 p-6 space-y-4">
      <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
        <Plus size={20} /> Creer une mission
      </h2>
      <TextInput label="Titre" value={form.title} onChange={(value) => setForm({ ...form, title: value })} />
      <div className="space-y-2">
        <label className="text-sm font-medium text-slate-700">Description</label>
        <textarea
          value={form.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })}
          className="min-h-28 w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
        />
      </div>
      <TextInput label="Competences" value={form.requiredSkills} onChange={(value) => setForm({ ...form, requiredSkills: value })} helper="Separez par des virgules." />
      <div className="grid grid-cols-2 gap-4">
        <TextInput label="Duree jours" value={form.durationDays} onChange={(value) => setForm({ ...form, durationDays: value })} />
        <TextInput label="Budget" value={form.budget} onChange={(value) => setForm({ ...form, budget: value })} />
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium text-slate-700">Mode</label>
        <select
          value={form.workMode}
          onChange={(event) => setForm({ ...form, workMode: event.target.value as MissionForm["workMode"] })}
          className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
        >
          <option value="REMOTE">Remote</option>
          <option value="PRESENTIEL">Presentiel</option>
          <option value="HYBRIDE">Hybride</option>
        </select>
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium text-slate-700">Statut initial</label>
        <select
          value={form.initialStatus}
          onChange={(event) => setForm({ ...form, initialStatus: event.target.value as MissionForm["initialStatus"] })}
          className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
        >
          <option value="BROUILLON">Brouillon</option>
          <option value="PUBLIEE">Publiee</option>
        </select>
      </div>
      <button
        type="button"
        onClick={() => void onSubmit()}
        disabled={isSaving}
        className="w-full rounded-xl bg-blue-500 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600 disabled:bg-slate-300"
      >
        {isSaving ? "Creation..." : "Creer la mission"}
      </button>
      {statusMessage && (
        <div className="rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
          {statusMessage}
        </div>
      )}
    </div>
  );
}

function MissionList({
  missions,
  applications,
  onAction,
}: {
  missions: Mission[];
  applications: ApplicationResponse[];
  onAction: (action: () => Promise<unknown>, successMessage: string) => void;
}) {
  const [editingMissionId, setEditingMissionId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<MissionForm>(emptyMissionForm);

  const startEdit = (mission: Mission) => {
    setEditingMissionId(mission.id);
    setEditForm(toMissionForm(mission));
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 p-6 space-y-4">
      <h2 className="text-xl font-bold text-slate-900">Missions</h2>
      {missions.length === 0 ? (
        <p className="text-sm text-slate-500">Aucune mission pour le moment.</p>
      ) : (
        <div className="space-y-3">
          {missions.map((mission) => (
            <div key={mission.id} className="rounded-xl border border-slate-100 p-4 hover:shadow-sm transition-shadow">
              <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="font-bold text-slate-900">{mission.title}</h3>
                    <StatusBadge status={mission.status ?? "BROUILLON"} />
                  </div>
                  <p className="text-sm text-slate-500 line-clamp-2">{mission.description}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {(mission.requiredSkills ?? []).map((skill) => (
                      <span key={skill} className="rounded-md bg-violet-50 px-2.5 py-1 text-xs font-semibold text-violet-600">
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="flex flex-wrap gap-2 lg:justify-end">
                  {(() => {
                    const hasAcceptedApplication = applications.some(
                      (application) => application.missionId === mission.id && application.status === "ACCEPTED"
                    );
                    const canStart = mission.status === "PUBLIEE" && hasAcceptedApplication;
                    const canClose = mission.status === "EN_COURS";

                    return (
                      <>
                  <ActionButton label="Modifier" onClick={() => startEdit(mission)} />
                        {mission.status === "BROUILLON" && (
                          <ActionButton label="Publier" onClick={() => onAction(() => publishCompanyMission(mission.id), "Mission publiee.")} />
                        )}
                        {mission.status === "PUBLIEE" && (
                          <ActionButton
                            label={canStart ? "Demarrer" : "Accepter un candidat d'abord"}
                            onClick={() => {
                              if (!canStart) {
                                onAction(
                                  () => Promise.reject(new ApiError(400, "Acceptez d'abord un candidat pour demarrer cette mission.", null)),
                                  ""
                                );
                                return;
                              }
                              onAction(() => startCompanyMission(mission.id), "Mission demarree.");
                            }}
                          />
                        )}
                        {canClose && (
                          <ActionButton label="Cloturer" onClick={() => onAction(() => closeCompanyMission(mission.id), "Mission cloturee.")} />
                        )}
                  <button
                    onClick={() => onAction(() => deleteCompanyMission(mission.id), "Mission supprimee.")}
                    className="inline-flex items-center gap-1 rounded-lg bg-red-50 px-3 py-2 text-xs font-bold text-red-600 hover:bg-red-100"
                  >
                    <Trash2 size={14} /> Supprimer
                  </button>
                      </>
                    );
                  })()}
                </div>
              </div>
              {editingMissionId === mission.id && (
                <div className="mt-4 grid gap-3 rounded-xl border border-slate-100 bg-slate-50 p-4">
                  <TextInput label="Titre" value={editForm.title} onChange={(value) => setEditForm({ ...editForm, title: value })} />
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">Description</label>
                    <textarea
                      value={editForm.description}
                      onChange={(event) => setEditForm({ ...editForm, description: event.target.value })}
                      className="min-h-24 w-full rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                    />
                  </div>
                  <TextInput label="Competences" value={editForm.requiredSkills} onChange={(value) => setEditForm({ ...editForm, requiredSkills: value })} helper="Separez par des virgules." />
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <TextInput label="Duree jours" value={editForm.durationDays} onChange={(value) => setEditForm({ ...editForm, durationDays: value })} />
                    <TextInput label="Budget" value={editForm.budget} onChange={(value) => setEditForm({ ...editForm, budget: value })} />
                    <div className="space-y-2">
                      <label className="text-sm font-medium text-slate-700">Mode</label>
                      <select
                        value={editForm.workMode}
                        onChange={(event) => setEditForm({ ...editForm, workMode: event.target.value as MissionForm["workMode"] })}
                        className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                      >
                        <option value="REMOTE">Remote</option>
                        <option value="PRESENTIEL">Presentiel</option>
                        <option value="HYBRIDE">Hybride</option>
                      </select>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      onClick={() => {
                        onAction(() => updateCompanyMission(mission.id, toMissionRequest(editForm)), "Mission mise a jour.");
                        setEditingMissionId(null);
                      }}
                      className="inline-flex items-center gap-2 rounded-lg bg-blue-500 px-4 py-2 text-xs font-bold text-white hover:bg-blue-600"
                    >
                      <Save size={14} /> Enregistrer
                    </button>
                    <button
                      onClick={() => setEditingMissionId(null)}
                      className="rounded-lg bg-white px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100"
                    >
                      Annuler
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ApplicationsView({
  missions,
  applications,
  freelancersById,
  selectedMissionId,
  setSelectedMissionId,
  paymentsByApplication,
  onStatusChange,
  onPay,
}: {
  missions: Mission[];
  applications: ApplicationResponse[];
  freelancersById: Record<number, FreelancerProfile>;
  selectedMissionId: number | "all";
  setSelectedMissionId: (value: number | "all") => void;
  paymentsByApplication: Record<number, PaymentResponse>;
  onStatusChange: (id: number, status: "ACCEPTED" | "REJECTED" | "WAITLISTED" | "PENDING") => void;
  onPay: (application: ApplicationResponse) => void;
}) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 p-6 space-y-5">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
          <Users size={20} /> Candidatures
        </h2>
        <select
          value={selectedMissionId}
          onChange={(event) => setSelectedMissionId(event.target.value === "all" ? "all" : Number(event.target.value))}
          className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
        >
          <option value="all">Toutes les missions</option>
          {missions.map((mission) => (
            <option key={mission.id} value={mission.id}>
              {mission.title}
            </option>
          ))}
        </select>
      </div>
      {applications.length === 0 ? (
        <p className="text-sm text-slate-500">Aucune candidature trouvee.</p>
      ) : (
        <div className="space-y-3">
          {applications.map((application) => {
            const payment = paymentsByApplication[application.id];
            const paymentSucceeded = isSuccessfulPayment(payment?.status);
            const freelancer = application.freelancerId ? freelancersById[application.freelancerId] : undefined;
            const freelancerName = freelancerFullName(freelancer) ?? application.freelancerFullname;

            return (
              <div key={application.id} className="rounded-xl border border-slate-100 p-4">
                <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="mb-3 flex items-start gap-3">
                      <FreelancerAvatar profile={freelancer} fallback={freelancerName} />
                      <div className="min-w-0">
                        <h3 className="font-bold text-slate-900">{freelancerName}</h3>
                        <p className="text-xs text-slate-500">{freelancer?.email ?? application.freelancerKeycloakId}</p>
                        {freelancer?.phone && <p className="text-xs text-slate-500">{freelancer.phone}</p>}
                      </div>
                    </div>
                    <p className="text-sm text-slate-500">
                      {missions.find((mission) => mission.id === application.missionId)?.title ?? `Mission #${application.missionId}`}
                    </p>
                    <p className="mt-2 text-sm text-slate-600">{application.coverLetter}</p>
                    {freelancer && (
                      <FreelancerApplicationProfile profile={freelancer} />
                    )}
                    <div className="mt-2 flex items-center gap-3 text-xs font-semibold text-slate-500">
                      <span>Score: {application.compatibilityScore}%</span>
                      <StatusBadge status={application.status} />
                    </div>
                    {payment && (
                      <p className={`mt-2 text-xs font-semibold ${paymentSucceeded ? "text-emerald-600" : "text-amber-600"}`}>
                        Paiement {formatPaymentStatus(payment.status)} - {formatMoney(payment.amountCents, payment.currency)}
                      </p>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {application.status === "PENDING" && (
                      <>
                        <ActionButton label="Accepter" onClick={() => onStatusChange(application.id, "ACCEPTED")} />
                        <ActionButton label="Waitlist" onClick={() => onStatusChange(application.id, "WAITLISTED")} />
                        <button
                          onClick={() => onStatusChange(application.id, "REJECTED")}
                          className="rounded-lg bg-red-50 px-3 py-2 text-xs font-bold text-red-600 hover:bg-red-100"
                        >
                          Rejeter
                        </button>
                      </>
                    )}
                    {application.status === "ACCEPTED" && !paymentSucceeded && (
                      <button
                        onClick={() => onPay(application)}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-600 hover:bg-emerald-100"
                      >
                        <CreditCard size={14} />
                        {payment?.clientSecret ? "Finaliser paiement" : "Payer"}
                      </button>
                    )}
                    {application.status === "ACCEPTED" && paymentSucceeded && (
                      <span className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-600">
                        <CheckCircle2 size={14} />
                        Paye
                      </span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function FreelancerApplicationProfile({ profile }: { profile: FreelancerProfile }) {
  return (
    <div className="mt-4 grid gap-3 rounded-xl border border-slate-100 bg-slate-50 p-3">
      {profile.summary && (
        <p className="text-sm leading-6 text-slate-600">{profile.summary}</p>
      )}

      {profile.skills && profile.skills.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {profile.skills.map((skill) => (
            <span key={skill} className="rounded-md bg-violet-50 px-2.5 py-1 text-xs font-semibold text-violet-600">
              {skill}
            </span>
          ))}
        </div>
      )}

      {profile.experiences && profile.experiences.length > 0 && (
        <div>
          <p className="mb-1 text-xs font-bold uppercase tracking-wide text-slate-400">Experiences</p>
          <ul className="space-y-1 text-sm text-slate-600">
            {profile.experiences.slice(0, 4).map((experience) => (
              <li key={experience}>- {experience}</li>
            ))}
          </ul>
        </div>
      )}

      {profile.cvUrl && (
        <a
          href={profile.cvUrl}
          target="_blank"
          rel="noreferrer"
          className="inline-flex w-fit items-center gap-2 rounded-lg bg-white px-3 py-2 text-xs font-bold text-blue-600 shadow-sm hover:bg-blue-50"
        >
          <FileText size={14} />
          Ouvrir le CV
        </a>
      )}
    </div>
  );
}

function PaymentDialog({
  payment,
  mission,
  freelancerName,
  onClose,
  onConfirmed,
}: {
  payment: PaymentResponse;
  mission: Mission;
  freelancerName: string;
  onClose: () => void;
  onConfirmed: (status: string) => void;
}) {
  const cardRef = useRef<HTMLDivElement | null>(null);
  const stripeRef = useRef<StripeClient | null>(null);
  const cardElementRef = useRef<StripeCardElement | null>(null);
  const [isReady, setIsReady] = useState(false);
  const [isPaying, setIsPaying] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => {
    let cancelled = false;

    const mountStripe = async () => {
      setStatusMessage("");
      const publishableKey = String(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY ?? "");
      if (
        !publishableKey ||
        publishableKey.includes("a_configurer") ||
        publishableKey.includes("mettre-plus-tard") ||
        publishableKey.includes("cle-stripe")
      ) {
        setStatusMessage("Cle Stripe publique manquante. Ajoutez VITE_STRIPE_PUBLISHABLE_KEY dans le .env du front.");
        return;
      }
      if (!payment.clientSecret) {
        setStatusMessage("Client secret Stripe manquant pour ce paiement.");
        return;
      }

      try {
        await loadStripeScript();
        if (cancelled || !window.Stripe || !cardRef.current) return;
        const stripe = window.Stripe(publishableKey);
        const elements = stripe.elements();
        const card = elements.create("card", {
          style: {
            base: {
              color: "#0f172a",
              fontFamily: "Inter, system-ui, sans-serif",
              fontSize: "15px",
              "::placeholder": { color: "#94a3b8" },
            },
            invalid: { color: "#dc2626" },
          },
        });
        card.mount(cardRef.current);
        stripeRef.current = stripe;
        cardElementRef.current = card;
        setIsReady(true);
      } catch {
        setStatusMessage("Impossible de charger Stripe.js.");
      }
    };

    void mountStripe();

    return () => {
      cancelled = true;
      cardElementRef.current?.destroy();
      cardElementRef.current = null;
      stripeRef.current = null;
    };
  }, [payment.clientSecret]);

  const handleConfirmPayment = async () => {
    if (!payment.clientSecret || !stripeRef.current || !cardElementRef.current || isPaying) return;

    setIsPaying(true);
    setStatusMessage("");

    try {
      const result = await stripeRef.current.confirmCardPayment(payment.clientSecret, {
        payment_method: {
          card: cardElementRef.current,
        },
      });

      if (result.error) {
        setStatusMessage(result.error.message ?? "Paiement refuse par Stripe.");
        return;
      }

      onConfirmed((result.paymentIntent?.status ?? "SUCCEEDED").toUpperCase());
    } finally {
      setIsPaying(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-slate-950/40 p-4">
      <div className="w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Paiement mission</h2>
            <p className="text-sm text-slate-500">{mission.title}</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-50 hover:text-slate-700">
            <X size={20} />
          </button>
        </div>

        <div className="space-y-5 px-5 py-5">
          <div className="grid grid-cols-2 gap-3">
            <PaymentMetric label="Freelancer" value={freelancerName} />
            <PaymentMetric label="Montant" value={formatMoney(payment.amountCents, payment.currency)} />
            <PaymentMetric label="Commission" value={formatMoney(payment.platformFeeCents, payment.currency)} />
            <PaymentMetric label="Net freelancer" value={formatMoney(payment.netAmountCents, payment.currency)} />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700">Carte bancaire</label>
            <div ref={cardRef} className="min-h-[48px] rounded-xl border border-slate-200 bg-slate-50 px-4 py-3" />
            <p className="text-xs text-slate-400">Utilisez une carte de test Stripe si vous etes en mode test.</p>
          </div>

          {statusMessage && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-700">
              {statusMessage}
            </div>
          )}

          <button
            type="button"
            onClick={() => void handleConfirmPayment()}
            disabled={!isReady || isPaying}
            className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-3 text-sm font-bold text-white shadow-sm shadow-emerald-500/20 transition-colors hover:bg-emerald-600 disabled:bg-slate-300"
          >
            {isPaying ? <Loader2 size={18} className="animate-spin" /> : <CreditCard size={18} />}
            {isPaying ? "Validation..." : "Confirmer le paiement"}
          </button>
        </div>
      </div>
    </div>
  );
}

function PaymentMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50 p-3">
      <p className="text-[11px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 truncate text-sm font-bold text-slate-900">{value}</p>
    </div>
  );
}

function loadStripeScript() {
  if (window.Stripe) return Promise.resolve();

  return new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[src="https://js.stripe.com/v3/"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("Stripe.js failed to load")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = "https://js.stripe.com/v3/";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Stripe.js failed to load"));
    document.head.appendChild(script);
  });
}

type CompanyConversationView = Conversation & {
  missionTitle: string;
  freelancerName: string;
  freelancerPhoto?: string;
};

function CompanyMessagesView({
  profile,
  missions,
  applications,
}: {
  profile: CompanyProfile | null;
  missions: Mission[];
  applications: ApplicationResponse[];
}) {
  const socketRef = useRef<MessagingSocket | null>(null);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [freelancersById, setFreelancersById] = useState<Record<number, FreelancerProfile>>({});
  const [selectedConversationId, setSelectedConversationId] = useState<string>("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState("");
  const [search, setSearch] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  const missionsById = useMemo(
    () => Object.fromEntries(missions.map((mission) => [mission.id, mission])),
    [missions]
  );

  const applicationsByConversationKey = useMemo(() => {
    return Object.fromEntries(
      applications
        .filter((application) => application.freelancerId)
        .map((application) => [`${application.missionId}:${application.freelancerId}`, application])
    );
  }, [applications]);

  const conversationViews = useMemo<CompanyConversationView[]>(
    () =>
      conversations.map((conversation) => {
        const mission = missionsById[conversation.missionId];
        const freelancer = freelancersById[conversation.freelancerId];
        const application = applicationsByConversationKey[`${conversation.missionId}:${conversation.freelancerId}`];
        return {
          ...conversation,
          missionTitle: mission?.title ?? `Mission #${conversation.missionId}`,
          freelancerName:
            freelancerFullName(freelancer) ??
            application?.freelancerFullname ??
            `Freelancer #${conversation.freelancerId}`,
          freelancerPhoto: freelancer?.pfpUrl,
        };
      }),
    [applicationsByConversationKey, conversations, freelancersById, missionsById]
  );

  const visibleConversations = useMemo(() => {
    const value = search.trim().toLowerCase();
    if (!value) return conversationViews;
    return conversationViews.filter(
      (conversation) =>
        conversation.freelancerName.toLowerCase().includes(value) ||
        conversation.missionTitle.toLowerCase().includes(value)
    );
  }, [conversationViews, search]);

  const availableApplications = useMemo(() => {
    const existingKeys = new Set(conversations.map((item) => `${item.missionId}:${item.freelancerId}`));
    return applications.filter(
      (application) => application.freelancerId && !existingKeys.has(`${application.missionId}:${application.freelancerId}`)
    );
  }, [applications, conversations]);

  const selectedConversation = conversationViews.find((item) => item.id === selectedConversationId);

  useEffect(() => {
    void loadMessagingData();

    return () => {
      socketRef.current?.disconnect();
    };
  }, []);

  useEffect(() => {
    if (!selectedConversationId) return;
    void loadConversation(selectedConversationId);
  }, [selectedConversationId]);

  const loadMessagingData = async () => {
    setIsLoading(true);
    setStatusMessage("");

    try {
      const conversationList = await listConversations();
      setConversations(conversationList);
      await loadFreelancers(conversationList, applications);

      if (!selectedConversationId && conversationList[0]) {
        setSelectedConversationId(conversationList[0].id);
      }
    } catch (err) {
      setStatusMessage(err instanceof ApiError ? err.message : "Impossible de charger la messagerie.");
    } finally {
      setIsLoading(false);
    }
  };

  const loadFreelancers = async (conversationList: Conversation[], applicationList: ApplicationResponse[]) => {
    const ids = Array.from(
      new Set([
        ...conversationList.map((conversation) => conversation.freelancerId),
        ...applicationList.map((application) => application.freelancerId),
      ].filter((id): id is number => typeof id === "number"))
    );
    if (ids.length === 0) return;

    const entries = await Promise.all(
      ids.map(async (freelancerId) => {
        try {
          const freelancer = await getFreelancerPublicProfile(freelancerId);
          const signedPhoto = freelancer.pfpUrl
            ? await createMediaReadUrl(freelancer.pfpUrl).catch(() => undefined)
            : undefined;
          return [freelancerId, { ...freelancer, pfpUrl: signedPhoto?.signedUrl ?? freelancer.pfpUrl }] as const;
        } catch {
          return undefined;
        }
      })
    );

    setFreelancersById((current) => ({
      ...current,
      ...Object.fromEntries(entries.filter(Boolean) as Array<readonly [number, FreelancerProfile]>),
    }));
  };

  const loadConversation = async (conversationId: string) => {
    setIsHistoryLoading(true);
    setStatusMessage("");

    try {
      const history = await getConversationMessages(conversationId);
      setMessages(history);
      subscribeToConversation(conversationId);
    } catch (err) {
      setMessages([]);
      setStatusMessage(err instanceof ApiError ? err.message : "Impossible de charger les messages.");
    } finally {
      setIsHistoryLoading(false);
    }
  };

  const subscribeToConversation = (conversationId: string) => {
    if (!getAccessToken()) return;

    socketRef.current?.disconnect();
    const socket = new MessagingSocket();
    socket.connect(conversationId, (message) => {
      setMessages((current) =>
        current.some((item) => item.id === message.id) ? current : [...current, message]
      );
    });
    socketRef.current = socket;
  };

  const startConversationFromApplication = async (application: ApplicationResponse) => {
    if (!profile) {
      setStatusMessage("Profil entreprise introuvable.");
      return;
    }
    if (!application.freelancerId) {
      setStatusMessage("Cette candidature ne contient pas encore l'identifiant freelancer.");
      return;
    }

    setIsSending(true);
    setStatusMessage("");

    try {
      const conversation = await createConversation({
        missionId: application.missionId,
        companyId: profile.id,
        freelancerId: application.freelancerId,
        freelancerKeycloakId: application.freelancerKeycloakId,
      });

      setConversations((current) =>
        current.some((item) => item.id === conversation.id) ? current : [conversation, ...current]
      );
      setSelectedConversationId(conversation.id);
      setStatusMessage("Conversation ouverte.");
    } catch (err) {
      setStatusMessage(err instanceof ApiError ? err.message : "Impossible d'ouvrir la conversation.");
    } finally {
      setIsSending(false);
    }
  };

  const handleSend = async () => {
    const content = inputText.trim();
    if (!content || !selectedConversation || isSending) return;

    setIsSending(true);
    setStatusMessage("");

    try {
      const sent = await sendChatMessage(selectedConversation.id, content);
      setMessages((current) =>
        current.some((message) => message.id === sent.id) ? current : [...current, sent]
      );
      setInputText("");
    } catch (err) {
      setStatusMessage(err instanceof ApiError ? err.message : "Message non envoye.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="grid min-h-[calc(100vh-11rem)] grid-cols-1 xl:grid-cols-[360px_1fr] gap-6">
      <div className="space-y-4">
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 overflow-hidden">
          <div className="p-4 border-b border-slate-100">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <MessageSquare size={20} /> Messages
            </h2>
            <div className="relative mt-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Rechercher..."
                className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-4 text-sm outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
              />
            </div>
          </div>

          <div className="max-h-[460px] overflow-y-auto">
            {isLoading ? (
              <CompanyMessageEmpty icon={<Loader2 className="animate-spin" size={28} />} title="Chargement..." />
            ) : visibleConversations.length === 0 ? (
              <CompanyMessageEmpty icon={<MessageSquare size={32} />} title="Aucune conversation" />
            ) : (
              visibleConversations.map((conversation) => (
                <button
                  key={conversation.id}
                  onClick={() => setSelectedConversationId(conversation.id)}
                  className={`w-full border-b border-slate-100 p-4 text-left transition-colors hover:bg-slate-50 ${
                    selectedConversationId === conversation.id ? "bg-blue-50" : "bg-white"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <FreelancerAvatar profile={freelancersById[conversation.freelancerId]} fallback={conversation.freelancerName} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold text-slate-900">{conversation.freelancerName}</p>
                      <p className="truncate text-xs font-semibold text-blue-500">{conversation.missionTitle}</p>
                    </div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 p-4 space-y-3">
          <h3 className="text-sm font-bold text-slate-900">Candidats a contacter</h3>
          {availableApplications.length === 0 ? (
            <p className="text-sm text-slate-500">Aucune nouvelle candidature disponible.</p>
          ) : (
            availableApplications.slice(0, 6).map((application) => (
              <div key={application.id} className="rounded-xl border border-slate-100 p-3">
                <p className="text-sm font-bold text-slate-900">{application.freelancerFullname}</p>
                <p className="truncate text-xs text-slate-500">{missionsById[application.missionId]?.title ?? `Mission #${application.missionId}`}</p>
                <button
                  onClick={() => void startConversationFromApplication(application)}
                  disabled={isSending}
                  className="mt-3 rounded-lg bg-blue-50 px-3 py-2 text-xs font-bold text-blue-600 hover:bg-blue-100 disabled:text-slate-400"
                >
                  Ouvrir conversation
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 overflow-hidden flex min-h-[620px] flex-col">
        <div className="h-20 border-b border-slate-100 px-6 flex items-center justify-between">
          {selectedConversation ? (
            <div className="flex items-center gap-3 min-w-0">
              <FreelancerAvatar profile={freelancersById[selectedConversation.freelancerId]} fallback={selectedConversation.freelancerName} />
              <div className="min-w-0">
                <h2 className="truncate text-lg font-bold text-slate-900">{selectedConversation.freelancerName}</h2>
                <p className="truncate text-sm text-slate-500">{selectedConversation.missionTitle}</p>
              </div>
            </div>
          ) : (
            <div>
              <h2 className="text-lg font-bold text-slate-900">Conversation</h2>
              <p className="text-sm text-slate-500">Selectionnez ou ouvrez une conversation.</p>
            </div>
          )}
        </div>

        {statusMessage && (
          <div className="mx-5 mt-4 rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
            {statusMessage}
          </div>
        )}

        <div className="flex-1 overflow-y-auto bg-slate-50 p-6">
          {isHistoryLoading ? (
            <CompanyMessageEmpty icon={<Loader2 className="animate-spin" size={36} />} title="Chargement des messages..." />
          ) : !selectedConversation ? (
            <CompanyMessageEmpty icon={<MessageSquare size={42} />} title="Aucune conversation selectionnee" />
          ) : messages.length === 0 ? (
            <CompanyMessageEmpty icon={<MessageSquare size={42} />} title="Aucun message" detail="Envoyez un premier message au candidat." />
          ) : (
            <div className="space-y-5">
              {messages.map((message) => (
                <CompanyMessageBubble key={message.id} message={message} />
              ))}
            </div>
          )}
        </div>

        <div className="border-t border-slate-100 p-4">
          <div className="flex items-end gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 focus-within:border-blue-500 focus-within:ring-4 focus-within:ring-blue-500/10">
            <textarea
              value={inputText}
              onChange={(event) => setInputText(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  void handleSend();
                }
              }}
              placeholder="Ecrivez votre message..."
              className="max-h-32 min-h-[42px] flex-1 resize-none bg-transparent px-3 py-2.5 text-sm text-slate-700 outline-none"
              rows={1}
              disabled={!selectedConversation || isSending}
            />
            <button
              onClick={() => void handleSend()}
              disabled={!inputText.trim() || !selectedConversation || isSending}
              className="rounded-xl bg-blue-500 p-3 text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600 disabled:bg-slate-200 disabled:text-slate-400"
            >
              {isSending ? <Loader2 size={18} className="animate-spin" /> : <Send size={18} />}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function CompanyMessageBubble({ message }: { message: ChatMessage }) {
  const isCompany = message.senderRole === "COMPANY";

  return (
    <div className={`flex flex-col ${isCompany ? "items-end" : "items-start"}`}>
      <div className="mb-1 px-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
        {message.senderRole}
      </div>
      <div
        className={`max-w-[85%] rounded-2xl p-4 text-sm shadow-sm md:max-w-[70%] ${
          isCompany
            ? "rounded-tr-sm bg-blue-500 text-white"
            : "rounded-tl-sm border border-slate-100 bg-white text-slate-700"
        }`}
      >
        <p className="whitespace-pre-line leading-relaxed">{message.content}</p>
      </div>
      <div className="mt-1.5 flex items-center gap-1.5 px-1 text-xs text-slate-400">
        <span>{formatMessageTime(message.sentAt)}</span>
        {isCompany && <CheckCheck size={14} className="text-blue-500" />}
      </div>
    </div>
  );
}

function FreelancerAvatar({ profile, fallback }: { profile?: FreelancerProfile; fallback: string }) {
  const name = freelancerFullName(profile) ?? fallback;
  if (profile?.pfpUrl) {
    return <img src={profile.pfpUrl} alt={name} className="h-11 w-11 rounded-xl object-cover bg-slate-100" />;
  }

  return (
    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-violet-50 text-sm font-bold text-violet-600">
      {name.slice(0, 2).toUpperCase()}
    </div>
  );
}

function CompanyMessageEmpty({ icon, title, detail }: { icon: React.ReactNode; title: string; detail?: string }) {
  return (
    <div className="flex h-full min-h-48 flex-col items-center justify-center p-6 text-center text-slate-400">
      <div className="mb-3 text-slate-300">{icon}</div>
      <p className="font-semibold text-slate-500">{title}</p>
      {detail && <p className="mt-1 text-sm">{detail}</p>}
    </div>
  );
}

function freelancerFullName(profile?: FreelancerProfile) {
  if (!profile) return undefined;
  const fullName = `${profile.firstName ?? ""} ${profile.lastName ?? ""}`.trim();
  return fullName || profile.email;
}

function formatMessageTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function ProfileView({
  profile,
  logoUrl,
  form,
  setForm,
  setLogoFile,
  onSave,
  isSaving,
}: {
  profile: CompanyProfile | null;
  logoUrl?: string;
  form: ProfileForm;
  setForm: (form: ProfileForm) => void;
  setLogoFile: (file: File | null) => void;
  onSave: () => void;
  isSaving: boolean;
}) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-6">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 p-6">
        {logoUrl ? (
          <img src={logoUrl} alt={profile?.companyName ?? "Company"} className="mb-5 h-28 w-28 rounded-2xl object-cover bg-slate-100" />
        ) : (
          <div className="mb-5 h-28 w-28 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center text-3xl font-bold">
            {profile?.companyName?.slice(0, 2).toUpperCase() ?? "CO"}
          </div>
        )}
        <h2 className="text-xl font-bold text-slate-900">{profile?.companyName}</h2>
        <p className="text-sm text-slate-500">{profile?.companyEmail}</p>
        <div className="mt-4">
          <StatusBadge status={profile?.status ?? "Pending"} />
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-100/50 p-6 space-y-4">
        <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
          <Settings size={20} /> Profil entreprise
        </h2>
        <TextInput label="Nom" value={form.companyName} onChange={(value) => setForm({ ...form, companyName: value })} />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <TextInput label="Prenom du contact" value={form.contactFirstName} onChange={(value) => setForm({ ...form, contactFirstName: value })} />
          <TextInput label="Nom du contact" value={form.contactLastName} onChange={(value) => setForm({ ...form, contactLastName: value })} />
        </div>
        <TextInput label="Adresse" value={form.companyAddress} onChange={(value) => setForm({ ...form, companyAddress: value })} />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <TextInput label="Telephone" value={form.companyPhone} onChange={(value) => setForm({ ...form, companyPhone: value })} />
          <TextInput label="Domaine" value={form.domaine} onChange={(value) => setForm({ ...form, domaine: value })} />
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-700">Logo</label>
          <input
            type="file"
            accept="image/*"
            onChange={(event) => setLogoFile(event.target.files?.[0] ?? null)}
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 file:mr-4 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-blue-600"
          />
        </div>
        <button
          onClick={onSave}
          disabled={isSaving}
          className="inline-flex items-center gap-2 rounded-xl bg-blue-500 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600 disabled:bg-slate-300"
        >
          <Save size={18} /> {isSaving ? "Enregistrement..." : "Enregistrer"}
        </button>
      </div>
    </div>
  );
}

function TextInput({ label, value, helper, onChange }: { label: string; value: string; helper?: string; onChange: (value: string) => void }) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-slate-700">{label}</label>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
      />
      {helper && <p className="text-xs text-slate-400">{helper}</p>}
    </div>
  );
}

function ActionButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button onClick={onClick} className="rounded-lg bg-blue-50 px-3 py-2 text-xs font-bold text-blue-600 hover:bg-blue-100">
      {label}
    </button>
  );
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const isGood = normalized === "VALIDATED" || normalized === "PUBLIEE" || normalized === "ACCEPTED";
  const isWarn = normalized === "PENDING" || normalized === "BROUILLON" || normalized === "WAITLISTED";
  const classes = isGood
    ? "bg-emerald-50 text-emerald-600 border-emerald-200"
    : isWarn
      ? "bg-amber-50 text-amber-600 border-amber-200"
      : "bg-slate-50 text-slate-600 border-slate-200";

  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold uppercase ${classes}`}>{status}</span>;
}

function formatMoney(amountCents: number, currency = "mad") {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: currency.toUpperCase(),
  }).format(amountCents / 100);
}

function isSuccessfulPayment(status?: string) {
  return status?.toUpperCase() === "SUCCEEDED" || status?.toLowerCase() === "succeeded";
}

function formatPaymentStatus(status: string) {
  const normalized = status.toUpperCase();
  if (normalized === "SUCCEEDED") return "confirme";
  if (normalized === "REQUIRES_PAYMENT") return "a finaliser";
  if (normalized === "FAILED") return "echoue";
  return normalized.toLowerCase().replaceAll("_", " ");
}

function toMissionForm(mission: Mission): MissionForm {
  return {
    title: mission.title ?? "",
    description: mission.description ?? "",
    requiredSkills: (mission.requiredSkills ?? []).join(", "),
    durationDays: String(mission.durationDays ?? 30),
    budget: String(mission.budget ?? 1000),
    workMode: mission.workMode ?? "REMOTE",
    initialStatus: mission.status === "PUBLIEE" ? "PUBLIEE" : "BROUILLON",
  };
}

function toMissionRequest(form: MissionForm): MissionRequest {
  return {
    title: form.title.trim(),
    description: form.description.trim(),
    requiredSkills: form.requiredSkills
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean),
    durationDays: Number(form.durationDays),
    budget: Number(form.budget),
    workMode: form.workMode,
  };
}

function validateMissionRequest(request: MissionRequest) {
  if (!request.title) return "Le titre de la mission est obligatoire.";
  if (!request.description) return "La description de la mission est obligatoire.";
  if (!request.requiredSkills.length) return "Ajoutez au moins une competence.";
  if (!Number.isFinite(request.durationDays) || request.durationDays <= 0) {
    return "La duree doit etre superieure a 0.";
  }
  if (!Number.isFinite(request.budget) || request.budget <= 0) {
    return "Le budget doit etre superieur a 0.";
  }
  return "";
}

function getApiErrorMessage(err: unknown, fallback: string) {
  return err instanceof ApiError ? err.message : fallback;
}
