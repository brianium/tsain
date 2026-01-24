# CSS Hot Reload - Implementation Plan

## Overview

Implement file watcher that broadcasts CSS reload to all sandbox connections.

## Prerequisites

- [x] Sandbox SSE connection working
- [x] sfere broadcast working

## Phase 1: Watcher Core

- [x] Add beholder dependency
- [x] Create sandbox/watcher.clj
- [x] Implement change-handler with extension extraction
- [x] Implement ext-fx map pattern

## Phase 2: CSS Reload

- [x] Create reload-css! function
- [x] Broadcast via sfere to [:* [:sandbox :*]]
- [x] Execute cache-busting script in browsers

## Phase 3: System Integration

- [x] Start watcher in start-system
- [x] Stop watcher in stop-system
- [x] Configure watched paths

## Phase 4: Testing

- [x] Verify CSS changes trigger reload
- [x] Verify multiple browsers all update
- [x] Verify watcher restarts with system

## Completed

All phases complete. CSS hot reload operational.
