import { Outlet } from "react-router";
import logo from "../../../assets/PFS_Logo.jpg";
import { ImageWithFallback } from "./figma/ImageWithFallback";
import "../../styles/index.css";

export function Layout() {
  return (
    <div className="min-h-screen bg-[#CFEFF3] text-slate-800 font-sans flex flex-col items-center justify-center p-4 sm:p-6 md:p-8">
      <div className="w-full max-w-4xl flex justify-center mb-8">
        <div className="flex items-center gap-3 text-2xl font-bold text-slate-900 bg-white/50 backdrop-blur-sm px-6 py-3 rounded-2xl shadow-sm border border-white/60">
          <div className="w-10 h-10 rounded-xl overflow-hidden shadow-sm flex-shrink-0 bg-white">
            <ImageWithFallback 
              src={logo} 
              alt="Freelancer In Connect Logo" 
              className="w-full h-full object-cover"
            />
          </div>
          <span className="bg-gradient-to-r from-blue-600 to-violet-600 bg-clip-text text-transparent">
            Freelancer In Connect
          </span>
        </div>
      </div>
      <main className="w-full max-w-2xl relative z-10">
        <Outlet />
      </main>
    </div>
  );
}
