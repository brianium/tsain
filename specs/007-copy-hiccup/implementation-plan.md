# Copy Hiccup to Clipboard - Implementation Plan

## Overview

Add copy button that fetches pretty-printed hiccup and copies to clipboard.

## Prerequisites

- [x] Component view working
- [x] Multiple examples working

## Phase 1: Copy Route

- [x] Add /sandbox/copy/:name endpoint
- [x] Accept ?idx query param for example index
- [x] Return pretty-printed hiccup as text/plain
- [x] Handle missing components gracefully

## Phase 2: Button UI

- [x] Add Copy button to component-actions
- [x] Wire up fetch + clipboard API
- [x] Include current example index

## Phase 3: Feedback

- [x] Change button text to "Copied!" on success
- [x] Reset after 1.5 seconds
- [x] Style button appropriately

## Phase 4: Testing

- [x] Test single-hiccup components (legacy)
- [x] Test multi-example components
- [x] Test missing component handling

## Completed

All phases complete. Copy button working for all component formats.
