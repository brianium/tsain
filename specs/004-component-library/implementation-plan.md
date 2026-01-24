# Component Library - Implementation Plan

## Overview

Add persistent component storage with browsable library UI.

## Prerequisites

- [x] Sandbox SSE working
- [x] REPL dispatch working

## Phase 1: State Management

- [x] Create sandbox/state.clj
- [x] Define state atom structure
- [x] Implement load-library from EDN
- [x] Implement save-library! to EDN

## Phase 2: Sandestin Registry

- [x] Create sandbox/registry.clj
- [x] Implement ::preview effect
- [x] Implement ::preview-append effect
- [x] Implement ::preview-clear effect
- [x] Implement ::commit effect
- [x] Implement ::uncommit effect
- [x] Implement ::show effect
- [x] Implement ::show-gallery effect
- [x] Implement ::show-preview effect

## Phase 3: Browser UI

- [x] Create nav bar with view switching
- [x] Create preview view
- [x] Create gallery grid view
- [x] Create single component view
- [x] Add uncommitted badge
- [x] Add commit button

## Phase 4: Routes

- [x] Add view switching routes
- [x] Add action routes (commit, clear)
- [x] Add deep link routes

## Phase 5: REPL API

- [x] Add wrapper functions to dev.clj
- [x] Document in CLAUDE.md

## Completed

All phases complete. Library persists to components.edn.
