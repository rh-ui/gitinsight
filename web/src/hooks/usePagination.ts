import { useEffect, useMemo, useState } from 'react';

export interface Pagination<T> {
  page: number; // page courante (1-based)
  pageCount: number; // nombre total de pages
  pageItems: T[]; // éléments de la page courante
  total: number; // nombre total d'éléments
  rangeStart: number; // index 1-based du 1er élément affiché
  rangeEnd: number; // index 1-based du dernier élément affiché
  canPrev: boolean;
  canNext: boolean;
  next: () => void;
  prev: () => void;
  setPage: (p: number) => void;
}

/**
 * Pagination locale générique sur un tableau déjà trié/filtré.
 * Recalcule la tranche courante et borne la page si la liste rétrécit
 * (ex. nouvelle analyse, tri).
 */
export function usePagination<T>(items: T[], pageSize = 10): Pagination<T> {
  const [page, setPage] = useState(1);

  const pageCount = Math.max(1, Math.ceil(items.length / pageSize));

  // Si la liste change et que la page courante n'existe plus, on la borne.
  useEffect(() => {
    if (page > pageCount) setPage(pageCount);
  }, [page, pageCount]);

  const pageItems = useMemo(() => {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
  }, [items, page, pageSize]);

  const total = items.length;
  const rangeStart = total === 0 ? 0 : (page - 1) * pageSize + 1;
  const rangeEnd = Math.min(page * pageSize, total);

  return {
    page,
    pageCount,
    pageItems,
    total,
    rangeStart,
    rangeEnd,
    canPrev: page > 1,
    canNext: page < pageCount,
    next: () => setPage((p) => Math.min(p + 1, pageCount)),
    prev: () => setPage((p) => Math.max(p - 1, 1)),
    setPage,
  };
}
