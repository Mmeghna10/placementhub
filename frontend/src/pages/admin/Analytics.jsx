import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { getAnalytics } from '../../api/adminApi';

const PIE_COLORS = ['#14555A', '#B08D57', '#5B6B75'];

export default function Analytics() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAnalytics().then((res) => setData(res.data)).finally(() => setLoading(false));
  }, []);

  if (loading || !data) return <DashboardLayout><LoadingState /></DashboardLayout>;

  const jobStatusData = [
    { name: 'Approved', value: data.approvedJobs },
    { name: 'Rejected', value: data.rejectedJobs },
    { name: 'Pending', value: data.totalJobsPosted - data.approvedJobs - data.rejectedJobs },
  ];

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">Analytics</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white border border-ink/10 rounded-lg p-6">
          <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-6">
            Applications by Status
          </h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data.applicationsByStatus}>
              <XAxis dataKey="status" tick={{ fontSize: 11, fontFamily: 'IBM Plex Mono' }} stroke="#5B6B75" />
              <YAxis tick={{ fontSize: 11 }} stroke="#5B6B75" allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#14555A" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white border border-ink/10 rounded-lg p-6">
          <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-6">
            Job Postings Breakdown
          </h2>
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={jobStatusData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90}>
                {jobStatusData.map((entry, index) => (
                  <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
          <div className="flex justify-center gap-4 mt-2">
            {jobStatusData.map((entry, index) => (
              <div key={entry.name} className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: PIE_COLORS[index] }} />
                <span className="text-xs text-slate">{entry.name}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}