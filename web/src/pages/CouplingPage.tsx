import { useMemo, useRef, useState, type ReactNode } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Line, OrbitControls, PerspectiveCamera } from '@react-three/drei';
import type { OrbitControls as OrbitControlsImpl } from 'three-stdlib';
import * as THREE from 'three';
import { AnalysisGate } from '../components/AnalysisGate';
import { toCouplingGraph, type GraphLink, type GraphNode } from '../lib/graph';
import { truncatePath } from '../lib/format';
import type { FileCoupling } from '../types/analysis';

const NODE_COLOR = '#f47d20';
const CANVAS_BG = '#0b1326';
const CONTAINER_BG = '#131b2e';
const BORDER = '#334155';

type Pos = [number, number, number];
interface Positioned extends GraphNode {
  pos: Pos;
}

/** Positions ~uniformes dans une sphère (figées via useMemo sur l'ensemble). */
function layout(nodes: GraphNode[], radius = 8): Positioned[] {
  return nodes.map((n) => {
    const r = radius * Math.cbrt(Math.random());
    const theta = Math.random() * Math.PI * 2;
    const phi = Math.acos(2 * Math.random() - 1);
    return {
      ...n,
      pos: [
        r * Math.sin(phi) * Math.cos(theta),
        r * Math.sin(phi) * Math.sin(theta),
        r * Math.cos(phi),
      ],
    };
  });
}

function Graph3D({
  nodes,
  links,
  posById,
}: {
  nodes: Positioned[];
  links: GraphLink[];
  posById: Map<string, Pos>;
}) {
  const groupRef = useRef<THREE.Group>(null);
  useFrame(({ clock }) => {
    if (groupRef.current) {
      groupRef.current.position.y = Math.sin(clock.elapsedTime * 0.6) * 0.3;
    }
  });
  return (
    <group ref={groupRef}>
      {links.map((l, i) => {
        const a = posById.get(l.source);
        const b = posById.get(l.target);
        if (!a || !b) return null;
        return (
          <Line key={i} points={[a, b]} color={NODE_COLOR} transparent opacity={0.12 + l.value * 0.6} lineWidth={1} />
        );
      })}
      {nodes.map((n) => {
        const radius = 0.25 + Math.sqrt(n.degree) * 0.12;
        return (
          <mesh key={n.id} position={n.pos}>
            <sphereGeometry args={[radius, 16, 16]} />
            <meshStandardMaterial color={NODE_COLOR} emissive={NODE_COLOR} emissiveIntensity={0.5} />
          </mesh>
        );
      })}
    </group>
  );
}

function CtrlButton({ children, label, onClick, active = false }: {
  children: ReactNode; label: string; onClick: () => void; active?: boolean;
}) {
  return (
    <button
      type="button"
      title={label}
      aria-label={label}
      onClick={onClick}
      className={`flex h-10 w-10 items-center justify-center rounded-md border text-lg transition-colors ${
        active ? 'border-[#f47d20] text-[#f47d20]' : 'border-[#334155] text-slate-200 hover:border-[#f47d20]'
      }`}
      style={{ backgroundColor: 'rgba(19,27,46,0.7)' }}
    >
      {children}
    </button>
  );
}

