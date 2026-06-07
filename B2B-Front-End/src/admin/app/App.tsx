import React, { useEffect, useMemo, useState } from "react";
import "../styles/index.css";
import logoImage from "../../assets/PFS_Logo.jpg";
import {
  AlertCircle,
  Briefcase,
  Building2,
  CheckCircle2,
  Eye,
  LayoutDashboard,
  Loader2,
  LogOut,
  Menu,
  Save,
  ShieldBan,
  Trash2,
  UserCircle,
  Users,
  X,
  type LucideIcon,
} from "lucide-react";
import { ApiError } from "../../shared/api";
import { clearTokens, decodeJwtPayload, getAccessToken } from "../../shared/auth";
import {
  approveCompany,
  deleteCompanyAdmin,
  deleteFreelancerAdmin,
  getAdminProfile,
  getAuditLogs,
  getCompanyMissionsForAdmin,
  getCompaniesForAdmin,
  getFreelancersForAdmin,
  getMissionsForAdmin,
  getPendingCompaniesForAdmin,
  listMissionsByCompany,
  rejectCompany,
  suspendCompany,
  suspendFreelancer,
  updateAdminProfile,
  type AdminCompany,
  type AdminFreelancer,
  type AdminMission,
  type AdminProfile,
  type AuditLog,
} from "../../shared/services";

