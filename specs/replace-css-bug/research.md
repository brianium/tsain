# Replace CSS Bug Fix - Research

## Problem Statement

The `::tsain/replace-css` effect has two bugs that prevent it from working as documented:

### Bug 1: Appends Instead of Replacing

**Root Cause Analysis:**

The `remove-rules` function in `src/clj/ascolais/tsain/css.clj` attempts to remove rules by:
1. Serializing each `RuleSet` object to a string via `.toString()`
2. Using `str/replace` to remove that string from the original CSS

```clojure
(defn remove-rules
  [css-string rules-to-remove]
  (let [rule-texts (set (map str rules-to-remove))
        remaining-css (reduce (fn [css rule-text]
                                (str/replace css rule-text ""))
                              css-string
                              rule-texts)
        ...]
    [remaining-css removed-css]))
```

**The problem:** jStyleParser's `RuleSet.toString()` normalizes the CSS output:
- Removes comments
- Normalizes whitespace
- May reorder properties
- Minifies selector formatting

Example:
```css
/* Original in file */
.feature-highlight__accent {
  color: teal;
  font-weight: bold;
}

/* jStyleParser toString() output */
.feature-highlight__accent{color:teal;font-weight:bold}
```

Since the normalized string doesn't exist in the original file, `str/replace` finds nothing to replace, and the rule remains intact. The new content is then appended, creating duplicates.

### Bug 2: Arity Error Without Options Map

**Root Cause Analysis:**

The effect handler signature:

```clojure
(fn [{:keys [dispatch]} _system selector-pattern new-content opts]
  ...)
```

Sandestin dispatches effects with the following arities:
- Schema has 4 elements: `[:tuple [:= ::replace-css] :string :string [:map {:optional true}]]`
- When called without options, sandestin passes 4 args: `[ctx system pattern content]`
- When called with options, sandestin passes 5 args: `[ctx system pattern content opts]`

The handler expects 5 args always, so the 4-arg call fails.

## Requirements

### Functional Requirements

1. `::tsain/replace-css` must remove matched rules from the stylesheet before appending new content
2. Effect must work with or without the optional third argument (options map)
3. Return value must accurately reflect which selectors were replaced
4. The effect must handle multi-selector rules (e.g., `.card, .card-header { }`)

### Non-Functional Requirements

- Performance: Must handle stylesheets up to ~500 rules efficiently
- Reliability: Must not corrupt the stylesheet on error
- Idempotency: Calling replace twice with same content should result in single instance of rules

## Options Considered

### Option A: Fix String Matching in remove-rules

**Description:** Pre-process the original CSS to normalize it, then match against serialized rules.

**Pros:**
- Minimal code change
- Uses existing jStyleParser infrastructure

**Cons:**
- Would change whitespace/formatting of entire file
- Loses comments
- Risky—could introduce formatting bugs

### Option B: Line-Range Based Removal

**Description:** Track source positions of rules, remove by line ranges.

jStyleParser rules have source position info via `.getSource()`:
- Line number where rule starts
- Could potentially calculate end line by parsing

**Approach:**
1. Parse stylesheet
2. Find matching rules
3. For each rule, get start line from `.getSource().getLine()`
4. Find rule end by scanning for closing `}`
5. Remove lines from original content
6. Append new content

**Pros:**
- Preserves original formatting of non-removed rules
- Preserves comments outside removed rules
- Reliable removal

**Cons:**
- More complex logic
- Need to handle edge cases (rules spanning multiple lines, nested braces in content)

### Option C: Regex-Based Removal

**Description:** Generate regex patterns from selectors to find and remove rules.

**Pros:**
- Doesn't require jStyleParser for removal
- Can match against original source

**Cons:**
- CSS parsing via regex is notoriously fragile
- Hard to handle all edge cases (nested media queries, selector lists)
- Maintenance burden

### Option D: Full Re-serialization

**Description:** Parse entire stylesheet, remove matching rules, re-serialize.

**Pros:**
- Clean approach
- Guaranteed consistent output

**Cons:**
- Loses all comments
- Changes all formatting
- Users may not want normalized CSS

## Recommendation

**Option B: Line-Range Based Removal**

Rationale:
1. Preserves the user's original formatting and comments
2. jStyleParser provides line numbers via `.getSource().getLine()`
3. The end of a rule can be found by counting braces from the start position
4. Most CSS rules are relatively short, making the brace-scanning efficient

For the arity issue, we'll use optional destructuring with a default:

```clojure
(fn [{:keys [dispatch]} _system selector-pattern new-content & [opts]]
  (let [opts (or opts {})]
    ...))
```

Or use Clojure's multi-arity:

```clojure
::s/handler
(fn
  ([ctx system selector-pattern new-content]
   (handler ctx system selector-pattern new-content {}))
  ([{:keys [dispatch]} system selector-pattern new-content opts]
   ...))
```

The variadic approach is cleaner for sandestin effects.

## Open Questions

- [x] Does jStyleParser track end line numbers? → No, only start line
- [x] How does sandestin handle optional schema elements? → Omits them from handler args

## References

- Bug report: `/Users/brian/projects/tsain/bug-report.md`
- jStyleParser docs: https://github.com/nicoulaj/jstyleparser
- Sandestin effect handlers: Pattern is `(fn [ctx system & args] ...)`
