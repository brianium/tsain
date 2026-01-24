# Portable Library - Research

## Problem Statement

Tsain is coupled to this project:
- Routes hardcoded in sandbox/app.clj
- Views assume project structure
- CLAUDE.md is monolithic
- Skills live in .claude/skills/ with no distribution
- Assets served ad-hoc

## Requirements

### Functional Requirements

1. Add tsain via deps.edn git dependency
2. Configure via tsain.edn
3. Compose routes with consumer's router
4. Copy sample files for quick setup

### Non-Functional Requirements

- Minimal coupling to consumer project
- Clear setup documentation

## Options Considered

### Option A: Template generator

**Description:** deps-new template creates full project

**Pros:**
- One command setup
- Complete scaffolding

**Cons:**
- Hard to update existing projects
- Opinionated structure

### Option B: Composable library + samples

**Description:** Library with factories, samples to copy

**Pros:**
- Works with existing projects
- Flexible integration
- Easy updates via git

**Cons:**
- Manual file copying

## Recommendation

**Option B** - Composable library. Better for incremental adoption and updates. Template approach (spec 014) can use this library internally.

## References

- twk library distribution
- sfere library distribution
