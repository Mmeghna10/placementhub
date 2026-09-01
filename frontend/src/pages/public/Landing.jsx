import { Link } from 'react-router-dom';
import { ArrowRight, Briefcase, ClipboardCheck, Users } from 'lucide-react';

export default function Landing() {
  return (
    <div className="min-h-screen bg-paper">
      <nav className="flex items-center justify-between px-8 py-6 max-w-6xl mx-auto">
        <span className="font-display text-xl font-semibold text-ink">
          PlacementHub
        </span>
        <div className="flex items-center gap-6">
          <Link to="/login" className="text-sm font-medium text-ink hover:text-teal transition">
            Sign In
          </Link>
          <Link
            to="/register"
            className="text-sm font-medium bg-ink text-paper px-4 py-2 rounded-md hover:bg-ink/90 transition"
          >
            Get Started
          </Link>
        </div>
      </nav>

      <section className="max-w-4xl mx-auto px-8 pt-20 pb-24 text-center">
        <p className="font-mono text-xs uppercase tracking-widest text-brass mb-4">
          Campus Placement, Organized
        </p>
        <h1 className="font-display text-5xl md:text-6xl font-semibold text-ink leading-tight tracking-tight">
          Every application,
          <br />
          one record.
        </h1>
        <div className="mt-6 mx-auto w-20 border-t border-ink/30" />
        <div className="mt-1 mx-auto w-20 border-t border-ink/30" />
        <p className="mt-6 text-lg text-slate max-w-xl mx-auto leading-relaxed">
          Track opportunities, applications, and outcomes in one place —
          built for students, recruiters, and placement cells alike.
        </p>
        <Link
          to="/register"
          className="mt-10 inline-flex items-center gap-2 bg-ink text-paper px-6 py-3 rounded-md font-medium hover:bg-ink/90 transition"
        >
          Create your account
          <ArrowRight size={16} />
        </Link>
      </section>

      <section className="max-w-5xl mx-auto px-8 pb-24 grid md:grid-cols-3 gap-8">
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