type Tab = "dashboard" | "entreprises" | "freelances" | "audit" | "profile";
const ADMIN_PROFILE_CACHE_KEY = "fic_admin_profile";

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>("dashboard");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [admin, setAdmin] = useState<AdminProfile | null>(null);
  const [adminForm, setAdminForm] = useState({ username: "", email: "" });
  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [freelancers, setFreelancers] = useState<AdminFreelancer[]>([]);
  const [missions, setMissions] = useState<AdminMission[]>([]);
  const [companyMissions, setCompanyMissions] = useState<Record<number, AdminMission[]>>({});
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [selectedCompany, setSelectedCompany] = useState<AdminCompany | null>(null);
  const [selectedFreelancer, setSelectedFreelancer] = useState<AdminFreelancer | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    void loadAdminSpace();
  }, []);

  useEffect(() => {
    if (!message) return;
    const timeoutId = window.setTimeout(() => setMessage(""), 3500);
    return () => window.clearTimeout(timeoutId);
  }, [message]);

  const stats = useMemo(
    () => ({
      totalCompanies: companies.length,
      pendingCompanies: companies.filter((item) => isCompanyStatus(item.status, "Pending")).length,
      validatedCompanies: companies.filter((item) => isCompanyStatus(item.status, "Validated")).length,
      rejectedCompanies: companies.filter((item) => isCompanyStatus(item.status, "Rejected")).length,
      suspendedCompanies: companies.filter((item) => isCompanyStatus(item.status, "Suspended")).length,
      missions: missions.length,
      freelancers: freelancers.length,
      suspendedFreelancers: freelancers.filter((item) => item.suspended).length,
      audit: auditLogs.length,
    }),
    [auditLogs.length, companies, freelancers, missions.length]
  );

  const loadAdminSpace = async () => {
    setIsLoading(true);
    setMessage("");

    const errors: string[] = [];
    let resolvedCompanies: AdminCompany[] = [];

    const [profileResult, companiesResult, freelancersResult, missionsResult, auditResult] = await Promise.allSettled([
      getAdminProfile(),
      loadCompaniesForAdmin(),
      getFreelancersForAdmin(),
      getMissionsForAdmin(),
      getAuditLogs(),
    ]);

    if (profileResult.status === "fulfilled") {
      const cachedAdmin = mergeCachedAdminProfile(profileResult.value);
      setAdmin(cachedAdmin);
      setAdminForm({ username: cachedAdmin.username ?? "", email: cachedAdmin.email ?? "" });
    } else {
      const fallbackAdmin = getAdminFromToken();
      const cachedAdmin = mergeCachedAdminProfile(fallbackAdmin);
      setAdmin(cachedAdmin);
      setAdminForm({ username: cachedAdmin.username, email: cachedAdmin.email ?? "" });
    }

    if (companiesResult.status === "fulfilled") {
      resolvedCompanies = companiesResult.value.items;
      setCompanies(resolvedCompanies);
      if (companiesResult.value.degraded) {
        errors.push(companiesResult.value.message);
      }
    } else {
      setCompanies([]);
      errors.push(getAdminErrorMessage(companiesResult.reason, "Entreprises indisponibles."));
    }

    if (freelancersResult.status === "fulfilled") {
      setFreelancers(freelancersResult.value);
    } else {
      setFreelancers([]);
      errors.push(getAdminErrorMessage(freelancersResult.reason, "Freelances indisponibles."));
    }

    if (missionsResult.status === "fulfilled") {
      setMissions(await resolveAdminMissions(missionsResult.value, resolvedCompanies));
    } else {
      const fallbackMissions = await resolveAdminMissions([], resolvedCompanies);
      setMissions(fallbackMissions);
      if (fallbackMissions.length === 0) {
        errors.push(getAdminErrorMessage(missionsResult.reason, "Missions indisponibles."));
      }
    }

    if (auditResult.status === "fulfilled") {
      setAuditLogs(auditResult.value);
    } else {
      setAuditLogs([]);
      errors.push(getAdminErrorMessage(auditResult.reason, "Audit logs indisponibles."));
    }

    setMessage(errors.filter((error) => error.trim()).join(" "));
    setIsLoading(false);
  };

  const handleLogout = () => {
    clearTokens();
    window.location.href = "/sign-in";
  };

  const runAdminAction = async (action: () => Promise<unknown>, successMessage: string) => {
    setMessage("");
    try {
      await action();
      setSelectedCompany(null);
      setSelectedFreelancer(null);
      await loadAdminSpace();
      setMessage(successMessage);
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Action admin impossible.");
    }
  };

  const openCompanyDetails = async (company: AdminCompany) => {
    setSelectedCompany(company);
    if (companyMissions[company.id]) return;
    try {
      const items = await loadCompanyMissionsForDashboard(company.id);
      setCompanyMissions((current) => ({ ...current, [company.id]: items }));
    } catch {
      setCompanyMissions((current) => ({ ...current, [company.id]: [] }));
    }
  };

  const promptReason = (defaultReason: string) => {
    const reason = window.prompt("Raison de l'action admin", defaultReason);
    return reason?.trim() || "";
  };

  const saveAdminProfile = async () => {
    if (!admin) return;
    setMessage("");
    const updatedAdmin = { ...admin, username: adminForm.username, email: adminForm.email };
    try {
      const savedAdmin = await updateAdminProfile(updatedAdmin);
      setAdmin(savedAdmin);
      cacheAdminProfile(savedAdmin);
      setMessage("Profil admin mis a jour.");
    } catch (err) {
      setAdmin(updatedAdmin);
      cacheAdminProfile(updatedAdmin);
      setMessage("Profil admin mis a jour localement.");
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col md:flex-row">
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/20 backdrop-blur-sm md:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-sidebar border-r border-sidebar-border transform transition-transform duration-200 ease-in-out md:relative md:translate-x-0 ${
          mobileMenuOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="h-16 flex items-center px-6 border-b border-sidebar-border">
          <div className="flex items-center gap-2 text-sidebar-primary font-bold text-xl tracking-tight">
            <img src={logoImage} alt="FIC Logo" className="size-8 rounded-lg object-contain" />
            FIC
          </div>
          <button className="ml-auto md:hidden text-sidebar-foreground" onClick={() => setMobileMenuOpen(false)}>
            <X className="size-5" />
          </button>
        </div>

        <div className="p-4 space-y-1 overflow-y-auto h-[calc(100vh-64px)]">
          <div className="text-xs font-semibold text-sidebar-foreground/50 uppercase tracking-wider mb-2 px-2">Menu Principal</div>
          <NavButton tab="dashboard" activeTab={activeTab} setActiveTab={setActiveTab} icon={LayoutDashboard} label="Vue d'ensemble" />
          <NavButton tab="entreprises" activeTab={activeTab} setActiveTab={setActiveTab} icon={Building2} label="Entreprises" />
          <NavButton tab="freelances" activeTab={activeTab} setActiveTab={setActiveTab} icon={Users} label="Freelances" />
          <NavButton tab="audit" activeTab={activeTab} setActiveTab={setActiveTab} icon={Briefcase} label="Audit" />

          <div className="mt-8 mb-2 px-2 text-xs font-semibold text-sidebar-foreground/50 uppercase tracking-wider">Parametres</div>
          <NavButton tab="profile" activeTab={activeTab} setActiveTab={setActiveTab} icon={UserCircle} label="Profil Admin" />
        </div>
      </aside>

      <main className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-card border-b border-border flex items-center justify-between px-4 sm:px-8 shrink-0">
          <div className="flex items-center gap-4">
            <button className="md:hidden p-2 text-muted-foreground hover:bg-muted rounded-lg" onClick={() => setMobileMenuOpen(true)}>
              <Menu className="size-5" />
            </button>
            <h2 className="text-sm font-medium text-muted-foreground hidden sm:block">{titleFor(activeTab)}</h2>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium leading-none">{admin?.username ?? "Admin"}</p>
                <p className="text-xs text-muted-foreground">Admin</p>
              </div>
              <button className="size-8 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-sm" onClick={() => setActiveTab("profile")}>
                {(admin?.username ?? "AD").slice(0, 2).toUpperCase()}
              </button>
              <button
                onClick={handleLogout}
                className="inline-flex items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm font-semibold text-red-600 transition-colors hover:bg-red-100"
                title="Deconnexion"
              >
                <LogOut size={16} />
                <span className="hidden sm:inline">Déconnexion</span>
              </button>
            </div>
          </div>
        </header>

        <div className="flex-1 p-4 sm:p-8 overflow-y-auto">
          <div className="max-w-6xl mx-auto space-y-6">
            {message && (
              <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
                {message}
              </div>
            )}

            {isLoading ? (
              <div className="flex min-h-[420px] items-center justify-center text-muted-foreground">
                <Loader2 className="mr-2 animate-spin" size={20} />
                Chargement de l'espace admin...
              </div>
            ) : (
              <>
                {activeTab === "dashboard" && <Dashboard stats={stats} />}
                {activeTab === "entreprises" && (
                  <Companies
                    companies={companies}
                    missions={missions}
                    onSelect={(company) => void openCompanyDetails(company)}
                    onApprove={(id) => runAdminAction(() => approveCompany(id), "Entreprise approuvee.")}
                    onReject={(id) => {
                      const reason = promptReason("Dossier incomplet");
                      if (reason) void runAdminAction(() => rejectCompany(id, reason), "Entreprise rejetee.");
                    }}
                    onSuspend={(id) => {
                      const reason = promptReason("Suspension admin");
                      if (reason) void runAdminAction(() => suspendCompany(id, reason), "Entreprise suspendue.");
                    }}
                    onDelete={(id) => {
                      const reason = promptReason("Suppression admin");
                      if (reason) void runAdminAction(() => deleteCompanyAdmin(id, reason), "Entreprise supprimee.");
                    }}
                  />
                )}
                {activeTab === "freelances" && (
                  <Freelancers
                    freelancers={freelancers}
                    onSelect={setSelectedFreelancer}
                    onSuspend={(id) => {
                      const reason = promptReason("Suspension admin");
                      if (reason) void runAdminAction(() => suspendFreelancer(id, reason), "Freelancer suspendu.");
                    }}
                    onDelete={(id) => {
                      const reason = promptReason("Suppression admin");
                      if (reason) void runAdminAction(() => deleteFreelancerAdmin(id, reason), "Freelancer supprime.");
                    }}
                  />
                )}
                {activeTab === "audit" && <Audit logs={auditLogs} />}
                {activeTab === "profile" && (
                  <Profile
                    form={adminForm}
                    setForm={setAdminForm}
                    onSave={saveAdminProfile}
                  />
                )}
              </>
            )}
          </div>
        </div>
      </main>

      <DetailPanel isOpen={!!selectedCompany} onClose={() => setSelectedCompany(null)} title="Details de l'entreprise">
        {selectedCompany && (
          <CompanyDetails
            company={selectedCompany}
            missions={companyMissions[selectedCompany.id] ?? []}
            onApprove={() => runAdminAction(() => approveCompany(selectedCompany.id), "Entreprise approuvee.")}
            onSuspend={() => {
              const reason = promptReason("Suspension admin");
              if (reason) void runAdminAction(() => suspendCompany(selectedCompany.id, reason), "Entreprise suspendue.");
            }}
            onDelete={() => {
              const reason = promptReason("Suppression admin");
              if (reason) void runAdminAction(() => deleteCompanyAdmin(selectedCompany.id, reason), "Entreprise supprimee.");
            }}
          />
        )}
      </DetailPanel>

      <DetailPanel isOpen={!!selectedFreelancer} onClose={() => setSelectedFreelancer(null)} title="Profil Freelance">
        {selectedFreelancer && <FreelancerDetails freelancer={selectedFreelancer} />}
      </DetailPanel>
    </div>
  );
}

function NavButton({
  tab,
  activeTab,
  setActiveTab,
  icon: Icon,
  label,
}: {
  tab: Tab;
  activeTab: Tab;
  setActiveTab: (tab: Tab) => void;
  icon: typeof LayoutDashboard;
  label: string;
}) {
  return (
    <button
      onClick={() => setActiveTab(tab)}
      className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
        activeTab === tab
          ? "bg-sidebar-primary text-sidebar-primary-foreground"
          : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
      }`}
    >
      <Icon className="size-5" /> {label}
    </button>
  );
}

function Dashboard({
  stats,
}: {
  stats: {
    totalCompanies: number;
    pendingCompanies: number;
    validatedCompanies: number;
    rejectedCompanies: number;
    suspendedCompanies: number;
    missions: number;
    freelancers: number;
    suspendedFreelancers: number;
    audit: number;
  };
}) {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-foreground">Vue d'ensemble</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-6 gap-6">
        <Stat icon={Building2} label="Entreprises" value={stats.totalCompanies} />
        <Stat icon={Building2} label="Entreprises en attente" value={stats.pendingCompanies} />
        <Stat icon={CheckCircle2} label="Entreprises validees" value={stats.validatedCompanies} />
        <Stat icon={AlertCircle} label="Entreprises refusees" value={stats.rejectedCompanies} />
        <Stat icon={Briefcase} label="Missions creees" value={stats.missions} />
        <Stat icon={Users} label="Freelances" value={stats.freelancers} />
        <Stat icon={ShieldBan} label="Freelances suspendus" value={stats.suspendedFreelancers} />
        <Stat icon={Briefcase} label="Audit logs" value={stats.audit} />
      </div>
    </div>
  );
}

function Stat({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value: number }) {
  return (
    <div className="bg-card p-6 rounded-2xl shadow-sm border border-border flex items-center gap-4">
      <div className="size-14 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
        <Icon className="size-7" />
      </div>
      <div>
        <p className="text-sm font-medium text-muted-foreground">{label}</p>
        <p className="text-3xl font-bold text-foreground">{value}</p>
      </div>
    </div>
  );
}

function Companies({
  companies,
  missions,
  onSelect,
  onApprove,
  onReject,
  onSuspend,
  onDelete,
}: {
  companies: AdminCompany[];
  missions: AdminMission[];
  onSelect: (company: AdminCompany) => void;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onSuspend: (id: number) => void;
  onDelete: (id: number) => void;
}) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const filteredCompanies = useMemo(() => {
    const query = normalizeSearch(search);
    return companies.filter((company) => {
      const matchesStatus = statusFilter === "all" || isCompanyStatus(company.status, statusFilter as "Pending" | "Validated" | "Rejected" | "Suspended");
      const haystack = [
        company.companyName,
        company.companyEmail,
        company.domaine,
        company.companyAddress,
        company.companyPhone,
        company.status,
      ].join(" ");
      return matchesStatus && normalizeSearch(haystack).includes(query);
    });
  }, [companies, search, statusFilter]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-foreground">Gestion des entreprises</h1>
      <div className="grid gap-3 rounded-2xl border border-border bg-card p-4 md:grid-cols-[1fr_220px]">
        <TextInput label="Recherche" value={search} onChange={setSearch} />
        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">Statut</label>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
            className="w-full px-4 py-2.5 bg-input-background border border-border rounded-xl text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
          >
            <option value="all">Tous</option>
            <option value="Pending">Pending</option>
            <option value="Validated">Validated</option>
            <option value="Rejected">Rejected</option>
            <option value="Suspended">Suspended</option>
          </select>
        </div>
      </div>
      <div className="bg-card rounded-2xl shadow-sm border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted/50 text-muted-foreground border-b border-border">
              <tr>
                <th className="px-6 py-4 font-medium">Compte</th>
                <th className="px-6 py-4 font-medium">Info</th>
                <th className="px-6 py-4 font-medium">Missions</th>
                <th className="px-6 py-4 font-medium">Statut</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filteredCompanies.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-muted-foreground">
                    Aucune entreprise trouvee.
                  </td>
                </tr>
              ) : (
                filteredCompanies.map((company) => {
                  const isPending = isCompanyStatus(company.status, "Pending");
                  const isSuspended = isCompanyStatus(company.status, "Suspended");
                  const companyMissions = missions.filter((mission) => mission.companyId === company.id);

                  return (
                    <tr key={company.id} className="hover:bg-muted/30 transition-colors">
                      <td className="px-6 py-4">
                        <p className="font-medium text-foreground">{company.companyName}</p>
                        <p className="text-muted-foreground text-xs">{company.companyEmail}</p>
                      </td>
                      <td className="px-6 py-4 text-muted-foreground">
                        <div>{company.domaine ?? "-"}</div>
                        {company.companyPhone && <div className="text-xs">{company.companyPhone}</div>}
                      </td>
                      <td className="px-6 py-4">
                        {companyMissions.length === 0 ? (
                          <span className="text-xs text-muted-foreground">Aucune mission</span>
                        ) : (
                          <div className="space-y-1">
                            <p className="text-xs font-semibold text-foreground">{companyMissions.length} mission(s)</p>
                            {companyMissions.slice(0, 3).map((mission) => (
                              <div key={mission.id} className="flex items-center gap-2 text-xs text-muted-foreground">
                                <span className="max-w-[220px] truncate">{mission.title}</span>
                                <StatusBadge status={mission.status ?? "BROUILLON"} />
                              </div>
                            ))}
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <StatusBadge status={company.status ?? "Pending"} />
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex flex-wrap items-center justify-end gap-2">
                          <AdminActionButton label="Details" onClick={() => onSelect(company)} icon={Eye} />
                          {!isCompanyStatus(company.status, "Validated") && (
                            <AdminActionButton label="Valider" onClick={() => onApprove(company.id)} icon={CheckCircle2} />
                          )}
                          {isPending && (
                            <AdminActionButton label="Rejeter" onClick={() => onReject(company.id)} icon={ShieldBan} danger />
                          )}
                          {!isSuspended && (
                            <AdminActionButton label="Suspendre" onClick={() => onSuspend(company.id)} icon={ShieldBan} danger />
                          )}
                          <AdminActionButton label="Supprimer" onClick={() => onDelete(company.id)} icon={Trash2} danger />
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function Freelancers({
  freelancers,
  onSelect,
  onSuspend,
  onDelete,
}: {
  freelancers: AdminFreelancer[];
  onSelect: (freelancer: AdminFreelancer) => void;
  onSuspend: (id: number) => void;
  onDelete: (id: number) => void;
}) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const filteredFreelancers = useMemo(() => {
    const query = normalizeSearch(search);
    return freelancers.filter((freelancer) => {
      const matchesStatus =
        statusFilter === "all"
        || (statusFilter === "active" && !freelancer.suspended)
        || (statusFilter === "suspended" && freelancer.suspended);
      const haystack = [freelancer.fullname, freelancer.email, freelancer.keycloakId].join(" ");
      return matchesStatus && normalizeSearch(haystack).includes(query);
    });
  }, [freelancers, search, statusFilter]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-foreground">Gestion des Freelances</h1>
      <div className="grid gap-3 rounded-2xl border border-border bg-card p-4 md:grid-cols-[1fr_220px]">
        <TextInput label="Recherche" value={search} onChange={setSearch} />
        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">Statut</label>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
            className="w-full px-4 py-2.5 bg-input-background border border-border rounded-xl text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
          >
            <option value="all">Tous</option>
            <option value="active">Actifs</option>
            <option value="suspended">Suspendus</option>
          </select>
        </div>
      </div>
      <TableCard title="" empty="Aucun freelance trouve.">
        {filteredFreelancers.map((freelancer) => (
          <tr key={freelancer.id} className="hover:bg-muted/30 transition-colors">
            <td className="px-6 py-4">
              <p className="font-medium text-foreground">{freelancer.fullname}</p>
              <p className="text-muted-foreground text-xs">{freelancer.email}</p>
            </td>
            <td className="px-6 py-4"><StatusBadge status={freelancer.suspended ? "Suspended" : "Active"} /></td>
            <td className="px-6 py-4 text-right">
              <div className="flex items-center justify-end gap-2">
                <IconButton title="Voir details" onClick={() => onSelect(freelancer)} icon={Eye} />
                <IconButton title="Suspendre" onClick={() => onSuspend(freelancer.id)} icon={ShieldBan} danger />
                <IconButton title="Supprimer" onClick={() => onDelete(freelancer.id)} icon={Trash2} danger />
              </div>
            </td>
          </tr>
        ))}
      </TableCard>
    </div>
  );
}

function TableCard({ title, empty, children }: { title: string; empty: string; children: React.ReactNode }) {
  const rows = React.Children.count(children);
  return (
    <div className="space-y-6">
      {title && <h1 className="text-2xl font-bold text-foreground">{title}</h1>}
      <div className="bg-card rounded-2xl shadow-sm border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted/50 text-muted-foreground border-b border-border">
              <tr>
                <th className="px-6 py-4 font-medium">Compte</th>
                <th className="px-6 py-4 font-medium">Info</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {rows === 0 ? (
                <tr>
                  <td colSpan={3} className="px-6 py-12 text-center text-muted-foreground">{empty}</td>
                </tr>
              ) : (
                children
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function Audit({ logs }: { logs: AuditLog[] }) {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-foreground">Audit</h1>
      <div className="bg-card rounded-2xl shadow-sm border border-border overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-muted/50 text-muted-foreground border-b border-border">
            <tr>
              <th className="px-6 py-4 font-medium">Admin</th>
              <th className="px-6 py-4 font-medium">Action</th>
              <th className="px-6 py-4 font-medium">Cible</th>
              <th className="px-6 py-4 font-medium">Date</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {logs.map((log) => (
              <tr key={log.id}>
                <td className="px-6 py-4">{log.adminUsername}</td>
                <td className="px-6 py-4">{log.action}</td>
                <td className="px-6 py-4">{log.targetAccountType} #{log.targetAccountId}</td>
                <td className="px-6 py-4">{log.timestamp ? new Date(log.timestamp).toLocaleString("fr-FR") : "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Profile({ form, setForm, onSave }: { form: { username: string; email: string }; setForm: (form: { username: string; email: string }) => void; onSave: () => void }) {
  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-foreground">Profil Administrateur</h1>
      <div className="bg-card rounded-2xl shadow-sm border border-border p-8 space-y-6">
        <TextInput label="Nom utilisateur" value={form.username} onChange={(value) => setForm({ ...form, username: value })} />
        <TextInput label="Adresse email" value={form.email} onChange={(value) => setForm({ ...form, email: value })} />
        <button onClick={onSave} className="bg-primary text-primary-foreground px-6 py-3 rounded-xl font-medium hover:bg-primary/90 transition-colors flex items-center gap-2">
          <Save className="size-5" /> Enregistrer
        </button>
      </div>
    </div>
  );
}

function DetailPanel({ isOpen, onClose, title, children }: { isOpen: boolean; onClose: () => void; title: string; children: React.ReactNode }) {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-slate-900/20 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md bg-card h-full shadow-2xl animate-in slide-in-from-right duration-300 flex flex-col">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <h2 className="text-xl font-semibold text-foreground">{title}</h2>
          <button onClick={onClose} className="p-2 hover:bg-muted text-muted-foreground rounded-full transition-colors">
            <X className="size-5" />
          </button>
        </div>
        <div className="p-6 overflow-y-auto flex-1">{children}</div>
      </div>
    </div>
  );
}

function CompanyDetails({
  company,
  missions,
  onApprove,
  onSuspend,
  onDelete,
}: {
  company: AdminCompany;
  missions: AdminMission[];
  onApprove: () => void;
  onSuspend: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-xl font-bold">{company.companyName}</h3>
        <p className="text-muted-foreground">{company.companyEmail}</p>
      </div>
      <Info label="Domaine" value={company.domaine} />
      <Info label="Adresse" value={company.companyAddress} />
      <Info label="Telephone" value={company.companyPhone} />
      <Info label="Statut" value={company.status} />
      <div className="space-y-3">
        <p className="text-sm text-muted-foreground">Missions</p>
        {missions.length === 0 ? (
          <p className="rounded-xl border border-border bg-muted/30 p-3 text-sm text-muted-foreground">Aucune mission trouvee.</p>
        ) : (
          missions.map((mission) => (
            <div key={mission.id} className="rounded-xl border border-border p-3">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-foreground">{mission.title}</p>
                  <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{mission.description}</p>
                </div>
                <StatusBadge status={mission.status ?? "BROUILLON"} />
              </div>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
                {mission.workMode && <span>{mission.workMode}</span>}
                {mission.budget !== undefined && <span>{mission.budget} MAD</span>}
                {(mission.requiredSkills ?? []).slice(0, 4).map((skill) => (
                  <span key={skill} className="rounded-md bg-primary/10 px-2 py-1 text-primary">{skill}</span>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
      <div className="pt-6 border-t border-border space-y-3">
        <ActionFull onClick={onApprove} icon={CheckCircle2} label="Approuver cette entreprise" />
        <ActionFull onClick={onSuspend} icon={ShieldBan} label="Suspendre cette entreprise" danger />
        <ActionFull onClick={onDelete} icon={Trash2} label="Supprimer le compte" danger />
      </div>
    </div>
  );
}

function FreelancerDetails({ freelancer }: { freelancer: AdminFreelancer }) {
  return (
    <div className="space-y-6">
      <div className="flex flex-col items-center text-center">
        <div className="size-24 rounded-full bg-secondary flex items-center justify-center text-secondary-foreground text-3xl font-bold mb-4 shadow-sm">
          {freelancer.fullname?.slice(0, 2).toUpperCase()}
        </div>
        <h3 className="text-2xl font-bold">{freelancer.fullname}</h3>
        <p className="text-muted-foreground">{freelancer.email}</p>
        <div className="mt-3"><StatusBadge status={freelancer.suspended ? "Suspended" : "Active"} /></div>
      </div>
      <Info label="Keycloak ID" value={freelancer.keycloakId} />
    </div>
  );
}

function TextInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-foreground">{label}</label>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full px-4 py-2.5 bg-input-background border border-border rounded-xl text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
      />
    </div>
  );
}

function IconButton({ title, onClick, icon: Icon, danger = false }: { title: string; onClick: () => void; icon: typeof Eye; danger?: boolean }) {
  return (
    <button onClick={onClick} className={`p-2 rounded-lg transition-colors ${danger ? "text-destructive hover:bg-destructive/10" : "text-primary hover:bg-primary/10"}`} title={title}>
      <Icon className="size-4" />
    </button>
  );
}

function AdminActionButton({
  label,
  onClick,
  icon: Icon,
  danger = false,
}: {
  label: string;
  onClick: () => void;
  icon: LucideIcon;
  danger?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold transition-colors ${
        danger
          ? "bg-destructive/10 text-destructive hover:bg-destructive/20"
          : "bg-primary/10 text-primary hover:bg-primary/20"
      }`}
    >
      <Icon className="size-4" />
      {label}
    </button>
  );
}

