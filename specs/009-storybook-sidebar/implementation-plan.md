# Storybook Sidebar - Implementation Plan

## Overview

Implement storybook-style collapsible sidebar for component navigation.

## Prerequisites

- [x] Component view working
- [x] Navigation working

## Phase 1: State Changes

- [x] Add sidebar-collapsed? to state atom
- [x] Add ::sidebar/toggle effect
- [x] Broadcast collapse state as signal

## Phase 2: Layout Structure

- [x] Create components-layout CSS grid
- [x] Sidebar in left column
- [x] Main content in right column
- [x] Handle collapsed state

## Phase 3: Sidebar UI

- [x] Sidebar header with title
- [x] Toggle button (« / »)
- [x] Component list
- [x] Active item highlight
- [x] Click handlers

## Phase 4: Routes

- [x] Update /sandbox/components to show sidebar
- [x] Add /sandbox/sidebar/toggle endpoint
- [x] Handle first-component selection

## Phase 5: CSS

- [x] Sidebar styles
- [x] Collapse animation
- [x] Mobile responsiveness

## Completed

All phases complete. Sidebar navigation working with collapse.
