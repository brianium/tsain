# Ascolais Project Template - Implementation Plan

## Overview

Create deps-new template at /Users/brian/projects/ascolais.

## Prerequisites

- [x] Tsain library distributable (spec 012)
- [x] Pattern established for integrant + sandestin

## Phase 1: Template Structure

- [x] Create ascolais repo
- [x] Set up deps-new template structure
- [x] Configure template.edn

## Phase 2: Core Files

- [x] deps.edn with all dependencies
- [x] .gitignore
- [x] README.md

## Phase 3: Integrant Configuration

- [x] src/clj/config.clj with app components
- [x] dev/src/clj/dev/config.clj extending app
- [x] Initializer function pattern
- [x] dev.clj with lifecycle commands

## Phase 4: Effect Organization

- [x] fx/ directory pattern
- [x] Example effect registry
- [x] Dispatch composition

## Phase 5: Database

- [x] docker-compose.yml for PostgreSQL
- [x] HikariCP datasource
- [x] Ragtime migration setup
- [x] Manse integration

## Phase 6: Tsain Integration

- [x] tsain.edn configuration
- [x] sandbox/ui.clj starter
- [x] Dev routes include tsain

## Phase 7: Claude Integration

- [x] Comprehensive CLAUDE.md
- [x] .claude/settings.json with hooks
- [x] clojure-eval skill
- [x] component-iterate skill

## Phase 8: Testing

- [x] Template generates without errors
- [x] Generated project compiles
- [x] Dev workflow works
- [x] Database migrations work

## Completed

All phases complete. Template at github.com/brianium/ascolais.
