import { useState } from "react";
import { truncatePath } from "../lib/format";
import type { FileCoupling } from "../types/analysis";

export default function TopCouplingsPanel({ coupling }: { coupling: FileCoupling[] }) {
    const [open, setOpen] = useState(true);
    const BORDER = '#334155';
    return (
        <div
            className="absolute right-0 top-0 z-20 flex h-full w-80 flex-col border-l backdrop-blur transition-transform duration-300"
            style={{
                borderColor: BORDER,
                backgroundColor: 'rgba(19,27,46,0.85)',
                transform: open ? 'translateX(0)' : 'translateX(100%)',
            }}
        >
            {/* Onglet de bascule (déborde sur la gauche du panneau) */}
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                aria-label={open ? 'Replier le panneau' : 'Déplier le panneau'}
                className="absolute -left-10 top-6 flex h-10 w-10 items-center justify-center rounded-l-md border border-r-0 text-slate-200 transition-colors hover:text-[#f47d20]"
                style={{ borderColor: BORDER, backgroundColor: 'rgba(19,27,46,0.85)' }}
            >
                {open ? '›' : '‹'}
            </button>

            <div
                className="flex items-center justify-between border-b px-5 py-4"
                style={{ borderColor: BORDER }}
            >
                <h2 className="text-xs font-bold uppercase tracking-wider text-slate-200">Top Couplages</h2>
                <span
                    className="cursor-help text-slate-400"
                    title="Paires de fichiers les plus couplées, triées par score de Jaccard (0–1)."
                >
                    ⓘ
                </span>
            </div>

            <div className="flex-1 space-y-3 overflow-y-auto p-4">
                {coupling.map((c, i) => {
                    const pct = Math.round(c.couplingScore * 100);
                    return (
                        <div
                            key={i}
                            className="rounded-lg border p-3 transition-colors hover:border-[#f47d20]/50"
                            style={{ borderColor: BORDER, backgroundColor: '#222a3d' }}
                        >
                            <div className="mb-1 flex items-start justify-between gap-2">
                                <span className="truncate font-mono text-xs text-slate-100" title={c.fileA}>
                                    {truncatePath(c.fileA, 26)}
                                </span>
                                <span className="shrink-0 font-mono text-xs text-[#f47d20]">
                                    {c.couplingScore.toFixed(2)}
                                </span>
                            </div>
                            <div className="mb-2 truncate font-mono text-xs text-slate-400" title={c.fileB}>
                                {truncatePath(c.fileB, 26)}
                            </div>
                            <div className="h-1.5 w-full overflow-hidden rounded-full" style={{ backgroundColor: '#171f33' }}>
                                <div className="h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: '#f47d20' }} />
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}