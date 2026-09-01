import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginUser } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await loginUser({ email, password });
      const { token, role } = res.data;
      login(token, email, role);

      if (role === 'STUDENT') navigate('/student/dashboard');
      else if (role === 'RECRUITER') navigate('/recruiter/dashboard');
      else if (role === 'ADMIN') navigate('/admin/dashboard');
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-paper flex items-center justify-center px-6">
      <div className="w-full max-w-sm">
        <div className="mb-10 text-center">
          <h1 className="font-display text-3xl font-semibold text-ink tracking-tight">
            PlacementHub
          </h1>
          <div className="mt-3 mx-auto w-16 border-t border-ink/30" />
          <div className="mt-1 mx-auto w-16 border-t border-ink/30" />
          <p className="mt-4 text-sm text-slate font-body">
            Sign in to your account
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
              Email
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-white border border-ink/15 rounded-md text-ink placeholder:text-slate/50 focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
              placeholder="you@college.edu"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
              Password
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-white border border-ink/15 rounded-md text-ink placeholder:text-slate/50 focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
              placeholder="••••••••"
            />
          </div>

          {error && (
            <p className="text-sm text-brick font-medium">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-ink text-paper rounded-md font-medium hover:bg-ink/90 transition disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate">
          Don't have an account?{' '}
          <Link to="/register" className="text-teal font-medium hover:underline">
            Create one
          </Link>
        </p>
      </div>
    </div>
  );
}