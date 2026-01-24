# Ascolais Project Template - Research

## Problem Statement

Starting a new sandestin-based project requires significant setup: deps.edn, integrant configuration, effect registries, database setup, tsain integration, CLAUDE.md. A template standardizes and accelerates this.

## Requirements

### Functional Requirements

1. Single command creates working project
2. Dev server starts without configuration
3. Database migrations ready
4. Component sandbox available
5. Claude-ready documentation

### Non-Functional Requirements

- Production-ready structure
- Follow ecosystem conventions

## Options Considered

### Option A: Minimal template

**Description:** Only essential files, consumer adds features

**Pros:**
- Smaller template
- No unused code

**Cons:**
- More manual setup
- Inconsistent conventions

### Option B: Full-featured template

**Description:** Complete setup including database, tsain, etc.

**Pros:**
- Batteries included
- Consistent conventions
- Production-ready structure

**Cons:**
- May include unused features
- Larger initial codebase

## Recommendation

**Option B** - Full-featured template. The sandestin ecosystem benefits from established patterns. Unused features are easy to remove, but establishing patterns from scratch is hard.

## References

- deps-new documentation
- clojure-claude-code template structure
