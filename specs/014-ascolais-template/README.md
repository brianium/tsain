---
title: "Ascolais Project Template"
status: completed
date: 2024-01-15
priority: 50
---

# Ascolais Project Template

## Overview

Create a deps-new template for scaffolding web applications powered by the sandestin effect ecosystem. The template produces projects optimized for Claude-assisted development with pre-configured tooling, conventions, and comprehensive documentation.

## Goals

1. Quick scaffold - `clj -Tnew create :template brianium/ascolais` produces working app
2. Effect-driven architecture - Sandestin effects as primary abstraction
3. Database-ready - Manse integration with PostgreSQL and ragtime
4. Datastar frontend - TWK for SSE reactivity, kaiin for route generation
5. Component sandbox - Tsain integration for REPL-driven UI development
6. Claude-optimized - Comprehensive CLAUDE.md, skills, conventions
7. Production path - Dev extends production

## Non-Goals

- Multi-database support
- Alternative frontend frameworks

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| System management | Integrant | Familiar, composable |
| Database | PostgreSQL | Common, well-supported |
| Template location | Separate repo | Independent versioning |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Template structure
- [x] Phase 2: Core files
- [x] Phase 3: Integrant config
- [x] Phase 4: Database setup
- [x] Phase 5: Tsain integration
- [x] Phase 6: Documentation