function ActionFull({ onClick, icon: Icon, label, danger = false }: { onClick: () => void; icon: typeof CheckCircle2; label: string; danger?: boolean }) {
  return (
    <button onClick={onClick} className={`w-full py-2.5 px-4 rounded-xl font-medium flex items-center justify-center gap-2 transition-colors ${danger ? "bg-destructive/10 text-destructive hover:bg-destructive/20" : "bg-primary/10 text-primary hover:bg-primary/20"}`}>
      <Icon className="size-5" /> {label}
    </button>
  );
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const good = normalized === "ACTIVE" || normalized === "VALIDATED";
  const warn = normalized === "PENDING";
  const classes = good ? "bg-emerald-100 text-emerald-700" : warn ? "bg-amber-100 text-amber-700" : "bg-destructive/10 text-destructive";
  return <span className={`px-2.5 py-1 rounded-full text-xs font-medium inline-flex items-center gap-1.5 ${classes}`}>{status}</span>;
}

async function loadCompaniesForAdmin() {
  try {
    return {
      items: await getPendingCompaniesForAdmin(true),
      degraded: false,
      message: "",
    };
  } catch {
    return {
      items: await getCompaniesForAdmin(),
      degraded: true,
      message: "",
    };
  }
}

async function resolveAdminMissions(primaryMissions: AdminMission[], companies: AdminCompany[]) {
  if (primaryMissions.length > 0 || companies.length === 0) return primaryMissions;

  const results = await Promise.allSettled(companies.map((company) => loadCompanyMissionsForDashboard(company.id)));
  const missionsById = new Map<number, AdminMission>();

  results.forEach((result) => {
    if (result.status !== "fulfilled") return;
    result.value.forEach((mission) => missionsById.set(mission.id, mission));
  });

  return Array.from(missionsById.values());
}

