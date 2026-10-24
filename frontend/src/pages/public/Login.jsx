import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginUser } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';
import AuthLayout, { authInputClass, authLabelClass } from '../../components/AuthLayout';

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
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to your account"
      footer={
        <>
          Don't have an account?{' '}
          <Link to="/register" className="text-teal font-medium hover:underline">
            Create one
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="login-email" className={authLabelClass}>
            Email
          </label>
          <input
            id="login-email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={authInputClass}
            placeholder="you@college.edu"
          />
        </div>

        <div>
          <label htmlFor="login-password" className={authLabelClass}>
            Password
          </label>
          <input
            id="login-password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={authInputClass}
            placeholder="••••••••"
          />
        </div>

        {error && (
          <p role="alert" className="text-sm text-brick font-medium bg-brick/5 border border-brick/15 rounded-lg px-3 py-2">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 bg-ink text-paper rounded-lg font-semibold hover:bg-ink/90 transition disabled:opacity-50 shadow-sm"
        >
          {loading ? 'Signing in…' : 'Sign In'}
        </button>
      </form>
    </AuthLayout>
  );
}
