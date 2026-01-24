---
title: "Discoverable Dev Registry"
status: completed
date: 2024-01-15
priority: 50
---

# Discoverable Dev Registry

## Overview

The dev namespace has two parallel APIs: ad-hoc helper functions and sandestin effects. This creates friction - Claude must learn both, documentation is scattered, and tsain as a reusable library is hindered.

Solution: Make the sandestin registry the only API. Discover the sandbox API via sandestin's REPL discovery functions.

## Goals

1. Registry-first - All functionality is effects/actions with rich schemas
2. Self-documenting - Descriptions explain what, when, and how
3. Sample-driven - (s/sample dispatch ::tsain/effect) generates usable invocations
4. Single access point - dispatch function in dev namespace
5. Shared config - tsain.edn serves both library runtime and Claude/skills

## Non-Goals

- Backward compatibility with helper functions
- Complex schema validation

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Effect namespace | ::tsain/ | Consistent with other ascolais libraries |
| Config file | tsain.edn | Shared by library and skills |
| Discovery | sandestin describe/sample/grep | Standard ecosystem tooling |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Create tsain.edn
- [x] Phase 2: Registry factory
- [x] Phase 3: Rich schemas
- [x] Phase 4: Update dev.clj
- [x] Phase 5: Update skill/docs
