import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { ArrowLeft, User } from "lucide-react";
import { Input } from "./Input";
import { ApiError } from "../../../shared/api";
import {
  registerFreelancer,
  uploadRegistrationCv,
  uploadRegistrationProfilePicture,
} from "../../../shared/services";

interface FreelancerFormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  phone: string;
  summary: string;
}

export function FreelancerForm() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [profilePicture, setProfilePicture] = useState<File | null>(null);
  const [cvFile, setCvFile] = useState<File | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FreelancerFormData>();

  const password = watch("password");

  const onSubmit = async (data: FreelancerFormData) => {
    setIsLoading(true);
    setSubmitError("");

    try {
      const [pictureUpload, cvUpload] = await Promise.all([
        profilePicture ? uploadRegistrationProfilePicture(profilePicture) : Promise.resolve(undefined),
        cvFile ? uploadRegistrationCv(cvFile) : Promise.resolve(undefined),
      ]);

      await registerFreelancer({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
        phone: data.phone,
        summary: data.summary,
        pfpUrl: pictureUpload?.url,
        cvUrl: cvUpload?.url,
      });

      navigate("/sign-up/success/freelancer");
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
        <div className="w-10 h-10 bg-violet-100 text-violet-600 rounded-xl flex items-center justify-center">
          <User size={20} />
        </div>
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Profil Freelancer</h2>
          <p className="text-sm text-slate-500">Creez votre compte pour proposer vos services.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <Input
            label="Prenom"
            placeholder="Jean"
            {...register("firstName", { required: "Le prenom est requis" })}
            error={errors.firstName?.message}
          />
          <Input
            label="Nom"
            placeholder="Dupont"
            {...register("lastName", { required: "Le nom est requis" })}
            error={errors.lastName?.message}
          />
        </div>

        <Input
          label="Email"
          type="email"
          placeholder="jean.dupont@email.com"
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
          label="Telephone mobile"
          type="tel"
          placeholder="+33 6 12 34 56 78"
          {...register("phone", { required: "Le numero de telephone est requis" })}
          error={errors.phone?.message}
        />

        <Input
          label="Resume professionnel"
          placeholder="Developpeur React/Spring Boot avec 3 ans d'experience..."
          {...register("summary")}
          error={errors.summary?.message}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div className="space-y-2">
            <label className="block text-sm font-medium text-slate-700">Photo de profil</label>
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(event) => setProfilePicture(event.target.files?.[0] ?? null)}
              className="block w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-blue-600"
            />
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-medium text-slate-700">CV PDF</label>
            <input
              type="file"
              accept="application/pdf"
              onChange={(event) => setCvFile(event.target.files?.[0] ?? null)}
              className="block w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-blue-600"
            />
          </div>
        </div>

        {submitError && (
          <div className="rounded-xl border border-violet-200 bg-violet-50 px-4 py-3 text-sm font-medium text-violet-700">
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
