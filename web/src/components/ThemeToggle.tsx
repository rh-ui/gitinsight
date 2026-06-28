import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../theme/ThemeProvider';

/** Bouton de bascule clair/sombre. Icône = action à venir (soleil en sombre). */
export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={isDark ? 'Passer en thème clair' : 'Passer en thème sombre'}
      title={isDark ? 'Thème clair' : 'Thème sombre'}
      className="rounded-md border border-border p-2 text-muted transition hover:border-primary hover:text-primary"
    >
      {isDark ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}
