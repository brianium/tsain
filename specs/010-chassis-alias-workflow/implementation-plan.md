# Chassis Alias Workflow - Implementation Plan

## Overview

Migrate components to use chassis aliases with namespaced config props.

## Prerequisites

- [x] Chassis working in sandbox
- [x] Components in library

## Phase 1: Define Pattern

- [x] Establish :component-name/prop convention
- [x] Document attrs pass-through for Datastar
- [x] Create alias template

## Phase 2: Migrate Components

Create aliases in sandbox/ui.clj and update components.edn:
- [x] game-card
- [x] combat-log
- [x] card-type-badges
- [x] player-hud
- [x] action-buttons
- [x] resource-display

## Phase 3: Ensure Copy Works

- [x] Verify copy button returns alias form
- [x] get-example-hiccup reads from components.edn

## Phase 4: Update Docs

- [x] Update component-iterate skill
- [x] Add chassis alias conventions to CLAUDE.md
- [x] Document file locations

## Phase 5: Require Pattern

- [x] sandbox.views requires sandbox.ui
- [x] Aliases registered before render

## Completed

All phases complete. Components use lean alias format.
