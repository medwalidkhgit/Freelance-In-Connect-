import { useEffect, useState } from "react";
import {
  Briefcase,
  Code,
  CreditCard,
  Download,
  Eye,
  FileText,
  Image,
  Loader2,
  MapPin,
  Save,
  Settings,
  Trash2,
  User,
  X,
} from "lucide-react";
import { ApiError } from "../../shared/api";
import {
  createMediaReadUrl,
  createFreelancerStripeAccount,
  getMyFreelancerProfile,
  updateMyFreelancerProfile,
  uploadCv,
  uploadProfilePicture,
  type FreelancerProfile,
} from "../../shared/services";

type ProfileForm = {
  firstName: string;
  lastName: string;
  phone: string;
  summary: string;
  skills: string;
};

export function Profile() {
  const [profile, setProfile] = useState<FreelancerProfile | null>(null);
  const [form, setForm] = useState<ProfileForm>({
    firstName: "",
    lastName: "",
    phone: "",
    summary: "",
    skills: "",
  });
  const [profilePicture, setProfilePicture] = useState<File | null>(null);
  const [cvFile, setCvFile] = useState<File | null>(null);
  const [profilePictureUrl, setProfilePictureUrl] = useState<string | undefined>();
  const [cvUrl, setCvUrl] = useState<string | undefined>();
  const [isPicturePreviewOpen, setIsPicturePreviewOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isStripeLoading, setIsStripeLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [stripeMessage, setStripeMessage] = useState("");

  useEffect(() => {
    void loadProfile();
  }, []);

  const loadProfile = async () => {
    setIsLoading(true);
    setMessage("");

    try {
      const data = await getMyFreelancerProfile();
      setProfile(data);
      setProfilePictureUrl(undefined);
      setCvUrl(undefined);
      setForm({
        firstName: data.firstName ?? "",
        lastName: data.lastName ?? "",
        phone: data.phone ?? "",
        summary: data.summary ?? "",
        skills: (data.skills ?? []).join(", "),
      });

      const [signedPicture, signedCv] = await Promise.all([
        data.pfpUrl ? createMediaReadUrl(data.pfpUrl).catch(() => undefined) : Promise.resolve(undefined),
        data.cvUrl ? createMediaReadUrl(data.cvUrl).catch(() => undefined) : Promise.resolve(undefined),
      ]);

      setProfilePictureUrl(signedPicture?.signedUrl ?? data.pfpUrl);
      setCvUrl(signedCv?.signedUrl ?? data.cvUrl);
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Impossible de charger le profil.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    setMessage("");

    try {
      const [pictureUpload, cvUpload] = await Promise.all([
        profilePicture ? uploadProfilePicture(profilePicture) : Promise.resolve(undefined),
        cvFile ? uploadCv(cvFile) : Promise.resolve(undefined),
      ]);

      await updateMyFreelancerProfile({
        ...buildProfilePayload(),
        pfpUrl: pictureUpload?.url ?? profile?.pfpUrl,
        cvUrl: cvUpload?.url ?? profile?.cvUrl,
      });

      setMessage("Profil mis a jour.");
      setProfilePicture(null);
      setCvFile(null);
      await loadProfile();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Mise a jour impossible pour le moment.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeletePicture = async () => {
    setIsSaving(true);
    setMessage("");

    try {
      await updateMyFreelancerProfile({
        ...buildProfilePayload(),
        pfpUrl: null,
        cvUrl: profile?.cvUrl ?? null,
      });
      setProfilePicture(null);
      setIsPicturePreviewOpen(false);
      setMessage("Photo de profil supprimee.");
      await loadProfile();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Suppression impossible pour le moment.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteCv = async () => {
    setIsSaving(true);
    setMessage("");

    try {
      await updateMyFreelancerProfile({
        ...buildProfilePayload(),
        pfpUrl: profile?.pfpUrl ?? null,
        cvUrl: null,
      });
      setCvFile(null);
      setMessage("CV supprime.");
      await loadProfile();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Suppression impossible pour le moment.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleCreateStripeAccount = async () => {
    const freelancerKeycloakId = profile?.keycloakUserId;
    if (!freelancerKeycloakId) {
      setStripeMessage("Identifiant Keycloak freelancer introuvable. Rechargez le profil puis reessayez.");
      return;
    }

    setIsStripeLoading(true);
    setMessage("");
    setStripeMessage("Connexion a Stripe en cours...");

    try {
      const stripeAccount = await createFreelancerStripeAccount(freelancerKeycloakId, profile.email);
      if (!stripeAccount.onboardingUrl) {
        setStripeMessage("Stripe n'a pas retourne de lien d'onboarding.");
        return;
      }
      setStripeMessage("Compte Stripe cree. Redirection vers l'onboarding...");
      window.location.href = stripeAccount.onboardingUrl;
    } catch (err) {
      const errorMessage =
        err instanceof ApiError
          ? err.message
          : err instanceof TypeError
            ? "Appel PaymentAPI bloque. Verifiez CORS/OPTIONS dans WSO2 pour /stripe/accounts/freelancers/{freelancerId}."
            : "Creation du compte Stripe impossible.";
      setStripeMessage(errorMessage);
    } finally {
      setIsStripeLoading(false);
    }
  };

  const buildProfilePayload = () => ({
    firstName: form.firstName,
    lastName: form.lastName,
    phone: form.phone,
    summary: form.summary,
    skills: form.skills
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean),
  });

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center text-slate-500">
        <Loader2 className="mr-2 animate-spin" size={20} />
        Chargement du profil...
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="mx-auto flex h-full max-w-2xl items-center justify-center">
        <div className="w-full rounded-2xl border border-red-100 bg-white p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500">
            <User size={28} />
          </div>
          <h2 className="text-xl font-bold text-slate-900">Profil indisponible</h2>
          <p className="mt-2 text-sm text-slate-500">
            {message || "Impossible de charger votre profil freelancer pour le moment."}
          </p>
          <button
            type="button"
            onClick={() => void loadProfile()}
            className="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-blue-500 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600"
          >
            <Loader2 size={16} />
            Recharger
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6 flex flex-col md:flex-row items-center md:items-start gap-6">
        <div className="relative shrink-0">
          {profilePictureUrl ? (
            <button
              type="button"
              onClick={() => setIsPicturePreviewOpen(true)}
              className="block rounded-2xl focus:outline-none focus:ring-4 focus:ring-blue-500/20"
            >
              <img
                src={profilePictureUrl}
                alt={`${profile.firstName} ${profile.lastName}`}
                onError={() => setProfilePictureUrl(undefined)}
                className="w-24 h-24 rounded-2xl object-cover shadow-md bg-slate-100"
              />
            </button>
          ) : (
            <div className="w-24 h-24 rounded-2xl bg-blue-50 text-blue-600 shadow-md flex items-center justify-center text-3xl font-bold">
              {initials(profile)}
            </div>
          )}
          <div className="absolute -bottom-2 -right-2 bg-blue-500 text-white p-1.5 rounded-lg shadow-sm border-2 border-white">
            <Settings size={14} />
          </div>
        </div>

        <div className="flex-1 text-center md:text-left">
          <div className="flex flex-col md:flex-row md:justify-between md:items-start gap-4 mb-2 w-full">
            <div>
              <h1 className="text-2xl font-bold text-slate-800">
                {profile.firstName} {profile.lastName}
              </h1>
              <p className="text-blue-500 font-medium">{profile.email}</p>
            </div>
            {cvUrl && (
              <a
                href={cvUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-center justify-center gap-2 px-4 py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 font-semibold rounded-xl border border-slate-200 transition-colors"
              >
                <Download size={16} />
                CV
              </a>
            )}
          </div>

          <div className="flex flex-wrap items-center justify-center md:justify-start gap-4 mb-4 text-sm text-slate-500 font-medium">
            <span className="flex items-center gap-1.5">
              <MapPin size={16} className="text-slate-400" /> Profil freelancer
            </span>
            {profile.phone && (
              <span className="flex items-center gap-1.5">
                <FileText size={16} className="text-slate-400" /> {profile.phone}
              </span>
            )}
          </div>

          <p className="text-sm text-slate-600 max-w-3xl leading-relaxed">
            {profile.summary || "Completez votre resume professionnel pour mieux presenter votre profil."}
          </p>
        </div>
      </div>

      {message && (
        <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
          {message}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="space-y-6">
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 className="text-lg font-bold text-slate-800 mb-5 flex items-center gap-2">
              <Code size={20} className="text-violet-500" />
              Competences
            </h2>
            <div className="flex flex-wrap gap-2">
              {(profile?.skills ?? []).length > 0 ? (
                profile?.skills?.map((skill) => (
                  <span key={skill} className="bg-blue-50 text-blue-600 text-xs font-semibold px-3 py-1.5 rounded-lg border border-blue-100/50">
                    {skill}
                  </span>
                ))
              ) : (
                <p className="text-sm text-slate-500">Aucune competence renseignee.</p>
              )}
            </div>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 className="text-lg font-bold text-slate-800 mb-5 flex items-center gap-2">
              <Briefcase size={20} className="text-blue-500" />
              Ressources
            </h2>
            <div className="space-y-4">
              <ExistingPicture
                imageUrl={profilePictureUrl}
                hasStoredImage={Boolean(profile?.pfpUrl)}
                fullName={`${profile?.firstName ?? ""} ${profile?.lastName ?? ""}`.trim()}
                onPreview={() => setIsPicturePreviewOpen(true)}
                onDelete={handleDeletePicture}
                disabled={isSaving}
              />
              <ExistingCv cvUrl={cvUrl} onDelete={handleDeleteCv} disabled={isSaving} />
              <FileInput label="Nouvelle photo de profil" accept="image/*" onChange={setProfilePicture} />
              <FileInput label="Nouveau CV PDF" accept="application/pdf" onChange={setCvFile} />
            </div>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 className="text-lg font-bold text-slate-800 mb-3 flex items-center gap-2">
              <CreditCard size={20} className="text-blue-500" />
              Paiements
            </h2>
            <p className="mb-4 text-sm text-slate-500">
              Connectez votre compte Stripe pour recevoir les virements des missions acceptees par les entreprises.
            </p>
            <button
              type="button"
              onClick={() => void handleCreateStripeAccount()}
              disabled={isStripeLoading}
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-blue-500 px-4 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600 disabled:bg-slate-300"
            >
              {isStripeLoading ? <Loader2 size={18} className="animate-spin" /> : <CreditCard size={18} />}
              {isStripeLoading ? "Ouverture..." : "Connecter mon compte Stripe"}
            </button>
            {stripeMessage && (
              <div className="mt-3 rounded-xl border border-blue-100 bg-blue-50 px-3 py-2 text-xs font-semibold text-blue-700">
                {stripeMessage}
              </div>
            )}
          </div>
        </div>

        <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-slate-100 p-6 space-y-5">
          <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <User size={20} className="text-blue-500" />
            Informations personnelles
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <TextInput label="Prenom" value={form.firstName} onChange={(value) => setForm({ ...form, firstName: value })} />
            <TextInput label="Nom" value={form.lastName} onChange={(value) => setForm({ ...form, lastName: value })} />
          </div>

          <TextInput label="Telephone" value={form.phone} onChange={(value) => setForm({ ...form, phone: value })} />

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700">Resume professionnel</label>
            <textarea
              value={form.summary}
              onChange={(event) => setForm({ ...form, summary: event.target.value })}
              className="min-h-28 w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
            />
          </div>

          <TextInput
            label="Competences"
            value={form.skills}
            onChange={(value) => setForm({ ...form, skills: value })}
            helper="Separez les competences par des virgules."
          />

          <button
            onClick={handleSave}
            disabled={isSaving}
            className="inline-flex items-center gap-2 rounded-xl bg-blue-500 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-colors hover:bg-blue-600 disabled:bg-slate-300"
          >
            <Save size={18} />
            {isSaving ? "Enregistrement..." : "Enregistrer"}
          </button>
        </div>
      </div>

      {isPicturePreviewOpen && profilePictureUrl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4">
          <div className="relative max-h-[90vh] w-full max-w-3xl rounded-2xl bg-white p-3 shadow-2xl">
            <button
              type="button"
              onClick={() => setIsPicturePreviewOpen(false)}
              className="absolute right-4 top-4 rounded-full bg-white/90 p-2 text-slate-700 shadow-sm transition-colors hover:bg-white"
              aria-label="Fermer l'aperçu"
            >
              <X size={20} />
            </button>
            <img
              src={profilePictureUrl}
              alt={`${profile?.firstName ?? ""} ${profile?.lastName ?? ""}`.trim()}
              className="max-h-[84vh] w-full rounded-xl object-contain"
            />
          </div>
        </div>
      )}
    </div>
  );
}

function initials(profile: FreelancerProfile | null) {
  const first = profile?.firstName?.[0] ?? "";
  const last = profile?.lastName?.[0] ?? "";
  return `${first}${last}`.toUpperCase() || "FR";
}

type TextInputProps = {
  label: string;
  value: string;
  helper?: string;
  onChange: (value: string) => void;
};

function TextInput({ label, value, helper, onChange }: TextInputProps) {
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

type FileInputProps = {
  label: string;
  accept: string;
  onChange: (file: File | null) => void;
};

function FileInput({ label, accept, onChange }: FileInputProps) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-slate-700">{label}</label>
      <input
        type="file"
        accept={accept}
        onChange={(event) => onChange(event.target.files?.[0] ?? null)}
        className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 file:mr-4 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-blue-600"
      />
    </div>
  );
}

type ExistingPictureProps = {
  imageUrl?: string;
  hasStoredImage: boolean;
  fullName: string;
  disabled: boolean;
  onPreview: () => void;
  onDelete: () => void;
};

function ExistingPicture({ imageUrl, hasStoredImage, fullName, disabled, onPreview, onDelete }: ExistingPictureProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
        <Image size={16} className="text-blue-500" />
        Photo actuelle
      </div>
      {imageUrl ? (
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onPreview}
            className="h-16 w-16 shrink-0 overflow-hidden rounded-xl border border-slate-200 focus:outline-none focus:ring-4 focus:ring-blue-500/20"
          >
            <img src={imageUrl} alt={fullName || "Photo de profil"} className="h-full w-full object-cover" />
          </button>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-slate-800">{fullName || "Photo de profil"}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={onPreview}
                className="inline-flex items-center gap-1.5 rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 ring-1 ring-slate-200 hover:bg-slate-100"
              >
                <Eye size={14} />
                Voir
              </button>
              <button
                type="button"
                onClick={onDelete}
                disabled={disabled}
                className="inline-flex items-center gap-1.5 rounded-lg bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-600 ring-1 ring-red-100 hover:bg-red-100 disabled:opacity-60"
              >
                <Trash2 size={14} />
                Supprimer
              </button>
            </div>
          </div>
        </div>
      ) : (
        <p className="text-sm text-slate-500">
          {hasStoredImage ? "Photo indisponible pour le moment." : "Aucune photo enregistree."}
        </p>
      )}
    </div>
  );
}

type ExistingCvProps = {
  cvUrl?: string;
  disabled: boolean;
  onDelete: () => void;
};

function ExistingCv({ cvUrl, disabled, onDelete }: ExistingCvProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
        <FileText size={16} className="text-blue-500" />
        CV actuel
      </div>
      {cvUrl ? (
        <div className="flex flex-wrap gap-2">
          <a
            href={cvUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 ring-1 ring-slate-200 hover:bg-slate-100"
          >
            <Eye size={14} />
            Consulter
          </a>
          <a
            href={cvUrl}
            download
            className="inline-flex items-center gap-1.5 rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 ring-1 ring-slate-200 hover:bg-slate-100"
          >
            <Download size={14} />
            Telecharger
          </a>
          <button
            type="button"
            onClick={onDelete}
            disabled={disabled}
            className="inline-flex items-center gap-1.5 rounded-lg bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-600 ring-1 ring-red-100 hover:bg-red-100 disabled:opacity-60"
          >
            <Trash2 size={14} />
            Supprimer
          </button>
        </div>
      ) : (
        <p className="text-sm text-slate-500">Aucun CV enregistre.</p>
      )}
    </div>
  );
}
