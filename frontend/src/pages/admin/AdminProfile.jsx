import { useAuth } from '../../context/AuthContext';
import DashboardLayout from '../../components/DashboardLayout';

export default function AdminProfile() {
  const { user } = useAuth();

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">Account</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8 max-w-md bg-white border border-ink/10 rounded-lg p-6">
        <dl className="space-y-5">
          <div>
            <dt className="text-xs font-medium text-slate uppercase tracking-wide">Email</dt>
            <dd className="text-sm text-ink mt-1 font-mono">{user?.email}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-slate uppercase tracking-wide">Role</dt>
            <dd className="text-sm text-ink mt-1">Administrator</dd>
          </div>
        </dl>
        <p className="text-xs text-slate mt-6 pt-5 border-t border-ink/10">
          Admin accounts are provisioned directly and don't have editable profile fields.
        </p>
      </div>
    </DashboardLayout>
  );
}