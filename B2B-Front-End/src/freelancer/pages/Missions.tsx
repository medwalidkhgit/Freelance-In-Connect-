import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import {
  Briefcase,
  ChevronRight,
  Clock,
  Filter,
  Loader2,
  MapPin,
  MessageSquare,
  Search,
  Wallet,
  X,
} from "lucide-react";
import { ApiError } from "../../shared/api";
import {
  applyToMission,
  createMediaReadUrl,
  getCompanyPublicProfile,
  listPublishedMissions,
  searchMissions,
  type CompanyPublicProfile,
  type Mission,
} from "../../shared/services";

const workModeLabels: Record<string, string> = {
  REMOTE: "Remote",
  PRESENTIEL: "Presentiel",
  HYBRIDE: "Hybride",
};

export function Missions() {
  const navigate = useNavigate();
  const [missions, setMissions] = useState<Mission[]>([]);
  const [companiesById, setCompaniesById] = useState<Record<number, CompanyPublicProfile>>({});
  const [selectedMission, setSelectedMission] = useState<Mission | null>(null);
  const [keyword, setKeyword] = useState("");
  const [skill, setSkill] = useState("");
  const [workMode, setWorkMode] = useState("");
  const [budgetPreset, setBudgetPreset] = useState("");
  const [minBudget, setMinBudget] = useState("");
  const [maxBudget, setMaxBudget] = useState("");
  const [coverLetter, setCoverLetter] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isApplying, setIsApplying] = useState(false);
  const [error, setError] = useState("");
  const [applyMessage, setApplyMessage] = useState("");

  useEffect(() => {
    void loadMissions();
  }, []);

  const availableSkills = useMemo(() => {
    const unique = new Set<string>();
    missions.forEach((mission) => {
      mission.requiredSkills?.forEach((item) => unique.add(item));
    });
    return Array.from(unique).sort();
  }, [missions]);

  const loadMissions = async () => {
    setIsLoading(true);
    setError("");

    try {
      const data = await listPublishedMissions();
      setMissions(data);
      setSelectedMission((current) => current ?? data[0] ?? null);
      void loadCompanies(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Impossible de charger les missions.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = async () => {
    setIsLoading(true);
    setError("");

    try {
      const budgetRange = resolveBudgetRange(budgetPreset, minBudget, maxBudget);
      const data = await searchMissions({
        skill,
        workMode,
        minBudget: budgetRange.minBudget,
        maxBudget: budgetRange.maxBudget,
      });
      const companies = await loadCompanies(data);
      const filtered = filterMissionsByKeyword(data, companies, keyword);
      setMissions(filtered);
      setSelectedMission(filtered[0] ?? null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Recherche impossible pour le moment.");
    } finally {
      setIsLoading(false);
    }
  };

  const loadCompanies = async (missionList: Mission[]) => {
    const companyIds = Array.from(new Set(missionList.map((mission) => mission.companyId).filter(Boolean)));
    const missingIds = companyIds.filter((companyId) => !companiesById[companyId]);
    if (missingIds.length === 0) return companiesById;

    const entries = await Promise.all(
      missingIds.map(async (companyId) => {
        try {
          const company = await getCompanyPublicProfile(companyId);
          const signedLogo = company.pfpUrl
            ? await createMediaReadUrl(company.pfpUrl).catch(() => undefined)
            : undefined;
          return [companyId, { ...company, pfpUrl: signedLogo?.signedUrl ?? company.pfpUrl }] as const;
        } catch {
          return undefined;
        }
      })
    );

    const loadedCompanies = Object.fromEntries(entries.filter(Boolean) as Array<readonly [number, CompanyPublicProfile]>);
    const nextCompanies = {
      ...companiesById,
      ...loadedCompanies,
    };
    setCompaniesById(nextCompanies);
    return nextCompanies;
  };

  const handleApply = async () => {
    if (!selectedMission) return;

    setIsApplying(true);
    setApplyMessage("");

    try {
      await applyToMission({
        missionId: selectedMission.id,
        coverLetter:
          coverLetter.trim() ||
          `Bonjour, je souhaite postuler a la mission "${selectedMission.title}".`,
      });
      setApplyMessage("Candidature envoyee.");
      setCoverLetter("");
    } catch (err) {
      setApplyMessage(err instanceof ApiError ? err.message : "Candidature impossible pour le moment.");
    } finally {
      setIsApplying(false);
    }
  };

  const selectedCompany = selectedMission ? companiesById[selectedMission.companyId] : undefined;

  return (
    <div className="flex h-full w-full relative">
      <div className={`flex-1 flex flex-col transition-all duration-300 ${selectedMission ? "lg:mr-[400px] xl:mr-[500px]" : ""}`}>
        <div className="bg-white p-4 rounded-2xl shadow-sm mb-6 flex flex-wrap gap-3 items-center">
          <div className="flex items-center gap-2 text-slate-500 font-medium px-2">
            <Filter size={18} />
            <span>Filtres :</span>
          </div>

          <div className="relative min-w-[220px] flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Mot cle..."
              className="w-full bg-slate-50 border border-slate-100 text-sm rounded-lg pl-9 pr-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <select
            value={skill}
            onChange={(event) => setSkill(event.target.value)}
            className="bg-slate-50 border border-slate-100 text-sm rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
          >
            <option value="">Toutes les competences</option>
            {availableSkills.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>

          <select
            value={workMode}
            onChange={(event) => setWorkMode(event.target.value)}
            className="bg-slate-50 border border-slate-100 text-sm rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
          >
            <option value="">Tous les modes</option>
            <option value="REMOTE">Remote</option>
            <option value="PRESENTIEL">Presentiel</option>
            <option value="HYBRIDE">Hybride</option>
          </select>

          <select
            value={budgetPreset}
            onChange={(event) => setBudgetPreset(event.target.value)}
            className="bg-slate-50 border border-slate-100 text-sm rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
          >
            <option value="">Tous les budgets</option>
            <option value="lt5000">Inferieur a 5 000 MAD</option>
            <option value="5000-10000">5 000 - 10 000 MAD</option>
            <option value="10000-25000">10 000 - 25 000 MAD</option>
            <option value="gt25000">Superieur a 25 000 MAD</option>
            <option value="custom">Intervalle personnalise</option>
          </select>

          {budgetPreset === "custom" && (
            <div className="flex items-center gap-2">
              <input
                type="number"
                min="0"
                value={minBudget}
                onChange={(event) => setMinBudget(event.target.value)}
                placeholder="Min"
                className="w-24 bg-slate-50 border border-slate-100 text-sm rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="number"
                min="0"
                value={maxBudget}
                onChange={(event) => setMaxBudget(event.target.value)}
                placeholder="Max"
                className="w-24 bg-slate-50 border border-slate-100 text-sm rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          )}

          <button
            onClick={handleSearch}
            className="bg-blue-500 hover:bg-blue-600 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            Rechercher
          </button>
        </div>

        {error && (
          <div className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </div>
        )}

        {isLoading ? (
          <div className="flex flex-1 items-center justify-center text-slate-500">
            <Loader2 className="mr-2 animate-spin" size={20} />
            Chargement des missions...
          </div>
        ) : missions.length === 0 ? (
          <div className="flex flex-1 items-center justify-center rounded-2xl bg-white p-10 text-center text-slate-500">
            Aucune mission publiee ne correspond aux filtres.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-5 pb-8">
            {missions.map((mission) => (
              <button
                key={mission.id}
                onClick={() => {
                  setSelectedMission(mission);
                  setApplyMessage("");
                }}
                className={`text-left bg-white rounded-2xl p-5 cursor-pointer transition-all duration-200 border-2 ${
                  selectedMission?.id === mission.id
                    ? "border-blue-500 shadow-md -translate-y-1"
                    : "border-transparent shadow-sm hover:shadow-md hover:-translate-y-1"
                }`}
              >
                <div className="flex justify-between items-start mb-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <CompanyAvatar
                      imageUrl={companiesById[mission.companyId]?.pfpUrl}
                      name={companiesById[mission.companyId]?.companyName ?? `Entreprise #${mission.companyId}`}
                    />
                    <div className="min-w-0">
                    <h3 className="truncate text-sm font-semibold text-slate-900">
                      {companiesById[mission.companyId]?.companyName ?? `Entreprise #${mission.companyId}`}
                    </h3>
                    <p className="text-xs text-slate-500">{formatDate(mission.createdAt)}</p>
                    </div>
                  </div>
                  <span className="bg-emerald-50 text-emerald-600 text-[10px] font-bold px-2 py-1 rounded-full uppercase tracking-wide">
                    {mission.status ?? "PUBLIEE"}
                  </span>
                </div>

                <h2 className="text-lg font-bold text-slate-800 mb-3 leading-tight line-clamp-2 min-h-[44px]">
                  {mission.title}
                </h2>

                <div className="flex flex-wrap gap-2 mb-4">
                  {(mission.requiredSkills ?? []).slice(0, 4).map((item) => (
                    <span key={item} className="bg-violet-50 text-violet-600 text-xs font-medium px-2.5 py-1 rounded-md">
                      {item}
                    </span>
                  ))}
                </div>

                <div className="space-y-2 mb-4 pt-4 border-t border-slate-100">
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <MapPin size={16} className="text-slate-400" />
                    <span>{mission.workMode ? workModeLabels[mission.workMode] : "Non precise"}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <Wallet size={16} className="text-slate-400" />
                    <span className="font-semibold text-slate-800">{formatBudget(mission.budget)}</span>
                  </div>
                </div>

                <span className="text-sm text-blue-500 font-medium flex items-center gap-1">
                  Voir details <ChevronRight size={16} />
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {selectedMission && (
        <>
          <div
            className="fixed inset-0 bg-slate-900/20 backdrop-blur-sm z-40 lg:hidden"
            onClick={() => setSelectedMission(null)}
          />

          <div className="fixed inset-y-0 right-0 w-full md:w-[400px] xl:w-[500px] bg-white shadow-2xl z-50 flex flex-col border-l border-slate-100 animate-in slide-in-from-right duration-300 lg:absolute lg:h-full lg:shadow-[-10px_0_30px_rgba(0,0,0,0.05)]">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between sticky top-0 bg-white z-10">
              <h3 className="font-semibold text-slate-800">Detail de la mission</h3>
              <button
                onClick={() => setSelectedMission(null)}
                className="p-2 bg-slate-50 hover:bg-slate-100 text-slate-500 rounded-full transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              <div className="mb-6">
                <div className="mb-4 flex items-center gap-3">
                  <CompanyAvatar
                    imageUrl={selectedCompany?.pfpUrl}
                    name={selectedCompany?.companyName ?? `Entreprise #${selectedMission.companyId}`}
                    large
                  />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-blue-500">
                      {selectedCompany?.companyName ?? `Entreprise #${selectedMission.companyId}`}
                    </p>
                    {selectedCompany?.domaine && (
                      <p className="truncate text-xs font-medium text-slate-500">{selectedCompany.domaine}</p>
                    )}
                  </div>
                </div>
                <h1 className="text-xl font-bold text-slate-900 mb-2">{selectedMission.title}</h1>
                <p className="text-xs text-slate-500 font-medium">{formatDate(selectedMission.createdAt)}</p>
              </div>

              <div className="flex flex-wrap gap-2 mb-8">
                {(selectedMission.requiredSkills ?? []).map((item) => (
                  <span key={item} className="bg-violet-50 text-violet-600 text-sm font-medium px-3 py-1.5 rounded-lg border border-violet-100">
                    {item}
                  </span>
                ))}
              </div>

              <div className="grid grid-cols-2 gap-4 mb-8">
                <Metric icon={Wallet} label="Budget" value={formatBudget(selectedMission.budget)} />
                <Metric icon={Clock} label="Duree" value={formatDuration(selectedMission.durationDays)} />
                <Metric icon={MapPin} label="Mode" value={selectedMission.workMode ? workModeLabels[selectedMission.workMode] : "Non precise"} wide />
              </div>

              <div className="space-y-4">
                <h3 className="text-lg font-bold text-slate-800">Description</h3>
                <div className="text-slate-600 leading-relaxed whitespace-pre-line text-sm">
                  {selectedMission.description}
                </div>
              </div>

              <div className="mt-8 space-y-3">
                <label className="text-sm font-bold text-slate-800">Lettre de motivation</label>
                <textarea
                  value={coverLetter}
                  onChange={(event) => setCoverLetter(event.target.value)}
                  placeholder="Expliquez rapidement pourquoi vous etes adapte a cette mission..."
                  className="min-h-28 w-full resize-none rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                />
              </div>

              {applyMessage && (
                <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
                  {applyMessage}
                </div>
              )}
            </div>

            <div className="shrink-0 p-6 border-t border-slate-100 bg-white space-y-3">
              <button
                onClick={() => navigate(`/freelancer/messages/new-${selectedMission.id}`)}
                className="w-full bg-white hover:bg-slate-50 text-slate-700 font-semibold py-3 px-4 rounded-xl transition-colors border border-slate-200 flex items-center justify-center gap-2"
              >
                <MessageSquare size={18} />
                Ecrire a l'entreprise
              </button>
              <button
                onClick={handleApply}
                disabled={isApplying}
                className="w-full bg-blue-500 hover:bg-blue-600 disabled:bg-slate-300 text-white font-semibold py-3 px-4 rounded-xl transition-colors shadow-sm shadow-blue-500/20 flex items-center justify-center gap-2"
              >
                <Briefcase size={18} />
                {isApplying ? "Envoi..." : "Postuler directement"}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

type MetricProps = {
  icon: typeof Wallet;
  label: string;
  value: string;
  wide?: boolean;
};

function Metric({ icon: Icon, label, value, wide = false }: MetricProps) {
  return (
    <div className={`bg-slate-50 p-4 rounded-xl border border-slate-100 ${wide ? "col-span-2" : ""}`}>
      <div className="flex items-center gap-2 text-slate-500 mb-1">
        <Icon size={16} />
        <span className="text-xs uppercase font-bold tracking-wider">{label}</span>
      </div>
      <p className="font-semibold text-slate-900">{value}</p>
    </div>
  );
}

function formatBudget(value?: number) {
  if (value === undefined || value === null) return "Budget non precise";
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "MAD",
    maximumFractionDigits: 0,
  }).format(value);
}

function CompanyAvatar({
  imageUrl,
  name,
  large = false,
}: {
  imageUrl?: string;
  name: string;
  large?: boolean;
}) {
  const size = large ? "h-14 w-14" : "h-10 w-10";
  if (imageUrl) {
    return <img src={imageUrl} alt={name} className={`${size} shrink-0 rounded-xl bg-slate-100 object-cover shadow-sm`} />;
  }

  return (
    <div className={`${size} flex shrink-0 items-center justify-center rounded-xl bg-blue-50 text-sm font-bold text-blue-600 shadow-sm`}>
      {name.slice(0, 2).toUpperCase()}
    </div>
  );
}

function formatDuration(days?: number) {
  if (!days) return "Duree non precise";
  return `${days} jours`;
}

function formatDate(value?: string) {
  if (!value) return "Date non precisee";
  return new Intl.DateTimeFormat("fr-FR").format(new Date(value));
}

function resolveBudgetRange(preset: string, minValue: string, maxValue: string) {
  if (preset === "lt5000") return { maxBudget: 5000 };
  if (preset === "5000-10000") return { minBudget: 5000, maxBudget: 10000 };
  if (preset === "10000-25000") return { minBudget: 10000, maxBudget: 25000 };
  if (preset === "gt25000") return { minBudget: 25000 };
  if (preset !== "custom") return {};

  const minBudget = parsePositiveNumber(minValue);
  const maxBudget = parsePositiveNumber(maxValue);
  return {
    minBudget,
    maxBudget,
  };
}

function parsePositiveNumber(value: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function filterMissionsByKeyword(
  missionList: Mission[],
  companiesById: Record<number, CompanyPublicProfile>,
  keyword: string
) {
  const normalized = normalizeText(keyword);
  if (!normalized) return missionList;

  return missionList.filter((mission) => {
    const company = companiesById[mission.companyId];
    const haystack = [
      mission.title,
      mission.description,
      ...(mission.requiredSkills ?? []),
      company?.companyName,
      company?.domaine,
    ]
      .map(normalizeText)
      .join(" ");

    return haystack.includes(normalized);
  });
}

function normalizeText(value?: string) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}
