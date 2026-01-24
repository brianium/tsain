# CSS Hot Reload - Research

## Problem Statement

Editing CSS requires manual browser refresh. Need automatic reload when CSS files change, using the existing SSE connection.

## Requirements

### Functional Requirements

1. Watch CSS files for changes
2. Broadcast reload to all connected browsers
3. Reload without full page refresh
4. Extensible to other file types

### Non-Functional Requirements

- Latency: <500ms from save to visual update
- Reliability: Watcher survives system restarts

## Options Considered

### Option A: Full page reload on change

**Description:** Execute `window.location.reload()` on any file change

**Pros:**
- Simple implementation
- Guaranteed fresh state

**Cons:**
- Loses Datastar signal state
- Slower feedback loop

### Option B: CSS-specific cache busting

**Description:** Update stylesheet href with cache-busting query param

**Pros:**
- Preserves page state
- Fast visual update
- CSS-specific optimization

**Cons:**
- Doesn't work for JS changes

## Recommendation

**Option B** - CSS cache busting for CSS files, with ext-fx map allowing different handlers per extension. Can add full reload for JS files later.

## References

- beholder library documentation
- CSS cache-busting techniques
