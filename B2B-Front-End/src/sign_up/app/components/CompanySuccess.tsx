import { ArrowRight, Clock } from "lucide-react";
import { useEffect } from "react";
import { useNavigate } from "react-router";

export function CompanySuccess() {
  const navigate = useNavigate();

  useEffect(() => {
    const timeout = window.setTimeout(() => navigate("/sign-in"), 2600);
    return () => window.clearTimeout(timeout);
  }, [navigate]);

  return (
    <div className="bg-white p-8 md:p-10 rounded-2xl shadow-sm text-center animate-in zoom-in-95 duration-300">
      <div className="w-20 h-20 bg-violet-50 text-violet-500 rounded-full flex items-center justify-center mx-auto mb-6 relative">
        <Clock size={36} />
        <div className="absolute top-2 right-2 w-3 h-3 bg-violet-500 rounded-full animate-ping" />
        <div className="absolute top-2 right-2 w-3 h-3 bg-violet-500 rounded-full" />
      </div>

      <h2 className="text-2xl font-bold text-slate-900 mb-4">Inscription en cours de validation</h2>

      <p className="text-slate-600 mb-8 max-w-md mx-auto leading-relaxed">
        Votre compte entreprise a bien ete cree. Un administrateur doit le valider avant
        que vous puissiez utiliser l'espace entreprise.
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
