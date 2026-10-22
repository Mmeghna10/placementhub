// Previous/next controls for the backend's PageResponse ({ content, page, size, totalElements, totalPages }).
export default function Pagination({ page, totalPages, totalElements, onPageChange, disabled }) {
  if (totalPages <= 1) return null;

  const buttonClass =
    'px-3 py-1.5 border border-ink/15 rounded-md text-xs font-medium text-ink hover:bg-ink/5 transition disabled:opacity-40 disabled:cursor-not-allowed';

  return (
    <div className="flex items-center justify-between mt-4 text-xs text-slate">
      <span className="font-mono">
        Page {page + 1} of {totalPages} · {totalElements} total
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          className={buttonClass}
          disabled={disabled || page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className={buttonClass}
          disabled={disabled || page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
