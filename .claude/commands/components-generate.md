Use the following skills together for this task:
- clojure-eval (for REPL interaction)
- frontend-design:frontend-design (for creating visually distinctive components)

## Setup

1. Start nREPL server: `clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.0"}}}' -M:dev -m nrepl.cmdline --port 7888`
2. Initialize: `(dev)` then `(start)`
3. Navigate to localhost:3000 in browser via Chrome extension
4. Confirm sandbox is visible before iterating

## Component Iteration Workflow

### Phase 1: Exploration (Inline Styles OK)
- Use `(dispatch [[::tsain/preview hiccup]])` to rapidly iterate
- Inline styles are fine during exploration
- Test visual concepts, layouts, color schemes

### Phase 2: CSS Extraction
Before committing, extract styles to `dev/resources/public/styles.css`:
- Use BEM naming: `.component-name`, `.component-name-element`, `.component-name--modifier`
- Use CSS custom properties for theme colors
- Test that CSS classes render correctly

### Phase 3: Chassis Alias (CRITICAL)
**IMPORTANT**: Create a chassis alias in `dev/src/clj/sandbox/ui.clj` BEFORE committing.

```clojure
(defmethod c/resolve-alias ::my-component
  [_ attrs _]
  (let [{:my-component/keys [title value]} attrs]
    [:div.my-component attrs
     [:h2.my-component-title title]
     [:span.my-component-value value]]))
```

Then reload: `(reload)`

### Phase 4: Test with Alias Form
**CRITICAL**: Test the ALIAS form renders correctly:
```clojure
(dispatch [[::tsain/preview
  [:sandbox.ui/my-component
   {:my-component/title "Test"
    :my-component/value "42"}]]])
```

Do NOT commit until the alias form renders correctly.

### Phase 5: Commit Using Alias
Commit with explicit hiccup containing aliases:
```clojure
(dispatch [[::tsain/commit :my-component
  {:description "Description here"
   :hiccup [:sandbox.ui/my-component
            {:my-component/title "Example"
             :my-component/value "100"}]}]])
```

**NEVER** commit raw expanded hiccup. Always use the alias form.

## Why Aliases Matter
- `components.edn` should store lean alias invocations, not verbose hiccup
- Aliases make copied hiccup portable and reusable
- The "Copy" button in the sandbox copies what's in components.edn
- If you commit expanded hiccup, users copy bloated non-portable code

## Common Mistakes to Avoid
1. Testing with raw hiccup then committing from preview state
2. Forgetting to reload REPL after creating alias
3. Committing before verifying alias form renders
4. Using `::tsain/commit :name` without explicit `:hiccup` (pulls from preview)
