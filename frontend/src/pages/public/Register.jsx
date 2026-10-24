import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';
import { Briefcase, GraduationCap } from 'lucide-react';
import AuthLayout, { authInputClass, authLabelClass } from '../../components/AuthLayout';

const roleOptions = [
  { value: 'STUDENT', label: 'Student', icon: GraduationCap },
  { value: 'RECRUITER', label: 'Recruiter', icon: Briefcase },
];

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
      setError(err.response?.data?.error || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Join as a student looking for roles, or a recruiter hiring on campus."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="text-teal font-medium hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <p className={authLabelClass}>I am a</p>
          <div className="grid grid-cols-2 gap-3">
            {roleOptions.map(({ value, label, icon: Icon }) => (
              <button
                key={value}
                type="button"
                aria-pressed={form.role === value}
                onClick={() => setForm({ ...form, role: value })}
                className={`flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-medium border transition ${
                  form.role === value
                    ? 'bg-ink text-paper border-ink shadow-sm'
                    : 'bg-white text-slate border-ink/15 hover:border-ink/40 hover:text-ink'
                }`}
              >
                <Icon size={16} strokeWidth={1.9} />
                {label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label htmlFor="register-full-name" className={authLabelClass}>
            Full Name
          </label>
          <input
            id="register-full-name"
            type="text"
            name="fullName"
            required
            autoComplete="name"
            value={form.fullName}
            onChange={handleChange}
            className={authInputClass}
            placeholder="Jordan Lee"
          />
        </div>

        <div>
          <label htmlFor="register-email" className={authLabelClass}>
            Email
          </label>
          <input
            id="register-email"
            type="email"
            name="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={handleChange}
            className={authInputClass}
            placeholder="you@college.edu"
          />
        </div>

        <div>
          <label htmlFor="register-password" className={authLabelClass}>
            Password
          </label>
          <input
            id="register-password"
            type="password"
            name="password"
            required
            minLength={6}
            autoComplete="new-password"
            value={form.password}
            onChange={handleChange}
            className={authInputClass}
            placeholder="At least 6 characters"
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
          {loading ? 'Creating account…' : 'Create Account'}
        </button>
      </form>
    </AuthLayout>
  );
}
