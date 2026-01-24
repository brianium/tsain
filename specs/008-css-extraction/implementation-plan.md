# CSS Extraction Workflow - Implementation Plan

## Overview

Migrate all components to use CSS classes and establish extraction workflow.

## Prerequisites

- [x] styles.css exists and hot-reloads
- [x] Components committed to library

## Phase 1: Workflow Updates

- [x] Update component-iterate skill with CSS extraction step
- [x] Add styling conventions to CLAUDE.md
- [x] Document BEM-like naming

## Phase 2: CSS Foundation

- [x] Set up CSS custom properties
- [x] Define theme variables (--bg-primary, --accent-cyan, etc.)
- [x] Add .theme-light variant
- [x] Add section headers for organization

## Phase 3: Component Refactoring

Extract CSS and update hiccup for each component:
- [x] game-card
- [x] combat-log
- [x] card-type-badges
- [x] player-hud
- [x] action-buttons
- [x] resource-display

## Phase 4: Verification

- [x] All components render correctly (Dark)
- [x] All components render correctly (Light)
- [x] Copied hiccup is clean
- [x] CSS hot-reload works

## Completed

All phases complete. Components use CSS classes, styles.css organized.
