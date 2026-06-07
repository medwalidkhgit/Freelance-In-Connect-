import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { ArrowLeft, Building2 } from "lucide-react";
import { Input } from "./Input";
import { ApiError } from "../../../shared/api";
import { registerCompany, uploadRegistrationCompanyLogo } from "../../../shared/services";

interface CompanyFormData {
  companyName: string;
  siret: string;
  email: string;
  password: string;
  confirmPassword: string;
  contactFirstName: string;
  contactLastName: string;
  address: string;
  phone: string;
  domaine: string;
}

export function CompanyForm() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [logoFile, setLogoFile] = useState<File | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<CompanyFormData>();

  const password = watch("password");

  const onSubmit = async (data: CompanyFormData) => {
    setIsLoading(true);
    setSubmitError("");

    try {
      const siret = data.siret.replace(/\s/g, "");
      const logoUpload = logoFile ? await uploadRegistrationCompanyLogo(logoFile) : undefined;

      await registerCompany({
        email: data.email,
        password: data.password,
        companyName: data.companyName,
        siret,
        contactFirstName: data.contactFirstName,
        contactLastName: data.contactLastName,
        companyAddress: data.address,
        companyPhone: data.phone,
        domaine: data.domaine,
        pfpUrl: logoUpload?.url,
      });

      navigate("/sign-up/success/company");
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : "Inscription impossible pour le moment.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white p-6 md:p-8 rounded-2xl shadow-sm animate-in slide-in-from-bottom-4 duration-300">
      <button
        onClick={() => navigate("/sign-up")}
        className="flex items-center gap-2 text-sm text-slate-500 hover:text-slate-800 mb-6 transition-colors w-fit"
      >
        <ArrowLeft size={16} /> Retour
      </button>

      <div className="flex items-center gap-3 mb-8">
        <div className="w-10 h-10 bg-blue-100 text-blue-600 rounded-xl flex items-center justify-center">
          <Building2 size={20} />
        </div>
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Profil Entreprise</h2>
          <p className="text-sm text-slate-500">Creez votre compte pour recruter des talents.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <Input
          label="Nom de l'entreprise"
          placeholder="Tech Solutions Inc."
          {...register("companyName", { required: "Le nom de l'entreprise est requis" })}
          error={errors.companyName?.message}
        />

        <Input
          label="N SIRET"
          placeholder="12345678900012"
          {...register("siret", {
            required: "Le numero SIRET est requis",
            pattern: {
              value: /^[0-9\s]{14,17}$/,
              message: "Le format du SIRET est invalide",
            },
          })}
          error={errors.siret?.message}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <Input
            label="Prenom du contact"
            placeholder="Jean"
            {...register("contactFirstName", { required: "Le prenom du contact est requis" })}
            error={errors.contactFirstName?.message}
          />
          <Input
            label="Nom du contact"
            placeholder="Dupont"
            {...register("contactLastName", { required: "Le nom du contact est requis" })}
            error={errors.contactLastName?.message}
          />
        </div>

        <Input
          label="Email professionnel"
          type="email"
          placeholder="contact@entreprise.com"
          {...register("email", {
            required: "L'email est requis",
            pattern: {
              value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
              message: "L'adresse email n'est pas valide",
            },
          })}
          error={errors.email?.message}
        />

        <Input
          label="Mot de passe"
          type="password"
          placeholder="********"
          {...register("password", {
            required: "Le mot de passe est requis",
            minLength: {
              value: 8,
              message: "Le mot de passe doit contenir au moins 8 caracteres",
            },
          })}
          error={errors.password?.message}
        />

        <Input
          label="Confirmer le mot de passe"
          type="password"
          placeholder="********"
          {...register("confirmPassword", {
            required: "Veuillez confirmer votre mot de passe",
            validate: (value) => value === password || "Les mots de passe ne correspondent pas",
          })}
          error={errors.confirmPassword?.message}
        />

        <Input
          label="Adresse du siege"
          placeholder="123 avenue Mohammed V, Casablanca"
          {...register("address", { required: "L'adresse du siege est requise" })}
          error={errors.address?.message}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <Input
            label="Telephone"
            type="tel"
            placeholder="+212 600 000 000"
            {...register("phone")}
            error={errors.phone?.message}
          />
          <Input
            label="Domaine"
            placeholder="IT Services"
            {...register("domaine")}
            error={errors.domaine?.message}
          />
        </div>

        <div className="space-y-2">
          <label className="block text-sm font-medium text-slate-700">Logo entreprise</label>
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            onChange={(event) => setLogoFile(event.target.files?.[0] ?? null)}
            className="block w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-blue-600"
          />
        </div>

        {submitError && (
          <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
            {submitError}
          </div>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className="w-full mt-4 bg-blue-500 hover:bg-blue-600 text-white font-semibold py-3 px-4 rounded-xl shadow-sm transition-all focus:outline-none focus:ring-4 focus:ring-blue-500/30 disabled:opacity-70 flex items-center justify-center"
        >
          {isLoading ? (
            <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : (
            "S'inscrire"
          )}
        </button>
      </form>
    </div>
  );
}
