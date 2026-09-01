import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

export default function Register() {
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'STUDENT',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await registerUser(form);
      const { token, role } = res.data;
      login(token, form.email, role);

      if (role === 'STUDENT') navigate('/student/dashboard');
      else if (role === 'RECRUITER') navigate('/recruiter/dashboard');
    } catch (err) {
      const errData = err.response?.data;
      const message = typeof errData === 'object' && errData !== null
        ? Object.values(errData)[0]
        : 'Registration failed';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-paper flex items-center justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-10 text-center">
          <h1 className="font-display text-3xl font-semibold text-ink tracking-tight">
            PlacementHub
          </h1>
          <div className="mt-3 mx-auto w-16 border-t border-ink/30" />
          <div className="mt-1 mx-auto w-16 border-t border-ink/30" />
          <p className="mt-4 text-sm text-slate font-body">
            Create your account
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
              I am a
            </label>
            <div className="grid grid-cols-2 gap-2">
              {['STUDENT', 'RECRUITER'].map((r) => (
                <button
                  key={r}
                  type="button"
                  onClick={() => setForm({ ...form, role: r })}
                  className={`py-2 rounded-md text-sm font-medium border transition ${
                    form.role === r
                      ? 'bg-ink text-paper border-ink'
                      : 'bg-white text-slate border-ink/15 hover:border-ink/40'
                  }`}
                >
                  {r === 'STUDENT' ? 'Student' : 'Recruiter'}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
              Full Name
            </label>
            <input
              type="text"
              name="fullName"
              required
              value={form.fullName}
              onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-white border border-ink/15 rounded-md text-ink placeholder:text-slate/50 focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
              placeholder="Jordan Lee"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
              Email
            </label>
            <input
              type="email"
              name="email"
              required
              value={form.email}
              onChange={handleChange}
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
              name="password"
              required
              minLength={6}
              value={form.password}
              onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-white border border-ink/15 rounded-md text-ink placeholder:text-slate/50 focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
              placeholder="At least 6 characters"
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
            {loading ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate">
          Already have an account?{' '}
          <Link to="/login" className="text-teal font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}