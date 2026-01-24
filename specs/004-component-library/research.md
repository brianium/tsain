# Component Library - Research

## Problem Statement

Components created during preview sessions are lost when the system restarts. Need persistence and browsing capability.

## Requirements

### Functional Requirements

1. Commit current preview to named library entry
2. Browse committed components in gallery
3. View single component in isolation
4. Delete components from library
5. Persist library across restarts

### Non-Functional Requirements

- Persistence: Survive REPL restarts
- URL stability: Deep links work

## Options Considered

### Option A: File-per-component

**Description:** Each component saved as separate file

**Pros:**
- Easy to version control individual components
- Can include related CSS

**Cons:**
- More file management
- Harder to browse programmatically

### Option B: Single EDN file

**Description:** All components in one components.edn file

**Pros:**
- Simple to load/save
- Easy to view entire library
- Human-readable

**Cons:**
- Large file with many components
- Merge conflicts possible

## Recommendation

**Option B** - Single EDN file for simplicity. Component libraries are typically small (<100 components). If scale becomes an issue, can migrate later.

## References

- Storybook component libraries
- Devcards (ClojureScript)
