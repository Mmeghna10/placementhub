import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Search } from 'lucide-react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import Pagination from '../../components/Pagination';
import { searchJobs } from '../../api/studentApi';

const PAGE_SIZE = 10;
const emptyFilters = { q: '', jobType: '', location: '', sort: 'newest' };
const inputClass =
  'px-3 py-2 bg-white border border-ink/15 rounded-md text-sm text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition';

export default function BrowseJobs() {
  const [jobs, setJobs] = useState([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // `form` is what the student is typing; `query` is what was last searched for.
  const [form, setForm] = useState(emptyFilters);
  const [query, setQuery] = useState({ ...emptyFilters, eligibleOnly: false, page: 0 });

  useEffect(() => {
    searchJobs({ ...query, size: PAGE_SIZE })
      .then((res) => {
        const { content, ...info } = res.data;
        setJobs(content);
        setPageInfo(info);
        setError('');
      })
      .catch((err) => setError(err.response?.data?.error || 'Could not load jobs'))
      .finally(() => setLoading(false));
  }, [query]);

  // Every change of what is shown goes through here, so loading is set by the event,
  // not inside the effect.
  const runQuery = (changes) => {
    setLoading(true);
    setQuery((current) => ({ ...current, ...changes }));
  };

  const handleSearch = (e) => {
    e.preventDefault();
    runQuery({ ...form, page: 0 });
  };

  const handleClear = () => {
    setForm(emptyFilters);
    runQuery({ ...emptyFilters, page: 0 });
  };

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const showRecommended = query.eligibleOnly;
  const filtersActive = query.q || query.jobType || query.location;

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">
            {showRecommended ? 'Recommended For You' : 'Open Roles'}
          </h1>
          <div className="mt-1 w-12 border-t border-ink/30" />
        </div>

        <div className="flex bg-white border border-ink/10 rounded-md p-1 self-start">
          <button
            onClick={() => showRecommended && runQuery({ eligibleOnly: false, page: 0 })}
            className={`px-3.5 py-1.5 rounded text-sm font-medium transition ${!showRecommended ? 'bg-ink text-paper' : 'text-slate'}`}
          >
            All
          </button>
          <button
            onClick={() => !showRecommended && runQuery({ eligibleOnly: true, page: 0 })}
            className={`px-3.5 py-1.5 rounded text-sm font-medium transition ${showRecommended ? 'bg-ink text-paper' : 'text-slate'}`}
          >
            Recommended
          </button>
        </div>
      </div>

      <form onSubmit={handleSearch} className="mt-6 flex flex-col lg:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate/60" />
          <input
            name="q" value={form.q} onChange={handleChange} placeholder="Title, company or skill"
            className={`${inputClass} w-full pl-9`} aria-label="Search jobs"
          />
        </div>
        <select name="jobType" value={form.jobType} onChange={handleChange} className={inputClass} aria-label="Job type">
          <option value="">Any type</option>
          <option value="FULL_TIME">Full time</option>
          <option value="INTERNSHIP">Internship</option>
        </select>
        <input name="location" value={form.location} onChange={handleChange} placeholder="Location" className={inputClass} aria-label="Location" />
        <select name="sort" value={form.sort} onChange={handleChange} className={inputClass} aria-label="Sort by">
          <option value="newest">Newest first</option>
          <option value="deadline">Deadline soonest</option>
          <option value="salary">Highest salary</option>
        </select>
        <button type="submit" className="px-4 py-2 bg-ink text-paper rounded-md text-sm font-medium hover:bg-ink/90 transition">
          Search
        </button>
        {filtersActive && (
          <button type="button" onClick={handleClear} className="px-3 py-2 text-sm text-teal hover:underline">
            Clear
          </button>
        )}
      </form>

      <div className="mt-6">
        {error && <p className="text-sm text-brick font-medium mb-4">{error}</p>}
        {loading ? (
          <LoadingState />
        ) : jobs.length === 0 ? (
          <EmptyState
            title={filtersActive ? 'No roles match your search' : 'No roles here yet'}
            description={
              filtersActive
                ? 'Try different words or clear the filters.'
                : showRecommended
                  ? "We'll match roles once your profile (CGPA, branch) is complete."
                  : 'Check back soon for new postings.'
            }
          />
        ) : (
          <>
            <p className="text-xs font-mono text-slate mb-3">{pageInfo.totalElements} role{pageInfo.totalElements === 1 ? '' : 's'}</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {jobs.map((job) => (
                <Link
                  key={job.id}
                  to={`/student/jobs/${job.id}`}
                  className="bg-white border border-ink/10 rounded-lg p-5 hover:border-teal/40 transition"
                >
                  <p className="text-xs font-mono uppercase tracking-wide text-brass">{job.companyName}</p>
                  <h3 className="font-display text-lg font-semibold text-ink mt-1">{job.title}</h3>
                  <p className="text-sm text-slate mt-2 line-clamp-2">{job.description}</p>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-4 text-xs text-slate">
                    <span>{job.location}</span>
                    <span>·</span>
                    <span>{job.jobType?.replace('_', ' ')}</span>
                    {job.salaryMin && (
                      <>
                        <span>·</span>
                        <span>₹{job.salaryMin.toLocaleString()}–{job.salaryMax?.toLocaleString()}</span>
                      </>
                    )}
                    {job.applicationDeadline && (
                      <>
                        <span>·</span>
                        <span>Apply by {job.applicationDeadline}</span>
                      </>
                    )}
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
        <Pagination {...pageInfo} onPageChange={(page) => runQuery({ page })} disabled={loading} />
      </div>
    </DashboardLayout>
  );
}
