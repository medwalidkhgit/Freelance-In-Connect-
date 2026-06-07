import { forwardRef } from "react";
import { AlertCircle } from "lucide-react";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className = "", ...props }, ref) => {
    return (
      <div className="flex flex-col gap-1.5 w-full">
        <label className="text-sm font-medium text-slate-700">
          {label}
        </label>
        <div className="relative">
          <input
            ref={ref}
            className={`w-full px-4 py-2.5 rounded-xl border ${
              error 
                ? "border-violet-500 focus:border-violet-500 focus:ring-violet-500/20" 
                : "border-slate-200 focus:border-blue-500 focus:ring-blue-500/20"
            } bg-slate-50 focus:bg-white outline-none focus:ring-4 transition-all text-slate-800 ${className}`}
            {...props}
          />
          {error && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2 text-violet-500">
              <AlertCircle size={18} />
            </div>
          )}
        </div>
        {error && (
          <p className="text-sm text-violet-500 mt-0.5 animate-in slide-in-from-top-1">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";
