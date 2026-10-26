import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ContextualTooltip } from './ContextualTooltip';

describe('ContextualTooltip', () => {
  it('renders trigger button with correct aria attributes', () => {
    render(
      <ContextualTooltip
        title="Header Matcher"
        description="Matches against incoming HTTP headers"
      />
    );

    const button = screen.getByRole('button', { name: /Help: Header Matcher/i });
    expect(button).toBeDefined();
    expect(button.getAttribute('aria-expanded')).toBe('false');
  });

  it('toggles popover visibility on click and updates aria-expanded', () => {
    render(
      <ContextualTooltip
        title="Query Params Matcher"
        description="Matches URL query string parameters"
        syntaxExample="page=1&size=20"
      />
    );

    const button = screen.getByRole('button', { name: /Help: Query Params Matcher/i });
    fireEvent.click(button);

    expect(button.getAttribute('aria-expanded')).toBe('true');
    const tooltip = screen.getByRole('tooltip');
    expect(tooltip).toBeDefined();
    expect(screen.getByText('Query Params Matcher')).toBeDefined();
    expect(screen.getByText('Matches URL query string parameters')).toBeDefined();
    expect(screen.getByText('page=1&size=20')).toBeDefined();

    // Click again to close
    fireEvent.click(button);
    expect(screen.queryByRole('tooltip')).toBeNull();
  });

  it('closes on Escape key press', () => {
    render(
      <ContextualTooltip
        title="Body Matcher"
        description="Inspects request body"
      />
    );

    const button = screen.getByRole('button', { name: /Help: Body Matcher/i });
    fireEvent.click(button);
    expect(screen.getByRole('tooltip')).toBeDefined();

    fireEvent.keyDown(button, { key: 'Escape' });
    expect(screen.queryByRole('tooltip')).toBeNull();
  });

  it('renders docLink when provided', () => {
    render(
      <ContextualTooltip
        title="XPath Guide"
        description="XPath expressions for XML body matching"
        docLink="https://example.com/docs/xpath"
      />
    );

    const button = screen.getByRole('button', { name: /Help: XPath Guide/i });
    fireEvent.click(button);

    const link = screen.getByRole('link', { name: /Documentation/i });
    expect(link.getAttribute('href')).toBe('https://example.com/docs/xpath');
  });
});
