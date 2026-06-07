import { useState, type FormEvent } from 'react';
import { Mail, KeyRound, LayoutGrid } from 'lucide-react';
import { motion } from 'motion/react';
import { Link, useNavigate } from "react-router";
import authImage from '../../assets/PFS_Logo.jpg';
import { ApiError } from "../../shared/api";
import { getDefaultPathForCurrentUser, saveTokens } from "../../shared/auth";
import { login, resendLoginOtp, verifyLoginOtp } from "../../shared/services";

export default function App() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [challengeId, setChallengeId] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const isOtpStep = Boolean(challengeId);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      if (isOtpStep) {
        const tokens = await verifyLoginOtp({ challengeId, otp });
        saveTokens(tokens);
        navigate(getDefaultPathForCurrentUser());
        return;
      }

      const response = await login({ email, password });
      if ("otpRequired" in response && response.otpRequired) {
        setChallengeId(response.challengeId);
        setInfoMessage(response.message || "Code OTP envoye.");
        setOtp('');
        return;
      }

      saveTokens(response);
      navigate(getDefaultPathForCurrentUser());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Connexion impossible pour le moment.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!challengeId) return;
    setError('');
    setInfoMessage('');
    setIsLoading(true);

    try {
      const message = await resendLoginOtp({ challengeId });
      setInfoMessage(message || "Nouveau code OTP envoye.");
      setOtp('');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Renvoi du code impossible pour le moment.");
    } finally {
      setIsLoading(false);
    }
  };

  const resetLoginStep = () => {
    setChallengeId('');
    setOtp('');
    setInfoMessage('');
    setError('');
  };

  return (
    <div
      className="min-h-screen w-full flex items-center justify-center p-4 sm:p-8 relative selection:bg-blue-200 selection:text-blue-900"
      style={{ backgroundColor: '#CFEFF3' }}
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
        <div className="w-full md:w-[400px] lg:w-[450px] shrink-0 p-8 sm:p-12 flex flex-col justify-center relative bg-white">
          <div className="flex items-center gap-3 mb-10">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#3B82F6] to-[#8B5CF6] flex items-center justify-center shadow-md">
              <LayoutGrid className="w-5 h-5 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900 tracking-tight">FIC</span>
          </div>

          <div className="mb-10">
            <p className="text-gray-500 text-sm leading-6">
              {isOtpStep
                ? "Veuillez saisir le code OTP envoye a votre adresse email."
                : "Veuillez entrer vos informations afin de vous connecter a votre portail."}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {!isOtpStep ? (
              <>
                <div className="space-y-2">
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                    Email
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Mail className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6] focus:bg-white transition-all duration-200"
                      placeholder="name@company.com"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                      Mot de passe
                    </label>
                    <Link
                      to="/forgot-password"
                      className="text-sm font-medium text-[#3B82F6] hover:text-[#8B5CF6] transition-colors"
                    >
                      Mot de passe oublie ?
                    </Link>
                  </div>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <KeyRound className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      id="password"
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6] focus:bg-white transition-all duration-200"
                      placeholder="********"
                      required
                    />
                  </div>
                </div>
              </>
            ) : (
              <div className="space-y-5">
                <div className="rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
                  {infoMessage || `Code OTP envoye a ${email}.`}
                </div>
                <div className="space-y-2">
                  <label htmlFor="otp" className="block text-sm font-medium text-gray-700">
                    Code OTP
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <KeyRound className="h-5 w-5 text-gray-400" />
                    </div>
                    <input
                      id="otp"
                      type="text"
                      inputMode="numeric"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      className="block w-full pl-11 pr-4 py-3.5 bg-gray-50/50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[#3B82F6]/50 focus:border-[#3B82F6] focus:bg-white transition-all duration-200"
                      placeholder="123456"
                      minLength={6}
                      maxLength={6}
                      required
                    />
                  </div>
                </div>
                <div className="flex items-center justify-between gap-3 text-sm">
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={isLoading}
                    className="font-medium text-[#3B82F6] hover:text-[#8B5CF6] disabled:opacity-60"
                  >
                    Generer un nouveau code
                  </button>
                  <button
                    type="button"
                    onClick={resetLoginStep}
                    disabled={isLoading}
                    className="font-medium text-gray-500 hover:text-gray-800 disabled:opacity-60"
                  >
                    Modifier l'email
                  </button>
                </div>
              </div>
            )}

            {error && (
              <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-4 mt-2 rounded-xl text-white font-semibold text-sm transition-all duration-300 hover:shadow-lg hover:shadow-[#3B82F6]/30 active:scale-[0.98] relative overflow-hidden group"
              style={{ background: 'linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%)' }}
            >
              <span className="relative z-10">
                {isLoading ? "Verification..." : isOtpStep ? "Valider le code" : "Se connecter"}
              </span>
              <div className="absolute inset-0 bg-white/20 opacity-0 group-hover:opacity-100 transition-opacity" />
            </button>

            {!isOtpStep && (
              <div className="text-center text-sm text-gray-500">
                Pas encore de compte ?{" "}
                <Link to="/sign-up" className="font-semibold text-[#3B82F6] hover:text-[#8B5CF6]">
                  Creer un compte
                </Link>
              </div>
            )}
          </form>
        </div>

        <div className="hidden md:flex flex-1 items-center justify-center bg-white p-12 lg:p-20 border-l border-gray-100">
          <div className="w-full max-w-[400px] transition-transform duration-700 hover:scale-105">
            <img
              src={authImage}
              alt="FIC Logo"
              className="w-full h-auto object-contain mix-blend-multiply"
            />
          </div>
        </div>
      </motion.div>
    </div>
  );
}
