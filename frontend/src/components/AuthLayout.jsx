import { Link } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import Logo from './Logo';

const highlights = [
  'Jobs matched to your CGPA and branch',
  'One timeline from application to offer',
  'Interview slots you can book yourself',
];

/** Split screen for login and registration: brand panel on the left, form card on the right. */
export default function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="min-h-screen grid lg:grid-cols-[1.05fr_1fr] bg-paper">
      <aside className="relative hidden lg:flex flex-col justify-between overflow-hidden bg-ink p-12 text-paper">
        <div className="absolute inset-0 bg-grid" aria-hidden="true" />
        <div
          className="absolute inset-0 bg-[radial-gradient(ellipse_70%_50%_at_20%_10%,rgb(20_85_90/0.55),transparent),radial-gradient(ellipse_60%_50%_at_90%_100%,rgb(176_141_87/0.2),transparent)]"
          aria-hidden="true"
        />
        <Link to="/" className="relative w-fit hover:opacity-90 transition">
          <Logo tone="light" />
        </Link>

        <div className="relative max-w-md">
          <p className="font-display text-4xl font-semibold leading-tight tracking-tight">
            Every application, <span className="text-brass">one record.</span>
          </p>
          <ul className="mt-8 space-y-3">
            {highlights.map((h) => (
              <li key={h} className="flex items-center gap-3 text-paper/75">
                <CheckCircle2 size={18} className="text-brass shrink-0" /> {h}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-paper/40">For students, recruiters and placement cells.</p>
      </aside>

      <main className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md animate-fade-up">
          <Link to="/" className="lg:hidden mb-8 flex justify-center">
            <Logo />
          </Link>
          <div className="bg-white border border-ink/[0.08] rounded-2xl shadow-card p-8 sm:p-10">
            <h1 className="font-display text-2xl font-semibold text-ink tracking-tight">{title}</h1>
            <p className="mt-1.5 text-sm text-slate">{subtitle}</p>
            <div className="mt-8">{children}</div>
          </div>
          {footer && <p className="mt-6 text-center text-sm text-slate">{footer}</p>}
        </div>
      </main>
    </div>
  );
}

export const authInputClass =
  'w-full px-3.5 py-2.5 bg-paper/60 border border-ink/15 rounded-lg text-ink placeholder:text-slate/50 focus:outline-none focus:bg-white focus:ring-2 focus:ring-teal/30 focus:border-teal transition';

export const authLabelClass = 'block text-sm font-medium text-ink mb-1.5';
