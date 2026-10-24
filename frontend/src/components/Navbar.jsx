import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogOut, Menu, X } from 'lucide-react';
import Logo from './Logo';

const profileRouteByRole = {
  STUDENT: '/student/profile',
  RECRUITER: '/recruiter/profile',
  ADMIN: '/admin/profile',
};

const roleLabels = { STUDENT: 'Student', RECRUITER: 'Recruiter', ADMIN: 'Admin' };

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

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const links = user ? roleLinks[user.role] || [] : [];
  const profileRoute = user ? profileRouteByRole[user.role] : '/login';
  const initial = user?.email?.[0]?.toUpperCase() ?? '?';

  const desktopLinkClass = ({ isActive }) =>
    `px-3 py-1.5 rounded-lg text-sm font-medium transition ${
      isActive ? 'bg-paper/10 text-paper' : 'text-paper/65 hover:text-paper hover:bg-paper/5'
    }`;

  return (
    <nav className="sticky top-0 z-30 bg-ink border-b border-paper/10 shadow-[0_1px_0_rgb(0_0_0/0.2)]">
      <div className="max-w-6xl mx-auto px-6 flex items-center justify-between h-16">
        <Link to="/" aria-label="PlacementHub home" className="hover:opacity-90 transition">
          <Logo tone="light" />
        </Link>

        {/* Desktop links */}
        <div className="hidden md:flex items-center gap-1">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} className={desktopLinkClass}>
              {link.label}
            </NavLink>
          ))}

          <div className="h-6 border-l border-paper/15 mx-3" />

          <Link
            to={profileRoute}
            className="flex items-center gap-2.5 rounded-lg pl-1 pr-2 py-1 hover:bg-paper/5 transition"
            title="View profile"
          >
            <span className="h-8 w-8 rounded-full bg-brass text-ink text-sm font-semibold flex items-center justify-center">
              {initial}
            </span>
            <span className="text-left leading-tight">
              <span className="block text-xs font-mono text-paper/80 max-w-[11rem] truncate">{user?.email}</span>
              <span className="block text-[11px] text-paper/45">{roleLabels[user?.role]}</span>
            </span>
          </Link>

          <button
            onClick={handleLogout}
            className="ml-1 flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-paper/65 hover:text-paper hover:bg-brick/30 transition"
          >
            <LogOut size={15} />
            Logout
          </button>
        </div>

        {/* Mobile toggle */}
        <button
          className="md:hidden text-paper p-1 rounded-lg hover:bg-paper/10"
          onClick={() => setOpen(!open)}
          aria-label="Toggle menu"
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {/* Mobile drawer */}
      {open && (
        <div className="md:hidden bg-ink border-t border-paper/10 px-4 py-3 space-y-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-lg text-sm ${isActive ? 'bg-paper/10 text-paper' : 'text-paper/75 hover:text-paper'}`
              }
            >
              {link.label}
            </NavLink>
          ))}
          <Link
            to={profileRoute}
            onClick={() => setOpen(false)}
            className="block px-3 pt-3 mt-2 text-xs font-mono text-paper/55 border-t border-paper/10"
          >
            {user?.email}
          </Link>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 px-3 py-2 text-sm text-brick"
          >
            <LogOut size={15} />
            Logout
          </button>
        </div>
      )}
    </nav>
  );
}
