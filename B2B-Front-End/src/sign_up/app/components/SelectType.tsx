import { useNavigate } from "react-router";
import { User, Building2, ArrowRight } from "lucide-react";

export function SelectType() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center animate-in fade-in zoom-in-95 duration-300">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Rejoignez-nous</h1>
        <p className="text-slate-600">Choisissez le type de compte qui vous correspond.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full">
        {/* Freelancer Card */}
        <button
          onClick={() => navigate("/sign-up/signup/freelancer")}
          className="group relative flex flex-col items-center p-8 bg-white rounded-2xl shadow-sm border-2 border-transparent hover:border-violet-500 hover:shadow-md transition-all text-left text-slate-800 focus:outline-none focus:ring-4 focus:ring-violet-500/20"
        >
          <div className="w-16 h-16 bg-violet-50 text-violet-500 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
            <User size={32} />
          </div>
          <h2 className="text-xl font-bold mb-2">S’inscrire en tant que Freelancer</h2>
          <p className="text-sm text-slate-500 text-center mb-6">
            Proposez vos services, trouvez des missions et développez votre activité en toute liberté.
          </p>
          <div className="mt-auto flex items-center gap-2 text-violet-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
            Sélectionner <ArrowRight size={16} />
          </div>
        </button>

        {/* Company Card */}
        <button
          onClick={() => navigate("/sign-up/signup/company")}
          className="group relative flex flex-col items-center p-8 bg-white rounded-2xl shadow-sm border-2 border-transparent hover:border-blue-500 hover:shadow-md transition-all text-left text-slate-800 focus:outline-none focus:ring-4 focus:ring-blue-500/20"
        >
          <div className="w-16 h-16 bg-blue-50 text-blue-500 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
            <Building2 size={32} />
          </div>
          <h2 className="text-xl font-bold mb-2 text-center">S’inscrire en tant qu’Entreprise</h2>
          <p className="text-sm text-slate-500 text-center mb-6">
            Trouvez les meilleurs talents pour vos projets et gérez vos équipes externes facilement.
          </p>
          <div className="mt-auto flex items-center gap-2 text-blue-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
            Sélectionner <ArrowRight size={16} />
          </div>
        </button>
      </div>
    </div>
  );
}
