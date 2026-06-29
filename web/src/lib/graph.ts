import type { FileCoupling } from '../types/analysis';
import { truncatePath } from './format';

export interface GraphNode {
  id: string; // chemin complet (clé unique)
  label: string; // chemin tronqué (affichage)
  degree: number; // nb de paires où le fichier apparaît
}

export interface GraphLink {
  source: string; // id (chemin) — ForceGraph le résout en objet nœud
  target: string;
  value: number; // couplingScore (→ épaisseur d'arête)
  coChanges: number;
}

export interface CouplingGraph {
  nodes: GraphNode[];
  links: GraphLink[];
}

/** Transforme les paires de couplage en graphe nœuds/arêtes (helper pur). */
export function toCouplingGraph(coupling: FileCoupling[]): CouplingGraph {
  const degree = new Map<string, number>();
  const ids = new Set<string>();

  for (const c of coupling) {
    ids.add(c.fileA);
    ids.add(c.fileB);
    degree.set(c.fileA, (degree.get(c.fileA) ?? 0) + 1);
    degree.set(c.fileB, (degree.get(c.fileB) ?? 0) + 1);
  }

  const nodes: GraphNode[] = [...ids].map((id) => ({
    id,
    label: truncatePath(id, 28),
    degree: degree.get(id) ?? 0,
  }));

  const links: GraphLink[] = coupling.map((c) => ({
    source: c.fileA,
    target: c.fileB,
    value: c.couplingScore,
    coChanges: c.coChanges,
  }));

  return { nodes, links };
}