# Discoverable Dev Registry - Implementation Plan

## Overview

Consolidate dev API into sandestin registry with rich schemas.

## Prerequisites

- [x] Effects working in sandbox/registry.clj
- [x] Helper functions in dev.clj

## Phase 1: Configuration

- [x] Create tsain.edn at project root
- [x] Document all config keys
- [x] Set sensible defaults

## Phase 2: Registry Factory

- [x] Create src/clj/ascolais/tsain.clj
- [x] Move effects from sandbox/registry.clj
- [x] Change namespace to ::tsain/
- [x] Add (tsain/registry) factory

## Phase 3: Rich Schemas

- [x] Add :description to all schemas
- [x] Add :gen/elements for sampling
- [x] Multi-line descriptions (what/when/how)

## Phase 4: Update dev.clj

- [x] Use (tsain/registry) in dispatch
- [x] Export describe, sample, grep aliases
- [x] Remove helper functions

## Phase 5: Update Docs

- [x] Update component-iterate skill
- [x] Update CLAUDE.md with discovery workflow
- [x] Document tsain.edn usage

## Phase 6: Testing

- [x] Unit tests for registry factory
- [x] Schema validation tests
- [x] s/sample generates useful output

## Completed

All phases complete. Registry-first API operational.
