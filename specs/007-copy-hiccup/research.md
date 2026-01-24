# Copy Hiccup to Clipboard - Research

## Problem Statement

When a component is ready to use, developers need to copy the hiccup to their codebase. Currently requires viewing source or using REPL.

## Requirements

### Functional Requirements

1. Copy button on component view
2. Copies currently selected variant
3. Pretty-printed output
4. Visual confirmation

### Non-Functional Requirements

- Usability: One click copy
- Format: Readable, properly indented

## Options Considered

### Option A: Client-side from data attribute

**Description:** Encode hiccup in HTML data attribute, copy directly

**Pros:**
- No server round-trip
- Instant copy

**Cons:**
- Raw format (not pretty-printed)
- Larger HTML payload

### Option B: Server-side fetch

**Description:** Fetch from endpoint, then clipboard API

**Pros:**
- Pretty-printed output
- Accurate to stored data

**Cons:**
- Small latency

## Recommendation

**Option B** - Server-side fetch. The latency is negligible, and pretty-printed output is much more valuable for developer experience.

## References

- Clipboard API documentation
- Storybook copy features
