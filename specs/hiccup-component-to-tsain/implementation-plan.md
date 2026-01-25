# Hiccup Component To Tsain - Implementation Plan

## Overview

Extract the `hiccup-component` skill into tsain's resources, adapting it for generic use with any tsain project.

## Prerequisites

- [x] Default-registry discovery API working (commit 17ee1ab)
- [x] SQLite storage working (commit 4c683b3)
- [x] Review current skill content for tsain-specific assumptions

## Phase 1: Create Skill Structure

- [x] Create `.claude/skills/tsain/` directory (tsain dogfoods its own skill)
- [x] Create `SKILL.md` with frontmatter (name: tsain, description, keywords)
- [x] Decide on skill name → `tsain` (commands: `/tsain iterate`, `/tsain implement`)

## Phase 2: Adapt Skill Content

- [x] Copy `iterate` command from current skill
  - [x] Replace hardcoded namespace with `{ui-namespace}` placeholder or "read from tsain.edn" instruction
  - [x] Update discovery examples to use new arities: `(tsain/describe)` instead of `(describe (dispatch))`
  - [x] Update effect examples to use `dispatch` helper from dev namespace

- [x] Copy `implement` command from current skill
  - [x] Reference `/specs implement` for spec-driven workflow
  - [x] Keep REPL discovery instructions (`clj-nrepl-eval --discover-ports`)

- [x] Remove `auto` command (too opinionated for library skill)

- [x] Update all path references to read from `tsain.edn`:
  - [x] `:ui-namespace` for component namespace
  - [x] `:stylesheet` for CSS file location
  - [x] `:database-file` or `:components-file` for storage

- [x] Update effect reference table with current effects

## Phase 3: Documentation

- [x] Add installation instructions to CLAUDE.md (project)
- [x] Update `sample/CLAUDE.md` to mention the skill
- [x] Add skill discovery section to CLAUDE.md (project)

## Phase 4: Testing

Testing deferred - skill is now in place and can be validated by using it.

## File Structure

```
.claude/
  skills/
    tsain/
      SKILL.md          # Main skill file - tsain dogfoods this
```

## Rollout Plan

1. [x] Implement skill in tsain repo
2. [ ] Test with tsain's own sandbox (dogfooding)
3. [x] Document in CLAUDE.md
4. [ ] Tag release

## Rollback Plan

If issues arise:
1. Skill is additive - removing from resources doesn't break anything
2. Users who copied the skill keep their copy
