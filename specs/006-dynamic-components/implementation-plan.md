# Dynamic Component Iteration - Implementation Plan

## Overview

Add support for multiple component configurations and REPL signal patching.

## Prerequisites

- [x] Component library working
- [x] Chassis aliases set up

## Phase 1: Examples Structure

- [x] Update components.edn schema for :examples vector
- [x] Each example has :label and :hiccup
- [x] Update state loading/saving

## Phase 2: Config Selector UI

- [x] Add dropdown to component view
- [x] Show when >1 example
- [x] Track selected example index in view state
- [x] Render selected example's hiccup

## Phase 3: patch-signals! Effect

- [x] Add ::patch-signals to registry
- [x] Broadcast via sfere
- [x] Add patch-signals! to dev.clj

## Phase 4: Commit Signature

- [x] Update commit! to accept :examples in opts
- [x] Support simple (current preview) and full options
- [x] Document in CLAUDE.md

## Phase 5: Skill Update

- [x] Update component-iterate skill
- [x] Document Datastar workflow

## Completed

All phases complete. Multiple examples and signal patching working.
