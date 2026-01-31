# Arity Unpleasantness - Implementation Plan

## Overview

Fix optional argument handling in sandestin effect dispatch so handlers receive `nil` for omitted optional args.

## Prerequisites

- [ ] Verify the bug: fresh REPL, call `::replace-css` without options
- [ ] Confirm sandestin source: check how dispatch invokes handlers
- [ ] Identify all affected effects in tsain (any with optional trailing args)

## Phase 1: Reproduce and Diagnose

- [ ] Clone/open sandestin project
- [ ] Write minimal reproduction test:
  ```clojure
  (deftest optional-arg-dispatch-test
    (let [reg {::s/effects
               {::test-effect
                {::s/schema [:tuple [:= ::test-effect] :string [:map {:optional true}]]
                 ::s/handler (fn [_ctx _sys arg & [opts]]
                               {:arg arg :opts opts})}}}
          dispatch (s/create-dispatch [reg])]
      ;; This should work
      (is (= {:arg "hello" :opts nil}
             (dispatch [[::test-effect "hello"]])))))
  ```
- [ ] Identify exact failure point in sandestin dispatch

## Phase 2: Fix in Sandestin

- [ ] Locate dispatch implementation (likely `create-dispatch` or related)
- [ ] Modify to inspect schema for optional elements
- [ ] Pad args vector with `nil` for missing optional elements
- [ ] Ensure backward compatibility (existing effects still work)
- [ ] Add tests for optional arg scenarios

## Phase 3: Verify in Tsain

- [ ] Update sandestin dependency in tsain
- [ ] Test `::replace-css` without options map
- [ ] Test `::split-css` without options map
- [ ] Verify no regressions in other effects
- [ ] Update bug-report.md with resolution

## Phase 4: Cleanup

- [ ] Remove workaround code if any was added
- [ ] Update documentation if needed
- [ ] Tag sandestin release
- [ ] Tag tsain release with updated dep

## Alternative: If Sandestin Fix Not Feasible

If fixing sandestin is blocked, implement workaround in tsain:

- [ ] Add `normalize-optional-args` helper
- [ ] Wrap affected effect handlers
- [ ] Document the workaround pattern
- [ ] File issue in sandestin for future fix

## Rollout Plan

1. Fix and release sandestin
2. Update tsain dependency
3. Verify in callit project
4. Release tsain

## Rollback Plan

If issues arise:
1. Revert sandestin to previous version
2. Add explicit empty map to effect calls as workaround
