# Schema Cleanup - Research

## Problem Statement

1. Registry uses ::s/state (:ascolais.sandestin/state) but this is tsain-specific state
2. Inline HiccupSchema and SignalMapSchema duplicate what twk.schema provides

## Requirements

### Functional Requirements

1. State accessible via ::tsain/state
2. Schemas use twk canonical definitions
3. s/sample still generates useful examples

### Non-Functional Requirements

- Consistency with ecosystem
- No breaking changes to consumers

## Options Considered

### Option A: Keep current approach

**Description:** Keep inline schemas and ::s/state

**Pros:**
- No changes needed

**Cons:**
- Namespace pollution
- Duplicate definitions

### Option B: Use twk schemas + correct namespace

**Description:** Import twk schemas, fix state namespace

**Pros:**
- Consistent with ecosystem
- DRY schemas
- Correct namespacing

**Cons:**
- Migration required

## Recommendation

**Option B** - Use ecosystem schemas and correct namespace. Small migration, big consistency win.

## References

- ascolais.twk.schema namespace
- malli.util/update-properties
