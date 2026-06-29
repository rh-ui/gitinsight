import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AnalysisProvider } from './analysis/AnalysisProvider';
import { AppLayout } from './components/layout/AppLayout';
import { OverviewPage } from './pages/OverviewPage';
import { AuthorsPage } from './pages/AuthorsPage';
import { HotspotsPage } from './pages/HotspotsPage';
import { BusFactorPage } from './pages/BusFactorPage';
import { CouplingPage } from './pages/CouplingPage';

function App() {
  return (
    <BrowserRouter>
      <AnalysisProvider>
        <Routes>
          {/* Route de layout : la coquille englobe toutes les pages. */}
          <Route element={<AppLayout />}>
            {/* Racine -> redirige vers la vue d'ensemble. */}
            <Route index element={<Navigate to="/overview" replace />} />
            <Route path="overview" element={<OverviewPage />} />
            <Route path="authors" element={<AuthorsPage />} />
            <Route path="hotspots" element={<HotspotsPage />} />
            <Route path="bus-factor" element={<BusFactorPage />} />
            <Route path="coupling" element={<CouplingPage />} />
            {/* Filet : toute URL inconnue retombe sur la vue d'ensemble. */}
            <Route path="*" element={<Navigate to="/overview" replace />} />
          </Route>
        </Routes>
      </AnalysisProvider>
    </BrowserRouter>
  );
}

export default App;
