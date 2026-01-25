# Barrel CSS Watcher Support - Implementation Plan

## Overview

Add middleware to disable browser caching for CSS files in development, ensuring `@import`ed stylesheets are always re-fetched when the parent stylesheet is reloaded.

## Prerequisites

- [x] Understand current watcher mechanism
- [x] Confirm middleware ordering with `wrap-resource`

## Phase 1: Add No-Cache Middleware

- [x] Add `wrap-no-cache-css` function to `sandbox/app.clj`
  ```clojure
  (defn- wrap-no-cache-css [handler]
    (fn [request]
      (let [response (handler request)]
        (if (and response
                 (some-> (:uri request) (str/ends-with? ".css")))
          (assoc-in response [:headers "Cache-Control"]
                    "no-cache, no-store, must-revalidate")
          response))))
  ```
- [x] Add `[clojure.string :as str]` require if not present
- [x] Insert middleware after `wrap-resource` in `create-app`

## Phase 2: Testing & Verification

- [x] Manual test: Edit `dev/resources/public/styles.css` → verify reload
- [x] Manual test: Create `dev/resources/public/components/test.css`
- [x] Manual test: Add `@import "./components/test.css"` to styles.css
- [x] Manual test: Edit `test.css` → verify changes appear without full reload
- [x] Verify browser DevTools shows `Cache-Control: no-cache` on CSS responses (verified via curl)

## Phase 3: Cleanup

- [x] Remove test CSS file
- [x] Update CLAUDE.md if needed (document barrel import support) - already documented

## Rollout Plan

1. Make changes in dev branch
2. Run sandbox and manually verify
3. Commit with message: `fix(watcher): ensure CSS hot reload works with @import`

## Rollback Plan

If issues arise:
1. Remove `wrap-no-cache-css` middleware from `create-app`
2. The watcher will continue to work for top-level stylesheets
