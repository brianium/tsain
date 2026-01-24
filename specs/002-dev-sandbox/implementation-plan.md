# Development Sandbox - Implementation Plan

## Overview

Set up REPL-driven component sandbox with real-time browser updates.

## Prerequisites

- [x] deps.edn with sandestin ecosystem dependencies
- [x] clj-reload configured

## Phase 1: Project Structure

- [x] Create user.clj with clj-reload init
- [x] Create dev.clj with lifecycle commands
- [x] Create sandbox/system.clj for *system* var
- [x] Create sandbox/app.clj with router
- [x] Create sandbox/views.clj with page template

## Phase 2: SSE Connection

- [x] Implement /sse endpoint
- [x] Store connections with sfere
- [x] Broadcast pattern [:* [:sandbox :*]]

## Phase 3: REPL Dispatch

- [x] Add dispatch function to dev namespace
- [x] Create preview! helper
- [x] Create preview-append! helper
- [x] Create preview-clear! helper

## Phase 4: Multi-Device Sync

- [x] Unique connection IDs per tab
- [x] All tabs receive broadcasts
- [x] Test with multiple devices

## Phase 5: Documentation

- [x] Update CLAUDE.md with usage
- [x] Add REPL workflow examples

## Completed

All phases complete. Sandbox operational at localhost:3000.
