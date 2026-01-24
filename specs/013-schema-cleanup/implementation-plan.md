# Schema Cleanup - Implementation Plan

## Overview

Fix namespace and use canonical twk schemas.

## Prerequisites

- [x] twk.schema available
- [x] malli.util available

## Phase 1: Update tsain.clj

- [x] Add require for ascolais.twk.schema
- [x] Add require for malli.util
- [x] Replace HiccupSchema with twk schema + gen hints
- [x] Replace SignalMapSchema with twk schema + gen hints
- [x] Change ::s/state to ::state

## Phase 2: Update Consumers

- [x] Update sandbox/app.clj to use ::tsain/state
- [x] Update tsain.routes to use ::tsain/state
- [x] Verify no other references to ::s/state

## Phase 3: Update Documentation

- [x] Update README examples
- [x] Update CLAUDE.md references

## Phase 4: Testing

- [x] Run all tests
- [x] Verify schema validation works
- [x] Verify s/sample generates useful output

## Completed

All phases complete. Clean namespacing and canonical schemas.
