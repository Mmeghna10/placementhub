import { Link } from 'react-router-dom';
import { ArrowRight, Briefcase, ClipboardCheck, Users } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const dashboardRouteByRole = {
  STUDENT: '/student/dashboard',
  RECRUITER: '/recruiter/dashboard',
  ADMIN: '/admin/dashboard',
};

export default function Landing() {
  const { user } = useAuth();
  const dashboardRoute = user ? dashboardRouteByRole[user.role] : null;

  return (
    <div className="min-h-screen bg-paper">
      <nav className="flex items-center justify-between px-6 md:px-8 py-6 max-w-6xl mx-auto">
        <span className="font-display text-xl font-semibold text-ink">
          PlacementHub
        </span>
        <div className="flex items-center gap-3 md:gap-6">
          {user ? (
            <Link
              to={dashboardRoute}
              className="text-sm font-medium bg-ink text-paper px-4 py-2 rounded-md hover:bg-ink/90 transition"
            >
              Go to Dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium text-ink hover:text-teal transition">
                Sign In
              </Link>
              <Link
                to="/register"
                className="text-sm font-medium bg-ink text-paper px-4 py-2 rounded-md hover:bg-ink/90 transition"
              >
                Get Started
              </Link>
            </>
          )}
        </div>
      </nav>

      {/* Hero with background image */}
      <section className="relative overflow-hidden">
        <div
          className="absolute inset-0 bg-cover bg-center"
          style={{
            backgroundImage:
              "url('https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1920&q=80')",
          }}
        />
        <div className="absolute inset-0 bg-ink/80" />

        <div className="relative max-w-4xl mx-auto px-6 pt-24 pb-28 text-center">
          <p className="font-mono text-xs uppercase tracking-widest text-brass mb-4">
            Campus Placement, Organized
          </p>
          <h1 className="font-display text-4xl md:text-6xl font-semibold text-paper leading-tight tracking-tight">
            Every application,
            <br />
            one record.
          </h1>
          <div className="mt-6 mx-auto w-20 border-t border-paper/30" />
          <div className="mt-1 mx-auto w-20 border-t border-paper/30" />
          <p className="mt-6 text-lg text-paper/70 max-w-xl mx-auto leading-relaxed">
            Track opportunities, applications, and outcomes in one place —
            built for students, recruiters, and placement cells alike.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-3">
            {user ? (
              <Link
                to={dashboardRoute}
                className="inline-flex items-center gap-2 bg-brass text-ink px-6 py-3 rounded-md font-medium hover:bg-brass/90 transition w-full sm:w-auto justify-center"
              >
                Go to Dashboard
                <ArrowRight size={16} />
              </Link>
            ) : (
              <>
                <Link
                  to="/register"
                  className="inline-flex items-center gap-2 bg-brass text-ink px-6 py-3 rounded-md font-medium hover:bg-brass/90 transition w-full sm:w-auto justify-center"
                >
                  Create your account
                  <ArrowRight size={16} />
                </Link>
                <Link
                  to="/login"
                  className="inline-flex items-center gap-2 border border-paper/30 text-paper px-6 py-3 rounded-md font-medium hover:bg-paper/10 transition w-full sm:w-auto justify-center"
                >
                  Sign In
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      <section className="max-w-5xl mx-auto px-6 md:px-8 py-20 grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
        {[
          {
            icon: Users,
            title: 'For Students',
            desc: 'Browse verified openings, apply in a click, and track every application from submission to offer.',
          },
          {
            icon: Briefcase,
            title: 'For Recruiters',
            desc: 'Post roles, review applicants, and move candidates through your pipeline with a clear paper trail.',
          },
          {
            icon: ClipboardCheck,
            title: 'For Placement Cells',
            desc: 'Approve postings, monitor activity across the college, and keep every outcome on record.',
          },
        ].map(({ icon: Icon, title, desc }) => (
          <div key={title} className="bg-white border border-ink/10 rounded-lg p-6">
            <Icon size={22} className="text-teal mb-4" strokeWidth={1.75} />
            <h3 className="font-display text-lg font-semibold text-ink mb-2">
              {title}
            </h3>
            <p className="text-sm text-slate leading-relaxed">{desc}</p>
          </div>
        ))}
      </section>
    </div>
  );
}