import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogOut } from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleLinks = {
    STUDENT: [
      { to: '/student/dashboard', label: 'Dashboard' },
      { to: '/student/jobs', label: 'Browse Jobs' },
      { to: '/student/applications', label: 'My Applications' },
      { to: '/student/profile', label: 'Profile' },
    ],
    RECRUITER: [
      { to: '/recruiter/dashboard', label: 'Dashboard' },
      { to: '/recruiter/jobs', label: 'My Jobs' },
      { to: '/recruiter/profile', label: 'Company' },
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

  return (
    <nav className="bg-ink border-b border-ink/10">
      <div className="max-w-6xl mx-auto px-6 flex items-center justify-between h-16">
        <span className="font-display text-lg font-semibold text-paper">
          PlacementHub
        </span>

        <div className="flex items-center gap-6">
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

          <span className="text-xs font-mono text-paper/50">
            {user?.email}
          </span>

          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 text-sm text-paper/70 hover:text-brick transition"
          >
            <LogOut size={15} />
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
}