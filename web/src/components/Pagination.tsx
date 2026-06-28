import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  page: number;
  pageCount: number;
  rangeStart: number;
  rangeEnd: number;
  total: number;
  canPrev: boolean;
  canNext: boolean;
  onPrev: () => void;
  onNext: () => void;
}

/** Contrôles de pagination : plage affichée + boutons précédent/suivant. */
export function Pagination({
  page,
  pageCount,
  rangeStart,
  rangeEnd,
  total,
  canPrev,
  canNext,
  onPrev,
  onNext,
}: PaginationProps) {
  if (total === 0) return null;

  return (
    <div className="flex items-center justify-between pt-3 text-sm text-muted">
      <span className="font-mono text-xs">
        {rangeStart}–{rangeEnd} sur {total}
      </span>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onPrev}
          disabled={!canPrev}
          aria-label="Page précédente"
          className="rounded-md border border-border p-1.5 transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-border disabled:hover:text-muted"
        >
          <ChevronLeft size={16} />
        </button>
        <span className="font-mono text-xs">
          {page} / {pageCount}
        </span>
        <button
          type="button"
          onClick={onNext}
          disabled={!canNext}
          aria-label="Page suivante"
          className="rounded-md border border-border p-1.5 transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-border disabled:hover:text-muted"
        >
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
}
