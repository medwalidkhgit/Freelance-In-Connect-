import { ArrowRight, CheckCircle2 } from "lucide-react";
import { useEffect } from "react";
import { useNavigate } from "react-router";

export function FreelancerSuccess() {
  const navigate = useNavigate();

  useEffect(() => {
    const timeout = window.setTimeout(() => navigate("/sign-in"), 2200);
    return () => window.clearTimeout(timeout);
  }, [navigate]);

  return (
    <div className="bg-white p-8 md:p-10 rounded-2xl shadow-sm text-center animate-in zoom-in-95 duration-300">
      <div className="w-20 h-20 bg-green-50 text-green-500 rounded-full flex items-center justify-center mx-auto mb-6">
        <CheckCircle2 size={40} />
      </div>

      <h2 className="text-2xl font-bold text-slate-900 mb-4">Inscription reussie</h2>

      <p className="text-slate-600 mb-8 max-w-md mx-auto leading-relaxed">
        Votre compte freelancer a bien ete cree. Connectez-vous pour completer votre profil,
        ajouter votre photo et televerser votre CV.
      </p>

      <p className="text-sm font-medium text-slate-500 mb-5">
        Redirection vers la connexion...
      </p>

      <button
        onClick={() => navigate("/sign-in")}
        className="inline-flex items-center gap-2 px-6 py-3 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded-xl transition-colors focus:outline-none focus:ring-4 focus:ring-blue-500/30"
      >
        Se connecter
        <ArrowRight size={18} />
      </button>
    </div>
  );
}
