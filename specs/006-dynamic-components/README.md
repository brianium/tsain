---
title: "Dynamic Component Iteration"
status: completed
date: 2024-01-15
priority: 30
---

# Dynamic Component Iteration

## Overview

Extend the component iteration workflow to support stateful Datastar components. Chassis aliases define structure, Datastar attributes are just HTML attributes passed explicitly, and the sandbox UI provides controls to toggle between different attribute configurations.

Primary goal: Enable Claude to autonomously create, iterate on, and test dynamic Datastar-ready components with minimal friction.

## Goals

1. Aliases = structure + styling (Chassis aliases define what a component looks like)
2. Datastar attributes = just HTML attributes (no magic, explicit at usage site)
3. Multiple configs per component (store several example configurations)
4. Sandbox UI controls (toggle between configs in the browser)
5. REPL-driven testing (patch-signals! lets Claude test interactivity)

## Non-Goals

- Complex state management
- Full application simulation

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Datastar attrs | Pass-through HTML attrs | No abstraction layer |
| Multiple examples | :examples vector | Store different configurations |
| Signal testing | patch-signals! effect | REPL control of client state |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Chassis alias pattern
- [x] Phase 2: Examples structure
- [x] Phase 3: Config selector UI
- [x] Phase 4: patch-signals! effect
