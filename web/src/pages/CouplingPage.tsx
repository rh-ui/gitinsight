import { useEffect, useMemo, useRef, useState } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import { AnalysisGate } from '../components/AnalysisGate';
import { toCouplingGraph, type GraphLink, type GraphNode } from '../lib/graph';
import { useTheme } from '../theme/ThemeProvider';
import type { FileCoupling } from '../types/analysis';

interface Palette {
  node: string;
  link: string;
  text: string;
}

/** Couleurs du canvas dérivées du thème (canvas = hex en JS, pas de classes). */
function palette(theme: 'light' | 'dark'): Palette {
  return theme === 'dark'
    ? { node: '#f47d20', link: '#475569', text: '#e2e8f0' }
    : { node: '#f47d20', link: '#94a3b8', text: '#0f172a' };
}

function CouplingGraph({ coupling }: { coupling: FileCoupling[] }) {
  const { theme } = useTheme();
  const colors = palette(theme);
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);
  const height = 480;

  const data = useMemo(() => toCouplingGraph(coupling), [coupling]);

  // Le graphe se redimensionne avec son conteneur (responsive).
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const ro = new ResizeObserver((entries) => setWidth(entries[0].contentRect.width));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  if (coupling.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-border p-12 text-center text-muted">
        <p>Pas assez de co-changements pour dessiner un graphe.</p>
        <p className="text-xs">Il faut des fichiers modifiés ensemble au moins 2 fois.</p>
      </div>
    );
  }

  return (
    <section className="rounded-lg border border-border bg-surface p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-foreground">Couplage temporel</h2>
        <span className="text-xs text-muted">
          épaisseur d'arête = force du couplage (Jaccard) · taille du nœud = nb de liens
        </span>
      </div>
      <div ref={containerRef} className="h-[480px] w-full overflow-hidden rounded-md">
        {width > 0 && (
          <ForceGraph2D
            width={width}
            height={height}
            graphData={data}
            backgroundColor="rgba(0,0,0,0)"
            nodeId="id"
            nodeLabel={(n) => (n as GraphNode).id}
            linkColor={() => colors.link}
            linkWidth={(l) => 0.5 + (l as GraphLink).value * 4}
            nodeCanvasObject={(node, ctx, globalScale) => {
              const n = node as GraphNode & { x: number; y: number };
              const radius = 3 + Math.sqrt(n.degree) * 1.5;
              ctx.beginPath();
              ctx.arc(n.x, n.y, radius, 0, 2 * Math.PI);
              ctx.fillStyle = colors.node;
              ctx.fill();
              // Libellé seulement quand on zoome assez (sinon illisible).
              if (globalScale > 1.5) {
                ctx.font = `${10 / globalScale}px sans-serif`;
                ctx.fillStyle = colors.text;
                ctx.textAlign = 'center';
                ctx.textBaseline = 'top';
                ctx.fillText(n.label, n.x, n.y + radius + 1);
              }
            }}
          />
        )}
      </div>
    </section>
  );
}

/** Couplage temporel : graphe de réseau des fichiers qui changent ensemble. */
export function CouplingPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Couplage</h1>
      <AnalysisGate>{(data) => <CouplingGraph coupling={data.coupling} />}</AnalysisGate>
    </div>
  );
}