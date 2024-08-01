import React from 'react';
import { render, screen } from '@testing-library/react';
import { test, expect } from '@jest/globals'

import App from './App';


test('renders remindme text', () => {
  render(<App />);

  const remindMeElement = screen.getByText(/RemindMe/i);
  expect(remindMeElement).toBeInTheDocument();
});
