# Barrel CSS Watcher Support - Research

## Problem Statement

The sandbox watcher detects CSS file changes and broadcasts a script that cache-busts `<link rel="stylesheet">` elements:

```javascript
document.querySelectorAll('link[rel=stylesheet]').forEach(l =>
  l.href = l.href.split('?')[0] + '?v=' + Date.now())
```

When `styles.css` uses `@import` statements:

```css
@import "./components/cards.css";
@import "./components/controls.css";
```

The browser re-fetches `styles.css?v=123`, parses it, and sees the `@import` URLs unchanged. The browser may serve these imported files from cache, meaning edits to `components/cards.css` don't appear even though the watcher detected the change.

## Requirements

### Functional Requirements

1. Editing any CSS file in the watched paths must trigger visible updates in connected browsers
2. Works with arbitrary `@import` nesting depth
3. No change to the existing `reload-css!` broadcast mechanism

### Non-Functional Requirements

- Performance: No noticeable latency impact
- Simplicity: Minimal code changes
- Dev-only: Solution doesn't need to consider production caching

## Options Considered

### Option A: Cache-Control Headers

**Description:** Add Ring middleware that sets `Cache-Control: no-cache, no-store, must-revalidate` for all CSS responses in development.

**Pros:**
- Simple one-liner middleware
- Works with any nesting depth automatically
- No changes to watcher logic
- Browser handles cascaded re-fetching

**Cons:**
- All CSS requests hit the server (fine for dev)
- Applies to all CSS, not just imported files (acceptable)

### Option B: Rewrite @import URLs

**Description:** Parse CSS files and rewrite `@import` URLs to include cache-busting query params.

**Pros:**
- Only busts cache on actual changes

**Cons:**
- Complex: need CSS parsing
- Must handle nested imports recursively
- Error-prone with relative paths
- Significant implementation effort

### Option C: Inline All Imports

**Description:** Pre-process CSS to inline all `@import` content into a single file.

**Pros:**
- Single file = single cache bust

**Cons:**
- Requires CSS bundling toolchain
- Development workflow becomes more complex
- Loses benefit of separate files

### Option D: Full Page Reload

**Description:** Use `window.location.reload()` instead of cache-busting.

**Pros:**
- Guaranteed to work

**Cons:**
- Loses page state and Datastar signals
- Poor developer experience
- Heavy-handed

## Recommendation

**Option A: Cache-Control Headers** is the clear winner.

It's a ~5 line middleware addition that completely solves the problem by letting the browser's natural CSS loading cascade work correctly. When `styles.css` is re-fetched, the browser re-requests all `@import`ed files because they're not cached.

The "downside" of always re-fetching CSS is actually desirable in development - it ensures you always see the latest styles.

## Open Questions

- [x] Does Ring's `wrap-resource` respect response headers added by downstream middleware? (Yes, middleware after wrap-resource can modify responses)

## References

- Current watcher: `dev/src/clj/sandbox/watcher.clj`
- Current app setup: `dev/src/clj/sandbox/app.clj`
- MDN Cache-Control: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
