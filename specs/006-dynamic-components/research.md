# Dynamic Component Iteration - Research

## Problem Statement

Components with Datastar interactivity (signals, event handlers) need a way to test different states and configurations without manually clicking through the UI.

## Requirements

### Functional Requirements

1. Store multiple example configurations per component
2. Switch between configurations in browser
3. Patch signals from REPL
4. Keep alias structure separate from behavior

### Non-Functional Requirements

- Simplicity: No magic, explicit attributes
- Testability: Full REPL control

## Options Considered

### Option A: Magic signal binding

**Description:** Aliases automatically set up Datastar signals

**Pros:**
- Less boilerplate per usage

**Cons:**
- Hidden behavior
- Harder to customize
- Couples structure to behavior

### Option B: Explicit attribute pass-through

**Description:** Datastar attrs are just HTML attrs, passed explicitly

**Pros:**
- No magic
- Full flexibility
- Clear what each usage does

**Cons:**
- More verbose examples

## Recommendation

**Option B** - Explicit attributes. Datastar attributes are just HTML attributes. The alias defines structure; the usage site decides behavior. This keeps things simple and predictable.

## References

- Datastar attribute documentation
- Chassis attribute handling
