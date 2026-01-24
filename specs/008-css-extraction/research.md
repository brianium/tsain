# CSS Extraction Workflow - Research

## Problem Statement

Components in components.edn have massive inline styles (200+ lines for some). Copied hiccup is unmanageable. styles.css is empty.

## Requirements

### Functional Requirements

1. Committed components use CSS classes
2. styles.css contains organized component styles
3. Theme variants work via CSS

### Non-Functional Requirements

- Maintainability: Styles in one place
- Portability: Copied hiccup works elsewhere

## Options Considered

### Option A: Inline styles acceptable

**Description:** Keep allowing inline styles in committed components

**Pros:**
- No migration work
- Simpler workflow

**Cons:**
- Bloated copied hiccup
- Duplicate styles for variants
- Hard to maintain

### Option B: CSS classes required for commit

**Description:** Extract styles to CSS before committing

**Pros:**
- Clean copied hiccup
- Maintainable styles
- Theme variants via CSS

**Cons:**
- Extra extraction step
- Migration needed

## Recommendation

**Option B** - CSS classes required. The initial migration effort pays off in maintainability and clean exports.

## References

- BEM naming convention
- CSS custom properties
