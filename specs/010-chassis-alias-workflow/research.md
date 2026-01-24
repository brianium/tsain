# Chassis Alias Workflow - Research

## Problem Statement

components.edn has 40+ line entries with verbose hiccup. Dark/Light variants duplicate 95% of markup. Copied hiccup is unwieldy.

## Requirements

### Functional Requirements

1. Component structure defined once
2. Examples use aliases with config
3. Copy button returns alias form

### Non-Functional Requirements

- Portability: Copied hiccup works in other projects
- Maintainability: Structure changes in one place

## Options Considered

### Option A: Keep verbose hiccup

**Description:** Continue storing full hiccup in components.edn

**Pros:**
- No migration
- What you see is what you store

**Cons:**
- Bloated files
- Duplicate markup
- Hard to copy

### Option B: Chassis aliases + lean configs

**Description:** Structure in defmethod, configs in components.edn

**Pros:**
- DRY structure
- Lean exports
- Self-documenting props

**Cons:**
- Requires alias-first workflow
- Initial migration

## Recommendation

**Option B** - Chassis aliases. The namespaced keyword convention makes props self-documenting, and lean exports are much more useful.

## References

- Chassis alias documentation
- Reagent/Re-frame component patterns
