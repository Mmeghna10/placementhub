const commonFaqs = [
  {
    keywords: ['what', 'placementhub', 'app', 'about'],
    answer: "PlacementHub connects students, recruiters, and placement cells in one place — students apply to jobs, recruiters post and manage roles, and admins approve postings and track activity.",
  },
  {
    keywords: ['logout', 'log out', 'sign out'],
    answer: "Click your email in the top navbar, or use the Logout button next to it, in the top-right corner.",
  },
  {
    keywords: ['password', 'forgot', 'reset'],
    answer: "Password reset isn't available yet in this version — for now, reach out to whoever set up your account.",
  },
  {
    keywords: ['who', 'built', 'made', 'creator'],
    answer: "PlacementHub was built as a full-stack project using Spring Boot and React.",
  },
];

const studentFaqs = [
  {
    keywords: ['edit', 'update', 'profile', 'change', 'details'],
    answer: "Click your email in the navbar, or go to 'Profile', then hit the Edit button. Fill in your details and Save — a complete profile helps you show up in Recommended Jobs.",
  },
  {
    keywords: ['recommend', 'match', 'suggested'],
    answer: "Recommended Jobs filters open roles by your CGPA and branch. Make sure your Branch field matches how recruiters write it (e.g. \"Computer Science\") for the best matches.",
  },
  {
    keywords: ['apply', 'application', 'applying'],
    answer: "Open any job from Browse Jobs, then click 'Apply Now' on the details page. You can only apply once per job.",
  },
  {
    keywords: ['status', 'shortlisted', 'rejected', 'offered', 'interview'],
    answer: "Your application moves through APPLIED → SHORTLISTED → INTERVIEW → OFFERED or REJECTED as the recruiter reviews it. Check My Applications to see where each one stands.",
  },
  {
    keywords: ['timeline', 'history', 'track'],
    answer: "Click into any application from 'My Applications' to see its full timeline — every status change with a date, in order.",
  },
  {
    keywords: ['pending', 'approve'],
    answer: "Jobs start as PENDING when a recruiter posts them, and only appear in Browse Jobs once an admin approves them.",
  },
];

const recruiterFaqs = [
  {
    keywords: ['post', 'create', 'new job'],
    answer: "Go to My Jobs → 'Post a Job', fill in the details, and submit. New jobs start as PENDING until an admin approves them.",
  },
  {
    keywords: ['edit', 'update job'],
    answer: "In My Jobs, click the pencil icon next to any job to edit its details.",
  },
  {
    keywords: ['delete', 'remove job'],
    answer: "In My Jobs, click the trash icon next to a job. This can't be undone, so you'll get a confirmation prompt first.",
  },
  {
    keywords: ['company', 'organisation', 'organization'],
    answer: "Your personal details (name, phone) live under your email → Profile. Your company's details (name, industry, description) live under 'Company' in the navbar — they're separate on purpose.",
  },
  {
    keywords: ['applicant', 'candidate', 'apply', 'who applied'],
    answer: "Click the people icon next to any job in My Jobs to see everyone who's applied, and change their status from there.",
  },
  {
    keywords: ['status', 'shortlist', 'reject', 'offer'],
    answer: "In the Applicants list, use the dropdown next to each applicant to move them through APPLIED → SHORTLISTED → INTERVIEW → OFFERED/REJECTED. They'll see the update on their end immediately.",
  },
  {
    keywords: ['pending', 'approved', 'waiting'],
    answer: "Every new job starts as PENDING and isn't visible to students until an admin approves it — this usually happens quickly, but it's outside your control.",
  },
];

const adminFaqs = [
  {
    keywords: ['approve', 'reject', 'pending job'],
    answer: "Go to Jobs, find any job marked PENDING, and use the Approve or Reject button right there.",
  },
  {
    keywords: ['student', 'view students'],
    answer: "The Students page lists every registered student with their college, branch, and graduation year.",
  },
  {
    keywords: ['recruiter', 'view recruiters'],
    answer: "The Recruiters page lists every registered recruiter along with their company.",
  },
  {
    keywords: ['analytics', 'chart', 'graph', 'stats'],
    answer: "Analytics shows two views: a bar chart of applications by status, and a pie chart breaking down job postings by approval status.",
  },
  {
    keywords: ['dashboard', 'overview', 'summary'],
    answer: "The Dashboard gives you totals at a glance — students, recruiters, companies, jobs, pending approvals, and applications.",
  },
];

const faqsByRole = {
  STUDENT: [...studentFaqs, ...commonFaqs],
  RECRUITER: [...recruiterFaqs, ...commonFaqs],
  ADMIN: [...adminFaqs, ...commonFaqs],
};

const greetings = ['hi', 'hello', 'hey', 'yo', 'sup'];

export function getGuideAnswer(question, role) {
  const q = question.toLowerCase().trim();

  if (greetings.some((g) => q === g || q.startsWith(g + ' '))) {
    return "Hey! Ask me anything about using PlacementHub — profile, jobs, applications, whatever you're stuck on.";
  }

  const faqs = faqsByRole[role] || commonFaqs;

  let bestMatch = null;
  let bestScore = 0;

  for (const faq of faqs) {
    const score = faq.keywords.filter((k) => q.includes(k)).length;
    if (score > bestScore) {
      bestScore = score;
      bestMatch = faq;
    }
  }

  if (bestMatch && bestScore > 0) {
    return bestMatch.answer;
  }

  const fallbackByRole = {
    STUDENT: "I'm not sure about that one. Try asking about your profile, browsing jobs, applying, or tracking application status.",
    RECRUITER: "I'm not sure about that one. Try asking about posting a job, editing your company profile, or reviewing applicants.",
    ADMIN: "I'm not sure about that one. Try asking about approving jobs, viewing students/recruiters, or analytics.",
  };

  return fallbackByRole[role] || "I'm not sure about that one — try rephrasing, or ask about profiles, jobs, or applications.";
}