import React from 'react';

import AppRouter from './components/AppRouter';
import { PageSizeProvider } from './components/PageSizeContext';

import './App.css';

function App() {
  return (
    <PageSizeProvider>
      <AppRouter />
    </PageSizeProvider>
  );
}

export default App;
