# Replace CSS Bug Fix - Implementation Plan

## Overview

Fix the two bugs in `::tsain/replace-css`: arity error and append-instead-of-replace behavior.

## Prerequisites

- [x] Understand root causes (see [research.md](research.md))
- [x] Verify test environment works: `clj -X:test`

## Phase 1: Fix Arity Issue

The simplest fix - make the options map truly optional.

- [x] Update handler signature in `make-css-effects` to use variadic args (commit: already done)

  ```clojure
  ;; Handler already uses variadic args
  (fn [{:keys [dispatch]} _system selector-pattern new-content & [opts]]
    (let [opts (or opts {})]
      ...))
  ```

- [x] Test that 2-arg and 3-arg invocations both work

## Phase 2: Fix Removal Logic

Replace the string-matching approach with line-range based removal.

### Task 2.1: Fix find-rule-end-line infinite loop

- [x] Rewrote `find-rule-end-line` in `src/clj/ascolais/tsain/css.clj`

  The original nested-loop implementation had a bug where `recur` at end-of-line
  targeted the inner loop instead of the outer loop, causing `line-idx` to never
  advance (infinite loop).

  Fixed by using a single-loop approach that scans through a joined string:
  ```clojure
  (defn find-rule-end-line
    [css-string start-line]
    (let [lines (vec (str/split-lines css-string))
          text (str/join "\n" (subvec lines (dec start-line)))]
      (loop [i 0, brace-count 0, line-offset 0, ...]
        ;; Single loop tracking line-offset via newline characters
        ...)))
  ```

### Task 2.2: Fix find-selector-line regex

- [x] Removed overly strict `^` anchor from regex pattern

  The original pattern `(?m)^\s*<selector>\s*[,{]` required selectors at the
  start of a line, which failed for selectors inside `@media` blocks.

  Fixed by removing the anchor:
  ```clojure
  pattern (re-pattern (str escaped "\\s*[,{]"))
  ```

### Task 2.3: Handle edge cases

- [x] Handle rules at end of file (no trailing newline)
- [x] Handle empty stylesheet after removal
- [x] Handle rules inside `@media` queries (fixed via regex change)
- [x] Clean up excessive blank lines after removal

## Phase 3: Testing

### Unit Tests

- [x] Test `find-rule-end-line` with various CSS structures:
  - Simple rule: `.foo { color: red; }`
  - Multi-line rule with properties
  - Rule with nested braces in content (e.g., `content: "}"`)
  - Rule at end of file

- [x] Test `remove-rules`:
  - Remove single rule
  - Remove multiple non-adjacent rules
  - Remove adjacent rules
  - Remove rule with multi-selector (`.a, .b { }`)

### Integration Tests

- [ ] Test `::tsain/replace-css` end-to-end (manual verification)

### Manual Testing

- [ ] Test in callit project with the `.feature-highlight__accent` case from bug report
- [x] Verify REPL workflow works without empty map

## Phase 4: Documentation & Cleanup

- [x] Update docstrings if behavior clarified
- [ ] Update CHANGELOG.md with bugfix entry

## Summary

All 27 tests pass with 207 assertions. The core bugs have been fixed:

1. **Infinite loop** - `find-rule-end-line` nested loops fixed
2. **Selector not found** - `find-selector-line` regex anchor removed
3. **Arity error** - Already fixed with variadic args
