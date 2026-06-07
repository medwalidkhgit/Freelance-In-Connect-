import { useState, type FormEvent, type ReactNode } from "react";
import { ArrowLeft, CheckCircle2, KeyRound, LayoutGrid, Mail, ShieldCheck } from "lucide-react";
import { motion } from "motion/react";
import { Link, useNavigate } from "react-router";
import authImage from "../../assets/PFS_Logo.jpg";
import { ApiError } from "../../shared/api";
import {
  requestPasswordReset,
  resetPassword,
  verifyPasswordResetOtp,
} from "../../shared/services";

type Step = "email" | "otp" | "reset" | "done";

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const submitEmail = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await run(async () => {
      const response = await requestPasswordReset({ email });
      setMessage(response || "Code OTP envoye.");
      setStep("otp");
    });
  };

  const submitOtp = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await run(async () => {
      const response = await verifyPasswordResetOtp({ email, otp });
      setMessage(response || "Code OTP valide.");
      setStep("reset");
    });
  };

  const submitReset = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await run(async () => {
      const response = await resetPassword({ email, otp, newPassword, confirmPassword });
      setMessage(response || "Mot de passe reinitialise.");
      setStep("done");
    });
  };

  const run = async (task: () => Promise<void>) => {
    setError("");
    setMessage("");
    setIsLoading(true);
    try {
      await task();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Operation impossible pour le moment.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen w-full flex items-center justify-center p-4 sm:p-8 relative selection:bg-blue-200 selection:text-blue-900"
      style={{ backgroundColor: "#CFEFF3" }}
    >
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute -top-[20%] -left-[10%] w-[50%] h-[50%] rounded-full bg-blue-400/20 blur-[120px]" />
        <div className="absolute bottom-[10%] -right-[10%] w-[50%] h-[50%] rounded-full bg-purple-400/20 blur-[120px]" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
        className="w-full max-w-[1200px] min-h-[600px] flex flex-col md:flex-row bg-white/95 backdrop-blur-2xl border border-white/60 shadow-[0_8px_40px_-12px_rgba(0,0,0,0.1)] rounded-[2.5rem] overflow-hidden relative z-10"
      >
        <div className="w-full md:w-[430px] lg:w-[470px] shrink-0 p-8 sm:p-12 flex flex-col justify-center bg-white">
          <Link
            to="/sign-in"
            className="mb-8 inline-flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-[#3B82F6]"
          >
            <ArrowLeft className="h-4 w-4" />
            Retour a la connexion
          </Link>

          <div className="flex items-center gap-3 mb-10">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#3B82F6] to-[#8B5CF6] flex items-center justify-center shadow-md">
              <LayoutGrid className="w-5 h-5 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900 tracking-tight">FIC</span>
          </div>

          {step !== "done" ? (
            <>
              <div className="mb-8">
                <h1 className="text-2xl font-bold text-gray-900">Mot de passe oublie</h1>
                <p className="mt-2 text-sm leading-6 text-gray-500">
                  Saisissez votre email, validez le code OTP recu, puis choisissez un nouveau mot de passe.
                </p>
              </div>

              {step === "email" && (
                <form onSubmit={submitEmail} className="space-y-6">
                  <FieldIcon icon={<Mail className="h-5 w-5 text-gray-400" />}>
                    <input
                      type="email"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6]"
                      placeholder="name@company.com"
                      required
                    />
                  </FieldIcon>
                  <PrimaryButton loading={isLoading} label="Envoyer le code OTP" loadingLabel="Envoi..." />
                </form>
              )}

              {step === "otp" && (
                <form onSubmit={submitOtp} className="space-y-6">
                  <FieldIcon icon={<ShieldCheck className="h-5 w-5 text-gray-400" />}>
                    <input
                      type="text"
                      inputMode="numeric"
                      value={otp}
                      onChange={(event) => setOtp(event.target.value)}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6]"
                      placeholder="Code OTP"
                      required
                    />
                  </FieldIcon>
                  <PrimaryButton loading={isLoading} label="Verifier le code" loadingLabel="Verification..." />
                </form>
              )}

              {step === "reset" && (
                <form onSubmit={submitReset} className="space-y-6">
                  <FieldIcon icon={<KeyRound className="h-5 w-5 text-gray-400" />}>
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6]"
                      placeholder="Nouveau mot de passe"
                      minLength={8}
                      required
                    />
                  </FieldIcon>
                  <FieldIcon icon={<KeyRound className="h-5 w-5 text-gray-400" />}>
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6]"
                      placeholder="Confirmation du mot de passe"
                      minLength={8}
                      required
                    />
                  </FieldIcon>
                  <PrimaryButton loading={isLoading} label="Reinitialiser" loadingLabel="Traitement..." />
                </form>
              )}
            </>
          ) : (
            <div className="rounded-2xl border border-green-100 bg-green-50 p-6 text-center">
              <CheckCircle2 className="mx-auto mb-3 h-10 w-10 text-green-600" />
              <h1 className="text-xl font-bold text-gray-900">Mot de passe reinitialise</h1>
              <p className="mt-2 text-sm text-gray-600">Vous pouvez maintenant vous connecter.</p>
              <button
                type="button"
                onClick={() => navigate("/sign-in")}
                className="mt-5 w-full rounded-xl py-3 text-sm font-semibold text-white"
                style={{ background: "linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%)" }}
              >
                Aller a la connexion
              </button>
            </div>
          )}

          {message && (
            <div className="mt-6 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
              {message}
            </div>
          )}
          {error && (
            <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
              {error}
            </div>
          )}
        </div>

        <div className="hidden md:flex flex-1 items-center justify-center bg-white p-12 lg:p-20 border-l border-gray-100">
          <div className="w-full max-w-[400px]">
            <img src={authImage} alt="FIC Logo" className="w-full h-auto object-contain mix-blend-multiply" />
          </div>
        </div>
      </motion.div>
    </div>
  );
}

function FieldIcon({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <div className="relative">
      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">{icon}</div>
      {children}
    </div>
  );
}

function PrimaryButton({ loading, label, loadingLabel }: { loading: boolean; label: string; loadingLabel: string }) {
  return (
    <button
      type="submit"
      disabled={loading}
      className="w-full py-4 rounded-xl text-white font-semibold text-sm transition-all duration-300 hover:shadow-lg hover:shadow-[#3B82F6]/30 active:scale-[0.98] disabled:opacity-70"
      style={{ background: "linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%)" }}
    >
      {loading ? loadingLabel : label}
    </button>
  );
}
