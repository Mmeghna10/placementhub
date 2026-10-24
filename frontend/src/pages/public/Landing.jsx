import { Link } from 'react-router-dom';
import {
  ArrowRight, Briefcase, CalendarCheck, CheckCircle2, ClipboardCheck, LineChart, ShieldCheck, Sparkles, Users,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Logo from '../../components/Logo';
import StatusBadge from '../../components/StatusBadge';

const dashboardRouteByRole = {
  STUDENT: '/student/dashboard',
  RECRUITER: '/recruiter/dashboard',
  ADMIN: '/admin/dashboard',
};

const audiences = [
  {
    icon: Users,
    title: 'For Students',
    desc: 'Browse verified openings, see only the roles you are eligible for, apply in a click and follow every application from submission to offer.',
    points: ['Eligibility-matched jobs', 'Live status timeline', 'Self-service interview booking'],
  },
  {
    icon: Briefcase,
    title: 'For Recruiters',
    desc: 'Post roles, review complete candidate profiles and move applicants through a clear pipeline that never loses an update.',
    points: ['Applicant profiles in one view', 'Guided status changes', 'Interview slots with capacity'],
  },
  {
    icon: ClipboardCheck,
    title: 'For Placement Cells',
    desc: 'Approve postings before students see them, monitor activity across the campus and keep every outcome on record.',
    points: ['Job approval workflow', 'Campus-wide dashboard', 'Placement analytics'],
  },
];

const steps = [
  { icon: Briefcase, title: 'Recruiters post roles', desc: 'With CGPA, branch and deadline rules.' },
  { icon: ShieldCheck, title: 'The placement cell approves', desc: 'Only verified jobs reach students.' },
  { icon: Sparkles, title: 'Students apply', desc: 'Matched to roles they qualify for.' },
  { icon: CalendarCheck, title: 'Interviews and offers', desc: 'Every step tracked on one timeline.' },
];

function PreviewCard() {
  return (
    <div className="relative">
      <div className="absolute -inset-6 rounded-3xl bg-brass/20 blur-3xl" aria-hidden="true" />
      <div className="relative bg-white rounded-2xl shadow-2xl ring-1 ring-ink/5 p-6 text-left w-full max-w-sm mx-auto">
        <div className="flex items-center gap-3">
          <div className="h-11 w-11 rounded-lg bg-teal/10 text-teal font-semibold flex items-center justify-center">NL</div>
          <div className="min-w-0">
            <p className="font-display font-semibold text-ink">Software Engineer I</p>
            <p className="text-xs text-slate">Northwind Labs · Bengaluru</p>
          </div>
        </div>
        <div className="mt-4 flex flex-wrap gap-2 text-xs">
          <span className="px-2 py-1 rounded-md bg-paper text-slate">Full time</span>
          <span className="px-2 py-1 rounded-md bg-paper text-slate">₹12–16 LPA</span>
          <span className="px-2 py-1 rounded-md bg-emerald-50 text-emerald-700">You are eligible</span>
        </div>
        <div className="mt-5 border-t border-ink/10 pt-4 space-y-3">
          {[
            ['Applied', 'Oct 12', true],
            ['Shortlisted', 'Oct 15', true],
            ['Interview', 'Oct 21 · 10:00', true],
            ['Offer', 'Pending', false],
          ].map(([label, when, done]) => (
            <div key={label} className="flex items-center gap-3">
              <CheckCircle2 size={17} className={done ? 'text-teal' : 'text-ink/20'} />
              <span className={`text-sm flex-1 ${done ? 'text-ink' : 'text-slate/70'}`}>{label}</span>
              <span className="text-xs font-mono text-slate">{when}</span>
            </div>
          ))}
        </div>
        <div className="mt-5 flex items-center justify-between">
          <StatusBadge status="INTERVIEW" />
          <span className="text-xs text-slate">Updated 2h ago</span>
        </div>
      </div>
    </div>
  );
}

export default function Landing() {
  const { user } = useAuth();
  const dashboardRoute = user ? dashboardRouteByRole[user.role] : null;

  const primaryCta = user ? (
    <Link
      to={dashboardRoute}
      className="inline-flex items-center justify-center gap-2 bg-brass text-ink px-6 py-3 rounded-lg font-semibold hover:bg-[#c29f68] transition shadow-lg shadow-brass/20"
    >
      Go to Dashboard
      <ArrowRight size={16} />
    </Link>
  ) : (
    <Link
      to="/register"
      className="inline-flex items-center justify-center gap-2 bg-brass text-ink px-6 py-3 rounded-lg font-semibold hover:bg-[#c29f68] transition shadow-lg shadow-brass/20"
    >
      Create your account
      <ArrowRight size={16} />
    </Link>
  );

  return (
    <div className="min-h-screen bg-paper">
      {/* Hero */}
      <header className="relative overflow-hidden bg-ink">
        <div className="absolute inset-0 bg-grid" aria-hidden="true" />
        <div
          className="absolute inset-0 bg-[radial-gradient(ellipse_60%_60%_at_80%_20%,rgb(20_85_90/0.55),transparent),radial-gradient(ellipse_50%_50%_at_10%_90%,rgb(176_141_87/0.18),transparent)]"
          aria-hidden="true"
        />

        <nav className="relative flex items-center justify-between px-6 py-5 max-w-6xl mx-auto">
          <Logo tone="light" />
          <div className="flex items-center gap-2 md:gap-3">
            {user ? (
              <Link
                to={dashboardRoute}
                className="whitespace-nowrap text-sm font-medium bg-paper text-ink px-3 sm:px-4 py-2 rounded-lg hover:bg-white transition"
              >
                Go to Dashboard
              </Link>
            ) : (
              <>
                <Link to="/login" className="whitespace-nowrap text-sm font-medium text-paper/80 hover:text-paper px-2 sm:px-3 py-2 transition">
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="whitespace-nowrap text-sm font-medium bg-paper text-ink px-3 sm:px-4 py-2 rounded-lg hover:bg-white transition"
                >
                  Get Started
                </Link>
              </>
            )}
          </div>
        </nav>

        <div className="relative max-w-6xl mx-auto px-6 pt-12 pb-24 md:pt-20 md:pb-28 grid md:grid-cols-2 gap-14 items-center">
          <div className="text-center md:text-left animate-fade-up">
            <p className="inline-flex items-center gap-2 rounded-full border border-paper/15 bg-paper/5 px-3 py-1 text-xs font-medium text-brass">
              <Sparkles size={13} /> Campus placement, organized
            </p>
            <h1 className="mt-6 font-display text-4xl md:text-[3.5rem] font-semibold text-paper leading-[1.08] tracking-tight">
              Every application,
              <br />
              <span className="text-brass">one record.</span>
            </h1>
            <p className="mt-6 text-lg text-paper/70 max-w-lg mx-auto md:mx-0 leading-relaxed">
              Track opportunities, applications and outcomes in one place, built for students,
              recruiters and placement cells alike.
            </p>
            <div className="mt-9 flex flex-col sm:flex-row gap-3 justify-center md:justify-start">
              {primaryCta}
              {!user && (
                <Link
                  to="/login"
                  className="inline-flex items-center justify-center gap-2 border border-paper/25 text-paper px-6 py-3 rounded-lg font-medium hover:bg-paper/10 transition"
                >
                  Sign In
                </Link>
              )}
            </div>
            <div className="mt-10 flex flex-wrap gap-x-6 gap-y-2 justify-center md:justify-start text-sm text-paper/55">
              {['Verified job postings', 'Server-checked eligibility', 'No double applications'].map((t) => (
                <span key={t} className="inline-flex items-center gap-1.5">
                  <CheckCircle2 size={15} className="text-brass" /> {t}
                </span>
              ))}
            </div>
          </div>
          <div className="hidden md:block animate-fade-up [animation-delay:120ms]">
            <PreviewCard />
          </div>
        </div>
      </header>

      {/* Audiences */}
      <section className="max-w-6xl mx-auto px-6 py-20">
        <div className="text-center max-w-2xl mx-auto">
          <p className="text-xs font-semibold uppercase tracking-widest text-teal">One platform, three roles</p>
          <h2 className="mt-3 font-display text-3xl font-semibold text-ink tracking-tight text-balance">
            Everyone in the placement season, on the same page
          </h2>
        </div>
        <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6">
          {audiences.map(({ icon: Icon, title, desc, points }) => (
            <div
              key={title}
              className="group bg-white border border-ink/[0.08] rounded-2xl p-7 shadow-card hover:shadow-card-hover hover:-translate-y-0.5 transition"
            >
              <div className="h-11 w-11 rounded-xl bg-teal/10 text-teal flex items-center justify-center group-hover:bg-teal group-hover:text-paper transition">
                <Icon size={21} strokeWidth={1.8} />
              </div>
              <h3 className="mt-5 font-display text-xl font-semibold text-ink">{title}</h3>
              <p className="mt-2 text-sm text-slate leading-relaxed">{desc}</p>
              <ul className="mt-5 space-y-2">
                {points.map((p) => (
                  <li key={p} className="flex items-center gap-2 text-sm text-ink/80">
                    <CheckCircle2 size={15} className="text-teal shrink-0" /> {p}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="bg-white border-y border-ink/[0.06]">
        <div className="max-w-6xl mx-auto px-6 py-20">
          <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-teal">How it works</p>
              <h2 className="mt-3 font-display text-3xl font-semibold text-ink tracking-tight text-balance">From posting to offer in four steps</h2>
            </div>
            <p className="text-sm text-slate max-w-sm">
              Each step is checked on the server, so deadlines, eligibility and interview capacity hold even on the busiest day.
            </p>
          </div>
          <ol className="mt-12 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {steps.map(({ icon: Icon, title, desc }, i) => (
              <li key={title} className="relative rounded-2xl border border-ink/[0.08] bg-paper/60 p-6">
                <span className="absolute top-5 right-5 font-mono text-xs text-slate/60">0{i + 1}</span>
                <Icon size={22} className="text-brass" strokeWidth={1.8} />
                <p className="mt-4 font-semibold text-ink">{title}</p>
                <p className="mt-1 text-sm text-slate">{desc}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* Call to action */}
      <section className="max-w-6xl mx-auto px-6 py-20">
        <div className="relative overflow-hidden rounded-3xl bg-ink px-8 py-14 md:px-14 text-center md:text-left md:flex md:items-center md:justify-between gap-8">
          <div className="absolute inset-0 bg-grid" aria-hidden="true" />
          <div className="relative">
            <LineChart className="text-brass mx-auto md:mx-0" size={26} strokeWidth={1.8} />
            <h2 className="mt-4 font-display text-3xl font-semibold text-paper tracking-tight">Ready for placement season?</h2>
            <p className="mt-2 text-paper/65">Create a free student or recruiter account in under a minute.</p>
          </div>
          <div className="relative mt-8 md:mt-0 shrink-0">{primaryCta}</div>
        </div>
      </section>

      <footer className="border-t border-ink/[0.08]">
        <div className="max-w-6xl mx-auto px-6 py-8 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-slate">
          <Logo />
          <p>Built for students, recruiters and placement cells.</p>
        </div>
      </footer>
    </div>
  );
}
