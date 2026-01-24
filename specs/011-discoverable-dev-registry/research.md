# Discoverable Dev Registry - Research

## Problem Statement

Two parallel APIs: preview!/commit! functions and ::sandbox/* effects. Claude must learn both. Documentation split between docstrings and effect descriptions. Schemas are minimal (:any).

## Requirements

### Functional Requirements

1. Single API via sandestin registry
2. Rich schemas with :gen/elements for sampling
3. Descriptions explain usage context
4. tsain.edn for shared configuration

### Non-Functional Requirements

- Discoverability: API learnable via REPL
- Consistency: Same pattern as other ascolais libs

## Options Considered

### Option A: Keep both APIs

**Description:** Maintain helper functions alongside effects

**Pros:**
- No breaking changes
- Convenience functions

**Cons:**
- Duplicate documentation
- Confusing which to use
- Harder to maintain

### Option B: Registry-only API

**Description:** Remove helper functions, use effects only

**Pros:**
- Single source of truth
- Self-documenting via schemas
- Consistent with ecosystem

**Cons:**
- Slightly more verbose calls

## Recommendation

**Option B** - Registry-only. The verbosity cost is minimal, and the discoverability/consistency benefits are significant.

## References

- sandestin discovery API
- twk registry pattern
- sfere registry pattern
