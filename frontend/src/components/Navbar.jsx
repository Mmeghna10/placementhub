import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogOut, Menu, X } from 'lucide-react';

const profileRouteByRole = {
  STUDENT: '/student/profile',
  RECRUITER: '/recruiter/profile',
  ADMIN: '/admin/profile',
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleLinks = {
    STUDENT: [
      { to: '/student/dashboard', label: 'Dashboard' },
      { to: '/student/jobs', label: 'Browse Jobs' },
      { to: '/student/applications', label: 'My Applications' },
    ],
    RECRUITER: [
      { to: '/recruiter/dashboard', label: 'Dashboard' },
      { to: '/recruiter/jobs', label: 'My Jobs' },
      { to: '/recruiter/company', label: 'Company' },
    ],
    ADMIN: [
      { to: '/admin/dashboard', label: 'Dashboard' },
      { to: '/admin/jobs', label: 'Jobs' },
      { to: '/admin/students', label: 'Students' },
      { to: '/admin/recruiters', label: 'Recruiters' },
      { to: '/admin/analytics', label: 'Analytics' },
    ],
  };

  const links = user ? roleLinks[user.role] || [] : [];
  const profileRoute = user ? profileRouteByRole[user.role] : '/login';

  return (
    <nav className="bg-ink border-b border-ink/10 relative">
      <div className="max-w-6xl mx-auto px-6 flex items-center justify-between h-16">
        <Link to="/" className="font-display text-lg font-semibold text-paper hover:text-brass transition">
          PlacementHub
        </Link>

        {/* Desktop links */}
        <div className="hidden md:flex items-center gap-6">
          {links.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className="text-sm text-paper/70 hover:text-paper transition"
            >
              {link.label}
            </Link>
          ))}

          <div className="h-5 border-l border-paper/20" />

          <Link
            to={profileRoute}
            className="text-xs font-mono text-paper/50 hover:text-brass transition"
            title="View profile"
          >
            {user?.email}
          </Link>

          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 text-sm text-paper/70 hover:text-brick transition"
          >
            <LogOut size={15} />
            Logout
          </button>
        </div>

        {/* Mobile toggle */}
        <button
          className="md:hidden text-paper"
          onClick={() => setOpen(!open)}
          aria-label="Toggle menu"
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {/* Mobile drawer */}
      {open && (
        <div className="md:hidden bg-ink border-t border-paper/10 px-6 py-4 space-y-3">
          {links.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              onClick={() => setOpen(false)}
              className="block text-sm text-paper/80 hover:text-paper transition"
            >
              {link.label}
            </Link>
          ))}
          <Link
            to={profileRoute}
            onClick={() => setOpen(false)}
            className="block text-xs font-mono text-paper/50 pt-2 border-t border-paper/10"
          >
            {user?.email}
          </Link>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 text-sm text-brick pt-1"
          >
            <LogOut size={15} />
            Logout
          </button>
        </div>
      )}
    </nav>
  );
}