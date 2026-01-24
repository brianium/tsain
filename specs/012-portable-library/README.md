---
title: "Portable Library"
status: completed
date: 2024-01-15
priority: 50
---

# Portable Library

## Overview

Tsain provides excellent component development tooling, but it's coupled to this project. Routes are hardcoded, views assume project structure, skills have no distribution mechanism.

Solution: Extract tsain as a distributable library that any sandestin/twk project can add via deps.edn.

## Goals

1. Library exports - Registry factory + route factory (configured via tsain.edn)
2. Sample files - CLAUDE.md, tsain.edn, starter ui.clj for consumers to copy
3. Skill - component-iterate teaches Claude the workflow via discovery

## Non-Goals

- npm/clojars publishing (git dep sufficient)
- Zero-config magic

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Distribution | Git dependency | Simple, version-controlled |
| Configuration | tsain.edn | Explicit, documented |
| Routes | Factory function | Composable with consumer routes |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Extract routes
- [x] Phase 2: Extract views
- [x] Phase 3: Package assets
- [x] Phase 4: Create samples
- [x] Phase 5: Update skill
