import { useEffect, useMemo, useState } from "react";
import { Outlet, NavLink, useLocation, useNavigate } from "react-router";
import "../styles/index.css";
import logoImage from "../../assets/PFS_Logo.jpg";
import { Bell, Briefcase, CheckCircle2, LogOut, MessageSquare, User } from "lucide-react";
import { clearTokens } from "../../shared/auth";
import {
  getCompanyPublicProfile,
  getMyApplications,
  listConversations,
  listPublishedMissions,
  type ApplicationResponse,
  type CompanyPublicProfile,
  type Conversation,
  type Mission,
} from "../../shared/services";

type NotificationItem = {
  id: string;
  kind: "message" | "mission" | "accepted";
  title: string;
  detail: string;
  createdAt?: string;
  to: string;
};

export function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [missions, setMissions] = useState<Mission[]>([]);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [companiesById, setCompaniesById] = useState<Record<number, CompanyPublicProfile>>({});

  useEffect(() => {
    void loadNotificationData();
  }, []);

  const handleLogout = () => {
    clearTokens();
    navigate("/sign-in");
  };

  const getPageTitle = () => {
    if (location.pathname.startsWith("/freelancer/messages")) return "Mes messages";
    if (location.pathname.startsWith("/freelancer/profile")) return "Profil";
    if (location.pathname.startsWith("/freelancer/missions")) return "Missions disponibles";
    return "Missions disponibles";
  };

  const navItems = [
    { to: "/freelancer/missions", icon: Briefcase, label: "Missions" },
    { to: "/freelancer/messages", icon: MessageSquare, label: "Mes messages" },
    { to: "/freelancer/profile", icon: User, label: "Profil" },
  ];

  const notifications = useMemo<NotificationItem[]>(() => {
    const messageItems = conversations.slice(0, 3).map((conversation) => {
      const companyName = companiesById[conversation.companyId]?.companyName ?? `Entreprise #${conversation.companyId}`;
      return {
        id: `conversation-${conversation.id}`,
        kind: "message" as const,
        title: "Message",
        detail: `${companyName} vous a envoye un message.`,
        createdAt: conversation.createdAt,
        to: `/freelancer/messages/${conversation.id}`,
      };
    });

    const missionItems = missions.slice(0, 3).map((mission) => {
      const companyName = companiesById[mission.companyId]?.companyName ?? `Entreprise #${mission.companyId}`;
      return {
        id: `mission-${mission.id}`,
        kind: "mission" as const,
        title: "Mission publiee",
        detail: `${companyName} vient de lancer une mission.`,
        createdAt: mission.createdAt,
        to: "/freelancer/missions",
      };
    });

    const acceptedItems = applications
      .filter((application) => application.status?.toUpperCase() === "ACCEPTED")
      .slice(0, 3)
      .map((application) => {
        const mission = missions.find((item) => item.id === application.missionId);
        const companyName = companiesById[application.missionCompanyId]?.companyName ?? `Entreprise #${application.missionCompanyId}`;
        return {
          id: `accepted-${application.id}`,
          kind: "accepted" as const,
          title: "Candidature acceptee",
          detail: `Vous avez ete accepte pour la mission : ${mission?.title ?? `#${application.missionId}`} de la societe ${companyName}.`,
          createdAt: application.updatedAt ?? application.createdAt,
          to: "/freelancer/messages",
        };
      });

    return [...acceptedItems, ...messageItems, ...missionItems]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 5);
  }, [applications, companiesById, conversations, missions]);

  const loadNotificationData = async () => {
    try {
      const [conversationList, missionList, applicationList] = await Promise.all([
        listConversations().catch(() => [] as Conversation[]),
        listPublishedMissions().catch(() => [] as Mission[]),
        getMyApplications().catch(() => [] as ApplicationResponse[]),
      ]);
      setConversations(conversationList);
      setMissions(missionList);
      setApplications(applicationList);
      await loadCompanies([
        ...conversationList.map((item) => item.companyId),
        ...missionList.map((item) => item.companyId),
        ...applicationList.map((item) => item.missionCompanyId),
      ]);
    } catch {
      // Notifications are non-blocking for the freelancer workspace.
    }
  };

  const loadCompanies = async (companyIds: number[]) => {
    const missingIds = Array.from(new Set(companyIds.filter(Boolean))).filter((companyId) => !companiesById[companyId]);
    if (missingIds.length === 0) return;

    const entries = await Promise.all(
      missingIds.map(async (companyId) => {
        try {
          const company = await getCompanyPublicProfile(companyId);
          return [companyId, company] as const;
        } catch {
          return undefined;
        }
      })
    );

    setCompaniesById((current) => ({
      ...current,
      ...Object.fromEntries(entries.filter(Boolean) as Array<readonly [number, CompanyPublicProfile]>),
    }));
  };

  return (
    <div className="flex h-screen bg-[#CFEFF3] font-sans text-slate-800 overflow-hidden">
      {/* Desktop Sidebar */}
      <aside className="hidden md:flex flex-col w-64 bg-white shadow-lg z-10">
        <div className="p-6 flex items-center gap-3">
          <img
            src={logoImage}
            alt="FIC Logo"
            className="w-8 h-8 rounded-lg object-contain"
          />
          <span className="text-xl font-bold text-slate-900">FreelanceHub</span>
        </div>
        
        <nav className="flex-1 px-4 space-y-2 mt-4">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-xl transition-colors ${
                  isActive
                    ? "bg-blue-50 text-blue-500 font-medium"
                    : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"
                }`
              }
            >
              <item.icon size={20} />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col h-full w-full relative">
        {/* Header */}
        <header className="relative z-50 h-20 bg-white/50 backdrop-blur-md border-b border-white/20 flex items-center justify-between px-6 shrink-0">
          <h1 className="text-2xl font-bold text-slate-800 hidden sm:block">{getPageTitle()}</h1>
          
          <div className="flex items-center gap-4 flex-1 sm:flex-none justify-end sm:justify-end">
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsNotificationsOpen((value) => !value)}
                className="relative p-2 rounded-full bg-white shadow-sm hover:bg-slate-50 transition-colors"
                aria-label="Notifications"
              >
                <Bell className="text-slate-600" size={20} />
                {notifications.length > 0 && (
                  <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-violet-500 rounded-full ring-2 ring-white"></span>
                )}
              </button>

              {isNotificationsOpen && (
                <div className="fixed right-6 top-16 z-[100] w-[360px] max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-2xl">
                  <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
                    <p className="text-sm font-bold text-slate-900">Notifications</p>
                    {notifications.length > 0 && (
                      <span className="rounded-full bg-blue-50 px-2 py-1 text-[10px] font-bold text-blue-600">
                        {notifications.length}
                      </span>
                    )}
                  </div>
                  {notifications.length === 0 ? (
                    <div className="px-4 py-6 text-center text-sm text-slate-500">Aucune notification pour le moment.</div>
                  ) : (
                    <div className="max-h-96 overflow-y-auto py-1">
                      {notifications.map((notification) => (
                        <button
                          key={notification.id}
                          type="button"
                          onClick={() => {
                            setIsNotificationsOpen(false);
                            navigate(notification.to);
                          }}
                          className="flex w-full gap-3 border-b border-slate-50 px-4 py-3 text-left transition-colors hover:bg-slate-50"
                        >
                          <span className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
                            notification.kind === "message"
                              ? "bg-blue-50 text-blue-600"
                              : notification.kind === "accepted"
                                ? "bg-emerald-50 text-emerald-600"
                              : "bg-emerald-50 text-emerald-600"
                          }`}>
                            {notification.kind === "message"
                              ? <MessageSquare size={17} />
                              : notification.kind === "accepted"
                                ? <CheckCircle2 size={17} />
                                : <Briefcase size={17} />}
                          </span>
                          <span className="min-w-0 flex-1">
                            <span className="block text-sm font-semibold text-slate-900">{notification.title}</span>
                            <span className="mt-1 block text-xs leading-relaxed text-slate-500">{notification.detail}</span>
                          </span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
            <button onClick={handleLogout} className="hidden sm:inline-flex items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm font-semibold text-red-600 transition-colors hover:bg-red-100">
              <LogOut size={16} />
              <span>Déconnexion</span>
            </button>
            <button onClick={handleLogout} className="inline-flex items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm font-semibold text-red-600 transition-colors hover:bg-red-100 sm:hidden">
              <LogOut size={16} />
              <span>Déconnexion</span>
            </button>
          </div>
        </header>

        {/* Scrollable Page Content */}
        <main className="flex-1 overflow-auto p-4 md:p-6 relative">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
