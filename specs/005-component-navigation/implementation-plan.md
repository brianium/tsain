# Component Navigation - Implementation Plan

## Overview

Add prev/next navigation to single component view.

## Prerequisites

- [x] Single component view working
- [x] Library state available

## Phase 1: Navigation Logic

- [x] Implement component-neighbors function
- [x] Sort components alphabetically
- [x] Calculate prev/next with wrap-around

## Phase 2: Navigation UI

- [x] Add prev button to component view
- [x] Add next button to component view
- [x] Show destination component name
- [x] Hide when only one component

## Phase 3: Routes

- [x] Navigation uses existing view routes
- [x] Broadcasts to all clients

## Phase 4: CSS

- [x] Style navigation buttons
- [x] Responsive layout

## Phase 5: Keyboard (Optional)

- [ ] Arrow key handlers
- [ ] Window-level event listener

## Completed

Core navigation complete. Keyboard shortcuts deferred as optional enhancement.
