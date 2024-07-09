import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

test('renders remindme text', () => {
  render(<App />);
  
  const remindMeElement = screen.getByText(/RemindMe/i);
  expect(remindMeElement).toBeInTheDocument();
});
