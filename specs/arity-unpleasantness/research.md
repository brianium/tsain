# Arity Unpleasantness - Research

## Problem Statement

When an effect schema has an optional trailing element like:

```clojure
[:tuple
  [:= ::replace-css]
  [:string]
  [:string]
  [:map {:optional true} ...]]
```

And the effect is invoked without the optional element:

```clojure
(dispatch [[::replace-css ".selector" "content"]])
```

Sandestin calls the handler with only the provided args:

```clojure
(handler ctx system ".selector" "content")  ;; 4 args
```

But the handler expects 5 args:

```clojure
(fn [ctx system selector content opts] ...)  ;; expects 5
```

This causes `Wrong number of args (4) passed to...`.

## Root Cause Analysis

### Where the Bug Lives

The issue is in **sandestin's dispatch mechanism**, not in tsain. When sandestin dispatches an effect:

1. It parses the effect vector `[::key arg1 arg2 ...]`
2. It extracts args from positions 1+
3. It calls `(apply handler ctx system args)`

The problem: sandestin doesn't consult the schema to determine if trailing args should be padded with `nil` for optional elements.

### Current Handler Workaround

The tsain handler uses variadic args as a workaround:

```clojure
(fn [{:keys [dispatch]} _system selector-pattern new-content & [opts]]
  (let [opts (or opts {})]
    ...))
```

But this doesn't work because sandestin still calls with a fixed arity based on effect vector length, not handler arity.

### Why Variadic Args Don't Help

Sandestin's dispatch likely uses something like:

```clojure
(apply handler ctx system (rest effect-vector))
```

For `[::replace-css "sel" "content"]`, this becomes:

```clojure
(apply handler ctx system ["sel" "content"])
```

Which calls the handler with 4 args. The `& [opts]` in the handler signature *should* work here - `opts` would be `nil`.

**Wait** - if `& [opts]` is in the signature, this should work. Let me re-examine...

Actually, looking at the error:
```
Wrong number of args (4) passed to: ascolais.tsain/make-css-effects/fn--18470
```

This suggests the handler is NOT using variadic args, or there's an issue with how it's defined. The current code shows:

```clojure
(fn [{:keys [dispatch]} _system selector-pattern new-content & [opts]]
```

This should accept 4+ args. If it's throwing an arity error with 4 args, either:
1. The code isn't what's running (stale compiled version?)
2. There's something else going on with how sandestin dispatches

## Requirements

### Functional Requirements

1. Effects with optional trailing schema elements must work when called without those elements
2. The handler should receive `nil` for omitted optional args (or the schema's default if specified)
3. No changes to how required args work

### Non-Functional Requirements

- Backward compatible with existing effect handlers
- No performance regression in dispatch hot path

## Options Considered

### Option A: Fix in Sandestin - Pad Optional Args

**Description:** Modify sandestin's dispatch to inspect the schema and pad missing optional args with `nil`.

**Pros:**
- Fixes the root cause
- All effects with optional args work automatically
- Schema is source of truth

**Cons:**
- Requires sandestin release
- Schema inspection adds complexity to dispatch

### Option B: Require Optional Args in Tsain Effects

**Description:** Change tsain effect schemas to make the options map required.

**Pros:**
- Simple workaround
- No dependency changes

**Cons:**
- Worse API ergonomics
- Documentation says it's optional but it's not
- Punts on the real issue

### Option C: Handler Wrapper in Tsain

**Description:** Wrap effect handlers to normalize arity.

**Pros:**
- Can fix without sandestin changes
- Contained to tsain

**Cons:**
- Boilerplate in every effect with optional args
- Doesn't fix the general sandestin issue

## Recommendation

**Option A: Fix in Sandestin** is the correct solution. The schema declares the argument as optional, so sandestin should honor that at dispatch time.

However, we need to verify the exact dispatch behavior first. The current tsain handler *appears* to use variadic args, which should work. There may be:
1. A stale compiled artifact
2. A different issue in sandestin's apply mechanism
3. Something preventing the variadic signature from working

## Open Questions

- [x] Is the handler actually using `& [opts]`? → Yes, confirmed in source
- [ ] Is the running code stale? → Need to test with fresh REPL
- [ ] Does sandestin use `apply` or fixed-arity invocation? → Need to check sandestin source
- [ ] Are there other effects in tsain with same issue? → `::split-css` has same pattern

## References

- Bug report: `bug-report.md`
- Effect handler: `src/clj/ascolais/tsain.clj:318-337`
- Sandestin dispatch: `ascolais.sandestin` namespace (external dep)
