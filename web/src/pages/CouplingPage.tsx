import { useMemo, useRef, useState, type ReactNode } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Line, OrbitControls, PerspectiveCamera } from '@react-three/drei';
import type { OrbitControls as OrbitControlsImpl } from 'three-stdlib';
import * as THREE from 'three';
import { AnalysisGate } from '../components/AnalysisGate';
import { toCouplingGraph, type GraphNode } from '../lib/graph';
import type { FileCoupling } from '../types/analysis';
import TopCouplingsPanel from '../components/TopCouplingsPanel';

const NODE_COLOR = '#f47d20'; // forge-orange
const CANVAS_BG = '#0b1326'; // fond de scène (palette sombre demandée)
const CONTAINER_BG = '#131b2e'; // conteneur
const BORDER = '#334155';

interface Positioned extends GraphNode {
  pos: [number, number, number];
}

/** Place les nœuds ~uniformément dans une sphère (positions figées via useMemo). */
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

/** Le graphe lui-même : sphères (nœuds) + lignes (arêtes), avec flottement doux. */
function Graph3D({ coupling }: { coupling: FileCoupling[] }) {
  const groupRef = useRef<THREE.Group>(null);
  const graph = useMemo(() => toCouplingGraph(coupling), [coupling]);
  const nodes = useMemo(() => layout(graph.nodes), [graph]);
  const posById = useMemo(() => new Map(nodes.map((n) => [n.id, n.pos])), [nodes]);

  // Flottement : on anime tout le groupe → les arêtes restent collées aux nœuds.
  useFrame(({ clock }) => {
    if (groupRef.current) {
      groupRef.current.position.y = Math.sin(clock.elapsedTime * 0.6) * 0.3;
    }
  });

  return (
    <group ref={groupRef}>
      {graph.links.map((l, i) => {
        const a = posById.get(l.source);
        const b = posById.get(l.target);
        if (!a || !b) return null;
        return (
          <Line
            key={i}
            points={[a, b]}
            color={NODE_COLOR}
            transparent
            opacity={0.12 + l.value * 0.6} // opacité = force du couplage (Jaccard)
            lineWidth={1}
          />
        );
      })}
      {nodes.map((n) => {
        const radius = 0.25 + Math.sqrt(n.degree) * 0.12; // taille = nb de liens
        return (
          <mesh key={n.id} position={n.pos}>
            <sphereGeometry args={[radius, 16, 16]} />
            <meshStandardMaterial
              color={NODE_COLOR}
              emissive={NODE_COLOR}
              emissiveIntensity={0.5}
            />
          </mesh>
        );
      })}
    </group>
  );
}

/** Bouton de contrôle flottant (zoom / reset / rotation). */
function CtrlButton({
  children,
  label,
  onClick,
  active = false,
}: {
  children: ReactNode;
  label: string;
  onClick: () => void;
  active?: boolean;
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
      style={{ backgroundColor: 'rgba(19,27,46,0.7)', backdropFilter: 'blur(8px)' }}
    >
      {children}
    </button>
  );
}

function CouplingScene({ coupling }: { coupling: FileCoupling[] }) {
  const controlsRef = useRef<OrbitControlsImpl>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera>(null);
  const [autoRotate, setAutoRotate] = useState(true);

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
    <div
      className="relative h-[70vh] w-full overflow-hidden rounded-xl border"
      style={{ backgroundColor: CONTAINER_BG, borderColor: BORDER }}
    >
      <div className="absolute left-4 top-4 z-10 flex flex-col gap-2">
        <CtrlButton label="Zoom avant" onClick={() => dolly(0.8)}>+</CtrlButton>
        <CtrlButton label="Zoom arrière" onClick={() => dolly(1.25)}>−</CtrlButton>
        <CtrlButton label="Réinitialiser la vue" onClick={() => controlsRef.current?.reset()}>⟲</CtrlButton>
        <CtrlButton label="Rotation auto" active={autoRotate} onClick={() => setAutoRotate((r) => !r)}>⟳</CtrlButton>
      </div>

      <div
        className="absolute bottom-4 right-4 z-10 rounded-lg border p-3 text-[11px] text-slate-300 backdrop-blur"
        style={{ borderColor: BORDER, backgroundColor: 'rgba(19,27,46,0.8)' }}
      >
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
        <Graph3D coupling={coupling} />
        <OrbitControls
          ref={controlsRef}
          autoRotate={autoRotate}
          autoRotateSpeed={0.6}
          enableDamping
          dampingFactor={0.1}
          minDistance={5}
          maxDistance={60}
        />
      </Canvas>


      <TopCouplingsPanel coupling={coupling} />
    </div>
  );
}

/** Couplage temporel : graphe de réseau 3D des fichiers qui changent ensemble. */
export function CouplingPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Couplage</h1>
      <AnalysisGate>{(data) => <CouplingScene coupling={data.coupling} />}</AnalysisGate>
    </div>
  );
}