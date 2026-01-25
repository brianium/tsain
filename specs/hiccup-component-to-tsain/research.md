# Hiccup Component To Tsain - Research

## Problem Statement

The `hiccup-component` skill encodes best practices for REPL-driven component development with tsain:
- Alias-first workflow (define structure before iterating)
- CSS extraction before commits
- Discovery-first API exploration
- Spec-driven implementation

Currently this skill must be copied to each project using tsain. This causes:
1. **Drift** - Skill becomes outdated as tsain API evolves
2. **Duplication** - Same content copied across projects
3. **Discovery** - New users don't know the workflow exists

## Requirements

### Functional Requirements

1. Skill distributed with tsain library (via resources or similar)
2. All project-specific paths read from `tsain.edn`
3. Commands: `/tsain iterate`, `/tsain implement`
4. Clear installation instructions for users
5. Works with the new default-registry discovery API

### Non-Functional Requirements

- **Discoverability:** Users should find the skill easily (README, sample files)
- **Maintainability:** Single source of truth, updates with tsain releases
- **Flexibility:** Users can extend/override with project-specific skills

## Options Considered

### Option A: Ship in `resources/tsain/skills/`

**Description:** Skill files live in tsain's resources directory. Users symlink or copy to `.claude/skills/`.

**Pros:**
- Distributed automatically via classpath
- Version-aligned with tsain

**Cons:**
- Complex symlink command to find jar path
- Tsain itself can't easily use the skill for dogfooding
- Users must know to look in resources

### Option B: Keep in `.claude/skills/tsain/`

**Description:** Skill lives in tsain's own `.claude/skills/` directory. Users copy to their project.

**Pros:**
- Tsain dogfoods its own skill
- Simple copy from repo (like `sample/` directory)
- Still version-aligned (in tsain repo)
- No symlink gymnastics

**Cons:**
- Requires manual copy step (but so does Option A)

### Option C: Ship in `sample/` directory

**Description:** Skill lives alongside other sample files users are expected to copy.

**Pros:**
- Consistent with existing `sample/` pattern

**Cons:**
- Tsain can't use it for dogfooding without duplication

## Recommendation

**Option B** - Keep in `.claude/skills/tsain/`.

This is the simplest approach:
1. Tsain uses its own skill during development
2. Users copy the directory from the repo (familiar pattern)
3. No complex classpath symlinks needed

```bash
# In project using tsain - just copy from the repo
cp -r path/to/tsain/.claude/skills/tsain .claude/skills/
```

## Open Questions

- [x] What commands to include? → `iterate` and `implement`
- [x] Where to put the skill files? → `.claude/skills/tsain/` (tsain dogfoods its own skill)
- [x] Should we rename from `hiccup-component` to `tsain` for the skill name? → Yes, `/tsain iterate`, `/tsain implement`
- [x] How to handle the `auto` command? → Omit from library skill, document as advanced

## References

- Current skill: `.claude/skills/hiccup-component/SKILL.md`
- Tsain sample files: `sample/`
- Claude Code skill format documentation