function TopCouplingsPanel({
  coupling,
  threshold,
  onThreshold,
}: {
  coupling: FileCoupling[];
  threshold: number;
  onThreshold: (v: number) => void;
}) {
  const [open, setOpen] = useState(true);
  return (
    <div
      className="absolute right-0 top-0 z-20 flex h-full w-80 flex-col border-l backdrop-blur transition-transform duration-300"
      style={{ borderColor: BORDER, backgroundColor: 'rgba(19,27,46,0.85)', transform: open ? 'translateX(0)' : 'translateX(100%)' }}
    >
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={open ? 'Replier le panneau' : 'Déplier le panneau'}
        className="absolute -left-10 top-6 flex h-10 w-10 items-center justify-center rounded-l-md border border-r-0 text-slate-200 transition-colors hover:text-[#f47d20]"
        style={{ borderColor: BORDER, backgroundColor: 'rgba(19,27,46,0.85)' }}
      >
        {open ? '›' : '‹'}
      </button>

      <div className="flex items-center justify-between border-b px-5 py-4" style={{ borderColor: BORDER }}>
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-200">Top Couplages</h2>
        <span className="cursor-help text-slate-400" title="Paires les plus couplées, triées par score de Jaccard (0–1).">ⓘ</span>
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto p-4">
        {coupling.length === 0 ? (
          <p className="px-1 pt-4 text-center text-xs text-slate-400">Aucune paire au-dessus du seuil.</p>
        ) : (
          coupling.map((c, i) => {
            const pct = Math.round(c.couplingScore * 100);
            return (
              <div key={i} className="rounded-lg border p-3 transition-colors hover:border-[#f47d20]/50" style={{ borderColor: BORDER, backgroundColor: '#222a3d' }}>
                <div className="mb-1 flex items-start justify-between gap-2">
                  <span className="truncate font-mono text-xs text-slate-100" title={c.fileA}>{truncatePath(c.fileA, 26)}</span>
                  <span className="shrink-0 font-mono text-xs text-[#f47d20]">{c.couplingScore.toFixed(2)}</span>
                </div>
                <div className="mb-2 truncate font-mono text-xs text-slate-400" title={c.fileB}>{truncatePath(c.fileB, 26)}</div>
                <div className="h-1.5 w-full overflow-hidden rounded-full" style={{ backgroundColor: '#171f33' }}>
                  <div className="h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: '#f47d20' }} />
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="border-t p-4" style={{ borderColor: BORDER }}>
        <div className="mb-3 flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Filtres actifs</span>
          <button type="button" onClick={() => onThreshold(0)} className="text-[10px] font-bold uppercase text-[#f47d20]">Reset</button>
        </div>
        <label className="flex flex-col gap-2">
          <span className="font-mono text-[11px] text-slate-200">Jaccard ≥ {threshold.toFixed(2)}</span>
          <input type="range" min={0} max={1} step={0.05} value={threshold} onChange={(e) => onThreshold(Number(e.target.value))} className="w-full accent-[#f47d20]" />
        </label>
      </div>
    </div>
  );
}

function CouplingScene({ coupling }: { coupling: FileCoupling[] }) {
  const controlsRef = useRef<OrbitControlsImpl>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera>(null);
  const [autoRotate, setAutoRotate] = useState(true);
  const [threshold, setThreshold] = useState(0);

  // Calculé UNE fois sur l'ensemble → positions stables pendant le filtrage.
  const graph = useMemo(() => toCouplingGraph(coupling), [coupling]);
  const positioned = useMemo(() => layout(graph.nodes), [graph]);
  const posById = useMemo(() => new Map(positioned.map((n) => [n.id, n.pos])), [positioned]);

  const links = useMemo(() => graph.links.filter((l) => l.value >= threshold), [graph, threshold]);
  const nodes = useMemo(() => {
    const ids = new Set<string>();
    links.forEach((l) => {
      ids.add(l.source);
      ids.add(l.target);
    });
    return positioned.filter((n) => ids.has(n.id));
  }, [positioned, links]);
  const filteredCoupling = useMemo(() => coupling.filter((c) => c.couplingScore >= threshold), [coupling, threshold]);

  function dolly(factor: number) {
    const cam = cameraRef.current;
    const ctr = controlsRef.current;
    if (!cam || !ctr) return;
    cam.position.sub(ctr.target).multiplyScalar(factor).add(ctr.target);
    ctr.update();
  }

  if (coupling.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-border p-12 text-center text-muted">
        <p>Pas assez de co-changements pour dessiner un graphe.</p>
        <p className="text-xs">Il faut des fichiers modifiés ensemble au moins 2 fois.</p>
      </div>
    );
  }

  return (
    <div className="relative h-[70vh] w-full overflow-hidden rounded-xl border" style={{ backgroundColor: CONTAINER_BG, borderColor: BORDER }}>
      <div className="absolute left-4 top-4 z-10 flex flex-col gap-2">
        <CtrlButton label="Zoom avant" onClick={() => dolly(0.8)}>+</CtrlButton>
        <CtrlButton label="Zoom arrière" onClick={() => dolly(1.25)}>−</CtrlButton>
        <CtrlButton label="Réinitialiser la vue" onClick={() => controlsRef.current?.reset()}>⟲</CtrlButton>
        <CtrlButton label="Rotation auto" active={autoRotate} onClick={() => setAutoRotate((r) => !r)}>⟳</CtrlButton>
      </div>

      <div className="absolute bottom-4 left-4 z-10 rounded-lg border p-3 text-[11px] text-slate-300 backdrop-blur" style={{ borderColor: BORDER, backgroundColor: 'rgba(19,27,46,0.8)' }}>
        <div className="mb-1 flex items-center gap-2">
          <span className="inline-block h-3 w-3 rounded-full" style={{ background: NODE_COLOR }} />
          Taille = nb de liens
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-block h-0.5 w-4" style={{ background: NODE_COLOR }} />
          Opacité = force du couplage
        </div>
      </div>

      <Canvas style={{ background: CANVAS_BG }}>
        <PerspectiveCamera ref={cameraRef} makeDefault position={[0, 0, 22]} fov={60} />
        <ambientLight intensity={0.6} />
        <pointLight position={[10, 10, 10]} intensity={1.2} />
        <Graph3D nodes={nodes} links={links} posById={posById} />
        <OrbitControls ref={controlsRef} autoRotate={autoRotate} autoRotateSpeed={0.6} enableDamping dampingFactor={0.1} minDistance={5} maxDistance={60} />
      </Canvas>

      <TopCouplingsPanel coupling={filteredCoupling} threshold={threshold} onThreshold={setThreshold} />
    </div>
  );
}

export function CouplingPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Couplage</h1>
      <AnalysisGate>{(data) => <CouplingScene coupling={data.coupling} />}</AnalysisGate>
    </div>
  );
}