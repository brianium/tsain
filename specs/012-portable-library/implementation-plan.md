# Portable Library - Implementation Plan

## Overview

Extract tsain as distributable library with route/registry factories.

## Prerequisites

- [x] Registry factory working (spec 011)
- [x] Effects using ::tsain/ namespace

## Phase 1: Extract Routes

- [x] Create src/clj/ascolais/tsain/routes.clj
- [x] Move route definitions from sandbox/app.clj
- [x] Parameterize with dispatch and state-atom
- [x] Return reitit route data

## Phase 2: Extract Views

- [x] Create src/clj/ascolais/tsain/views.clj
- [x] Move view functions from sandbox/views.clj
- [x] Parameterize with config for namespace resolution
- [x] Update routes to use extracted views

## Phase 3: Package Assets

- [x] Move sandbox.css to resources/tsain/sandbox.css
- [x] Update routes to serve from classpath
- [x] Document consumer asset serving

## Phase 4: Create Samples

- [x] Create sample/CLAUDE.md with ecosystem docs
- [x] Create sample/tsain.edn with documented config
- [x] Create sample/ui.clj starter template
- [x] Document copy-and-customize flow

## Phase 5: Update Skill

- [x] Make component-iterate skill generic
- [x] Discovery-first approach
- [x] tsain.edn config reading

## Phase 6: Dogfood

- [x] Tsain uses its own library exports
- [x] Verify full workflow works

## Completed

All phases complete. Library distributable via git.