async function loadCompanyMissionsForDashboard(companyId: number) {
  try {
    return await getCompanyMissionsForAdmin(companyId);
  } catch {
    return listMissionsByCompany(companyId);
  }
}

function getAdminFromToken(): AdminProfile {
  const payload = getAccessToken() ? decodeJwtPayload(getAccessToken() as string) : undefined;
  return {
    id: 0,
    username: payload?.preferred_username ?? "Admin",
    email: payload?.email,
  };
}

function mergeCachedAdminProfile(profile: AdminProfile): AdminProfile {
  try {
    const raw = localStorage.getItem(ADMIN_PROFILE_CACHE_KEY);
    if (!raw) return profile;
    const cached = JSON.parse(raw) as Partial<AdminProfile>;
    return {
      ...profile,
      username: cached.username ?? profile.username,
      email: cached.email ?? profile.email,
    };
  } catch {
    return profile;
  }
}

function cacheAdminProfile(profile: AdminProfile) {
  localStorage.setItem(ADMIN_PROFILE_CACHE_KEY, JSON.stringify({
    username: profile.username,
    email: profile.email,
  }));
}

function getAdminErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message;
  return fallback;
}

function normalizeSearch(value: unknown) {
  return String(value ?? "").trim().toLowerCase();
}

function isCompanyStatus(status: string | undefined, expected: "Pending" | "Validated" | "Rejected" | "Suspended") {
  return (status ?? "").toLowerCase() === expected.toLowerCase();
}

function Info({ label, value }: { label: string; value?: string }) {
  return (
    <div>
      <p className="text-sm text-muted-foreground mb-1">{label}</p>
      <p className="font-medium break-all">{value || "-"}</p>
    </div>
  );
}

function titleFor(tab: Tab) {
  if (tab === "dashboard") return "Dashboard";
  if (tab === "entreprises") return "Gestion > Entreprises";
  if (tab === "freelances") return "Gestion > Freelances";
  if (tab === "audit") return "Administration > Audit";
  return "Parametres > Profil";
}
