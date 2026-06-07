import { ArrowRight } from "lucide-react";
import { motion } from "motion/react";
import { Link } from "react-router";
import logoImage from "../assets/PFS_Logo.jpg";

export default function Landing() {
  return (
    <main
      className="relative flex min-h-screen w-full items-center justify-center overflow-hidden px-6 py-10 text-slate-950"
      style={{ backgroundColor: "#CFEFF3" }}
    >
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-[-12%] top-[-20%] h-[52rem] w-[52rem] rounded-full bg-sky-300/30 blur-[140px]" />
        <div className="absolute bottom-[-22%] right-[-12%] h-[48rem] w-[48rem] rounded-full bg-indigo-300/30 blur-[140px]" />
      </div>

      <motion.section
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 flex w-full max-w-[760px] flex-col items-center rounded-[2.5rem] border border-white/60 bg-white/95 px-8 pb-14 pt-10 text-center shadow-[0_8px_40px_-12px_rgba(0,0,0,0.1)] backdrop-blur-2xl sm:px-14 sm:pb-16 sm:pt-12"
      >
        <div className="mb-6 flex h-40 w-40 items-center justify-center sm:h-52 sm:w-52">
          <img src={logoImage} alt="FIC" className="max-h-full max-w-full object-contain mix-blend-multiply" />
        </div>

        <p className="mb-4 text-sm font-bold uppercase tracking-[0.32em] text-blue-700">
          Freelance In Connect
        </p>
        <h1 className="max-w-3xl text-4xl font-black leading-tight text-slate-950 sm:text-6xl">
          Bienvenue a FIC
        </h1>
        <p className="mt-6 max-w-3xl text-lg font-medium leading-8 text-slate-700 sm:text-xl">
          Votre plateforme B2B de sous-traitance des projets informatique.
        </p>

        <Link
          to="/sign-in"
          className="group relative mt-8 inline-flex items-center justify-center gap-3 overflow-hidden rounded-xl px-10 py-4 text-base font-bold text-white shadow-[0_18px_40px_-20px_rgba(59,130,246,0.8)] transition-all duration-300 hover:shadow-lg hover:shadow-[#3B82F6]/30 active:scale-[0.98] focus:outline-none focus:ring-4 focus:ring-blue-300"
          style={{ background: "linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%)", minWidth: "220px" }}
        >
          <span className="relative z-10 inline-flex items-center gap-3">
            Entrer
            <ArrowRight className="h-5 w-5" />
          </span>
          <span className="absolute inset-0 bg-white/20 opacity-0 transition-opacity group-hover:opacity-100" />
        </Link>
      </motion.section>
    </main>
  );
}
