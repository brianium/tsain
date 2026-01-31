# CLAUDE.md Documentation - Research

## Problem Statement

Claude needs comprehensive documentation to work effectively with the sandestin effect ecosystem. Without proper docs, Claude repeatedly asks about registry structure, dispatch patterns, and discovery APIs.

## Requirements

### Functional Requirements

1. Documentation must cover registry authoring (effects, actions, placeholders)
2. Must show dispatch creation and composition
3. Must teach REPL discovery workflow
4. Must include working code examples

### Non-Functional Requirements

- Accuracy: All examples must be runnable
- Completeness: Cover the essential workflow, not every edge case
- Size: ~800-1000 lines (dense but navigable)

## Options Considered

### Option A: Separate docs per library

**Description:** Create separate CLAUDE.md sections for each library (sandestin, twk, sfere)

**Pros:**
- Clear separation of concerns
- Easier to update individual libraries

**Cons:**
- Cross-cutting patterns harder to document
- More navigation required

### Option B: Unified workflow-oriented docs

**Description:** Single CLAUDE.md organized by workflow (registry authoring → dispatch → discovery → patterns)

**Pros:**
- Natural learning progression
- Cross-cutting patterns documented inline

**Cons:**
- Longer single file

## Recommendation

**Option B** - Unified workflow-oriented documentation. The sandestin ecosystem is designed to work together, and documenting it as a unified workflow helps Claude understand the big picture.

## References

- sandestin README
- twk API documentation
