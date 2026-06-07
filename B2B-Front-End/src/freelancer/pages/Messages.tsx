import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router";
import {
  ArrowLeft,
  CheckCheck,
  Loader2,
  MessageSquare,
  MoreVertical,
  Paperclip,
  Search,
  Send,
} from "lucide-react";
import { ApiError } from "../../shared/api";
import { getAccessToken } from "../../shared/auth";
import { MessagingSocket } from "../../shared/messagingSocket";
import {
  createConversation,
  createMediaReadUrl,
  getCompanyPublicProfile,
  getConversationMessages,
  getMyFreelancerProfile,
  listConversations,
  listPublishedMissions,
  sendChatMessage,
  type ChatMessage,
  type CompanyPublicProfile,
  type Conversation,
  type Mission,
} from "../../shared/services";

type ConversationView = Conversation & {
  missionTitle: string;
  companyName: string;
  companyLogo?: string;
};

export function Messages() {
  const { chatId } = useParams();
  const navigate = useNavigate();
  const socketRef = useRef<MessagingSocket | null>(null);

  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [missionsById, setMissionsById] = useState<Record<number, Mission>>({});
  const [companiesById, setCompaniesById] = useState<Record<number, CompanyPublicProfile>>({});
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState("");
  const [search, setSearch] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    void loadMessagingData();

    return () => {
      socketRef.current?.disconnect();
    };
  }, []);

  useEffect(() => {
    if (!chatId?.startsWith("new-") || isLoading) return;
    void createConversationFromMission(chatId.replace("new-", ""));
  }, [chatId, isLoading]);

  const conversationViews = useMemo(
    () =>
      conversations.map((conversation) => {
        const mission = missionsById[conversation.missionId];
        const company = companiesById[conversation.companyId];
        return {
          ...conversation,
          missionTitle: mission?.title ?? `Mission #${conversation.missionId}`,
          companyName: company?.companyName ?? `Entreprise #${conversation.companyId}`,
          companyLogo: company?.pfpUrl,
        };
      }),
    [companiesById, conversations, missionsById]
  );

  const routeConversationId = chatId && !chatId.startsWith("new-") ? chatId : undefined;
  const selectedConversationId = routeConversationId ?? (!chatId ? conversationViews[0]?.id : undefined);
  const activeConversation = selectedConversationId
    ? conversationViews.find((conversation) => conversation.id === selectedConversationId)
    : undefined;

  useEffect(() => {
    if (!selectedConversationId) return;
    void loadConversation(selectedConversationId);
  }, [selectedConversationId]);

  const visibleConversations = useMemo(() => {
    const value = search.trim().toLowerCase();
    if (!value) return conversationViews;
    return conversationViews.filter(
      (conversation) =>
        conversation.missionTitle.toLowerCase().includes(value) ||
        conversation.companyName.toLowerCase().includes(value)
    );
  }, [conversationViews, search]);

  const loadMessagingData = async () => {
    setIsLoading(true);
    setError("");

    try {
      const [conversationList, missions] = await Promise.all([
        listConversations(),
        listPublishedMissions().catch(() => [] as Mission[]),
      ]);

      setConversations(conversationList);
      setMissionsById(Object.fromEntries(missions.map((mission) => [mission.id, mission])));
      await loadCompanies(conversationList);

      if (!chatId && conversationList[0]) {
        navigate(`/freelancer/messages/${conversationList[0].id}`, { replace: true });
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Impossible de charger la messagerie.");
    } finally {
      setIsLoading(false);
    }
  };

  const loadCompanies = async (conversationList: Conversation[]) => {
    const companyIds = Array.from(new Set(conversationList.map((conversation) => conversation.companyId)));
    if (companyIds.length === 0) return;

    const entries = await Promise.all(
      companyIds.map(async (companyId) => {
        try {
          const company = await getCompanyPublicProfile(companyId);
          const signedLogo = company.pfpUrl
            ? await createMediaReadUrl(company.pfpUrl).catch(() => undefined)
            : undefined;
          if (signedLogo?.signedUrl) {
            company.pfpUrl = signedLogo.signedUrl;
          }
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

  const loadConversation = async (conversationId: string) => {
    setIsHistoryLoading(true);
    setError("");

    try {
      const history = await getConversationMessages(conversationId);
      setMessages(history);
      subscribeToConversation(conversationId);
    } catch (err) {
      setMessages([]);
      setError(err instanceof ApiError ? err.message : "Impossible de charger les messages.");
    } finally {
      setIsHistoryLoading(false);
    }
  };

  const createConversationFromMission = async (missionIdText: string) => {
    const missionId = Number(missionIdText);
    if (!Number.isFinite(missionId)) {
      setError("Mission invalide.");
      return;
    }

    setIsHistoryLoading(true);
    setError("");

    try {
      const mission = missionsById[missionId] ?? (await listPublishedMissions()).find((item) => item.id === missionId);
      if (!mission?.companyId) {
        throw new Error("Mission introuvable.");
      }

      const profile = await getMyFreelancerProfile();
      const conversation = await createConversation({
        missionId,
        companyId: mission.companyId,
        freelancerId: profile.id,
      });

      setConversations((current) =>
        current.some((item) => item.id === conversation.id) ? current : [conversation, ...current]
      );
      setMissionsById((current) => ({ ...current, [mission.id]: mission }));
      await loadCompanies([conversation]);
      navigate(`/freelancer/messages/${conversation.id}`, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Conversation impossible.");
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

  const handleSend = async () => {
    const content = inputText.trim();
    if (!content || !activeConversation || isSending) return;

    setIsSending(true);
    setError("");

    try {
      const sent = await sendChatMessage(activeConversation.id, content);
      setMessages((current) =>
        current.some((message) => message.id === sent.id) ? current : [...current, sent]
      );
      setInputText("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Message non envoye.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="grid h-full w-full grid-cols-1 md:grid-cols-[350px_minmax(0,1fr)] bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden relative">
      <div className={`min-w-0 flex-col border-r border-slate-100 bg-slate-50/50 ${activeConversation ? "hidden md:flex" : "flex"}`}>
        <div className="p-4 border-b border-slate-100 bg-white">
          <h2 className="text-xl font-bold text-slate-800 mb-4">Messagerie</h2>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Rechercher une conversation..."
              className="w-full pl-9 pr-4 py-2 bg-slate-100 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {isLoading ? (
            <EmptyState icon={<Loader2 className="animate-spin" size={32} />} title="Chargement..." />
          ) : visibleConversations.length === 0 ? (
            <EmptyState
              icon={<MessageSquare size={36} />}
              title="Aucune conversation"
              detail="Ouvrez une mission puis utilisez le bouton Ecrire a l'entreprise."
              actionLabel="Voir les missions"
              onAction={() => navigate("/freelancer/missions")}
            />
          ) : (
            visibleConversations.map((conversation) => (
              <ConversationRow
                key={conversation.id}
                conversation={conversation}
                active={selectedConversationId === conversation.id}
                lastMessage={lastMessageFor(conversation.id, messages)}
                onClick={() => navigate(`/freelancer/messages/${conversation.id}`)}
              />
            ))
          )}
        </div>
      </div>

      <div className={`min-w-0 flex-col bg-white ${activeConversation ? "flex" : "hidden md:flex"}`}>
        <div className="h-[72px] px-6 border-b border-slate-100 flex items-center justify-between bg-white flex-shrink-0">
          <div className="flex items-center gap-3 min-w-0">
            <button
              className="md:hidden p-2 -ml-2 text-slate-500 hover:bg-slate-100 rounded-full"
              onClick={() => navigate("/freelancer/messages")}
            >
              <ArrowLeft size={20} />
            </button>

            {activeConversation ? (
              <ConversationHeader conversation={activeConversation} />
            ) : (
              <div>
                <h2 className="font-bold text-slate-800 leading-tight">Messagerie</h2>
                <p className="text-xs text-slate-500 font-medium">Selectionnez une conversation</p>
              </div>
            )}
          </div>
          <button className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-full">
            <MoreVertical size={20} />
          </button>
        </div>

        {error && (
          <div className="mx-4 mt-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
            {error}
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-6 bg-[#F8FAFC]">
          {isHistoryLoading ? (
            <EmptyState icon={<Loader2 className="animate-spin" size={40} />} title="Chargement des messages..." />
          ) : !activeConversation ? (
            <EmptyState
              icon={<MessageSquare size={48} />}
              title="Choisissez une conversation"
              detail="Aucune conversation n'est active. Selectionnez une conversation a gauche ou contactez une entreprise depuis une mission."
              actionLabel="Voir les missions"
              onAction={() => navigate("/freelancer/missions")}
            />
          ) : messages.length === 0 ? (
            <EmptyState
              icon={<MessageSquare size={48} />}
              title="Aucun message"
              detail={`Envoyez votre premier message pour "${activeConversation.missionTitle}".`}
            />
          ) : (
            <div className="space-y-6">
              {messages.map((message) => (
                <MessageBubble key={message.id} message={message} />
              ))}
            </div>
          )}
        </div>

        <div className="p-4 border-t border-slate-100 bg-white">
          <div className="flex items-end gap-2 bg-slate-50 rounded-2xl border border-slate-200 p-2 focus-within:ring-2 focus-within:ring-blue-500/20 focus-within:border-blue-500 transition-all">
            <button className="p-2 text-slate-400 hover:text-blue-500 transition-colors shrink-0">
              <Paperclip size={20} />
            </button>
            <textarea
              value={inputText}
              onChange={(event) => setInputText(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  void handleSend();
                }
              }}
              placeholder={activeConversation ? "Ecrivez votre message..." : "Selectionnez une conversation pour ecrire..."}
              className="flex-1 max-h-32 min-h-[40px] bg-transparent border-none resize-none focus:outline-none text-sm text-slate-700 py-2.5 px-2"
              rows={1}
              disabled={!activeConversation || isSending}
            />
            <button
              onClick={() => void handleSend()}
              disabled={!inputText.trim() || !activeConversation || isSending}
              className="p-3 bg-blue-500 hover:bg-blue-600 disabled:bg-slate-200 disabled:text-slate-400 text-white rounded-xl transition-colors shrink-0 flex items-center justify-center shadow-sm"
            >
              {isSending ? <Loader2 size={18} className="animate-spin" /> : <Send size={18} className="translate-x-0.5 -translate-y-0.5" />}
            </button>
          </div>
          <p className="text-[10px] text-slate-400 text-center mt-2">Appuyez sur Entree pour envoyer</p>
        </div>
      </div>
    </div>
  );
}

function ConversationRow({
  conversation,
  active,
  lastMessage,
  onClick,
}: {
  conversation: ConversationView;
  active: boolean;
  lastMessage?: ChatMessage;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full p-4 border-b border-slate-100 cursor-pointer transition-colors hover:bg-slate-100 flex items-start gap-3 text-left ${
        active ? "bg-blue-50 hover:bg-blue-50 border-l-4 border-l-blue-500" : "border-l-4 border-l-transparent"
      }`}
    >
      <CompanyAvatar imageUrl={conversation.companyLogo} name={conversation.companyName} />
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-baseline mb-1">
          <h3 className="font-semibold text-slate-900 truncate pr-2">{conversation.companyName}</h3>
          <span className="text-xs text-slate-400 whitespace-nowrap">{formatTime(lastMessage?.sentAt ?? conversation.createdAt)}</span>
        </div>
        <p className="text-xs text-blue-500 font-medium truncate mb-1">{conversation.missionTitle}</p>
        <p className="text-sm text-slate-500 truncate">{lastMessage?.content ?? "Aucun message pour le moment."}</p>
      </div>
    </button>
  );
}

function ConversationHeader({ conversation }: { conversation: ConversationView }) {
  return (
    <div className="flex items-center gap-3 min-w-0">
      <CompanyAvatar imageUrl={conversation.companyLogo} name={conversation.companyName} small />
      <div className="flex flex-col min-w-0">
        <h2 className="font-bold text-slate-800 leading-tight truncate">Mission : {conversation.missionTitle}</h2>
        <p className="text-xs text-slate-500 font-medium truncate">avec {conversation.companyName}</p>
      </div>
    </div>
  );
}

function MessageBubble({ message }: { message: ChatMessage }) {
  const isFreelancer = message.senderRole === "FREELANCER";

  return (
    <div className={`flex flex-col ${isFreelancer ? "items-end" : "items-start"}`}>
      <div className="mb-1 px-1 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
        {message.senderRole}
      </div>
      <div
        className={`max-w-[85%] md:max-w-[70%] rounded-2xl p-4 shadow-sm ${
          isFreelancer
            ? "bg-blue-500 text-white rounded-tr-sm"
            : "bg-white text-slate-700 border border-slate-100 rounded-tl-sm"
        }`}
      >
        <p className="text-sm leading-relaxed whitespace-pre-line">{message.content}</p>
      </div>
      <div className="mt-1.5 flex items-center gap-1.5 text-xs text-slate-400 px-1">
        <span>{formatTime(message.sentAt)}</span>
        {isFreelancer && <CheckCheck size={14} className="text-blue-500" />}
      </div>
    </div>
  );
}

function CompanyAvatar({ imageUrl, name, small = false }: { imageUrl?: string; name: string; small?: boolean }) {
  const size = small ? "w-10 h-10" : "w-12 h-12";
  if (imageUrl) {
    return <img src={imageUrl} alt={name} className={`${size} rounded-xl object-cover shadow-sm bg-slate-100`} />;
  }

  return (
    <div className={`${size} rounded-xl bg-blue-50 text-blue-600 shadow-sm flex items-center justify-center text-sm font-bold`}>
      {name.slice(0, 2).toUpperCase()}
    </div>
  );
}

function EmptyState({
  icon,
  title,
  detail,
  actionLabel,
  onAction,
}: {
  icon: React.ReactNode;
  title: string;
  detail?: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="h-full flex flex-col items-center justify-center text-slate-400 text-center p-6">
      <div className="mb-4 text-slate-300">{icon}</div>
      <p className="font-medium text-slate-500">{title}</p>
      {detail && <p className="mt-1 max-w-sm text-sm">{detail}</p>}
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-4 rounded-xl bg-blue-500 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}

function lastMessageFor(conversationId: string, activeMessages: ChatMessage[]) {
  const related = activeMessages.filter((message) => message.conversationId === conversationId);
  return related[related.length - 1];
}

function formatTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}
