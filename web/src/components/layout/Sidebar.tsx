import { NavLink } from 'react-router-dom';
import { Flame, LayoutDashboard, Users, type LucideIcon } from 'lucide-react';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

const NAV: NavItem[] = [
  { to: '/overview', label: "Vue d'ensemble", icon: LayoutDashboard },
  { to: '/authors', label: 'Auteurs', icon: Users },
  { to: '/hotspots', label: 'Fichiers à risque', icon: Flame },
];

/** Barre latérale de navigation entre les vues de l'analyse. */
export function Sidebar() {
  return (
    <aside className="flex w-60 shrink-0 flex-col border-r border-border bg-surface">
      <div className="flex items-center gap-2 border-b border-border px-5 py-4">
        <img src="/logo.png" alt="GitInsight" className="h-7 w-7 object-contain" />
        <span className="text-lg font-bold tracking-tigh">
          <span className=" text-primary">Git</span>Insight
        </span>
      </div>

      <nav className="flex flex-col gap-1 p-3">
        {NAV.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition ${
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted hover:bg-surface-2 hover:text-foreground'
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
