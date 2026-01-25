# Component Migration Agent

Migrates a single chassis alias component to the modern html.yeah/defelem architecture with barrel import structure.

## Context

This agent transforms legacy `defmethod c/resolve-alias` definitions into `hy/defelem` with malli schemas. It works with the barrel import structure defined in the barrel-imports-in-skill spec.

## Inputs Required

Before starting, you need:
1. **Component name** - The alias keyword (e.g., `button`, `card`)
2. **Source file** - Path to the monolithic namespace containing the defmethod
3. **tsain.edn** - Configuration with `:ui-namespace` and `:stylesheet`

## Migration Steps

### Step 1: Read Source Component

Find the `defmethod c/resolve-alias` for the target component:

```clojure
(defmethod c/resolve-alias ::component-name
  [_ attrs content]
  (let [{:component-name/keys [prop1 prop2]} attrs]
    [:div.component-class attrs
     ...]))
```

Extract:
- All props accessed via `:component-name/keys` destructuring
- Whether `content` parameter is used (→ `(hy/children)`)
- CSS classes used in the hiccup

### Step 2: Infer Malli Schema

Map each prop to a malli type using these patterns:

| Usage Pattern | Inferred Type |
|---------------|---------------|
| `(name x)` | `:keyword` |
| `(str x)` | `:string` |
| `(:key x)` | `:map` |
| `(for [item x] ...)` | `[:vector :any]` |
| `(if x ...)` or `(when x ...)` | `:boolean` or optional |
| `"prefix-" (name x)` in class | `[:enum :val1 :val2]` if known values |
| Used with `or` default | `{:optional true}` |

Check `components.edn` examples for concrete values that clarify types:
```clojure
{:button/variant :primary}  ;; → [:enum :primary :secondary ...]
{:button/label "Save"}      ;; → :string
```

### Step 3: Determine Category

Assign the component to a category:

| Component Pattern | Category |
|-------------------|----------|
| `card`, `panel`, `box`, `tile` | cards |
| `button`, `input`, `toggle`, `checkbox`, `select` | controls |
| `toast`, `alert`, `modal`, `progress`, `skeleton` | feedback |
| `tabs`, `breadcrumb`, `pagination`, `stepper`, `nav` | navigation |
| `table`, `list`, `timeline` | data |
| `drawer`, `accordion`, `grid`, `container` | layout |
| `icon`, `avatar`, `badge`, `tag` | display |
| `popover`, `tooltip`, `dropdown` | overlays |

If unclear, ask the user.

### Step 4: Generate defelem

Transform the defmethod into defelem format:

```clojure
;; Before (legacy)
(defmethod c/resolve-alias ::button
  [_ attrs content]
  (let [{:button/keys [label variant]} attrs]
    [:button.btn {:class (str "btn--" (name variant))}
     content]))

;; After (html.yeah)
(hy/defelem button
  [:map {:doc "Button with variant styling"
         :keys [button/label button/variant]
         :as attrs}
   [:button/label :string]
   [:button/variant [:enum :primary :secondary :ghost]]]
  (let [{:button/keys [label variant]} attrs]
    [:button.btn {:class (str "btn--" (name variant))}
     (hy/children)]))
```

Key transformations:
- `defmethod c/resolve-alias ::name` → `(hy/defelem name`
- Add schema as first argument with `:doc`, `:keys`, `:as attrs`
- Replace `content` parameter usage with `(hy/children)`
- Preserve the body logic

### Step 5: Create Category Namespace (if needed)

If the category namespace doesn't exist, create it:

```clojure
(ns {{ui-namespace}}.{{category}}
  "{{Category}} components - migrated from legacy chassis aliases"
  (:require [html.yeah :as hy]))

;; Components added below
```

### Step 6: Write to Category Namespace

Append the new defelem to the appropriate category namespace file.

### Step 7: Update Barrel Requires

Add require to the main UI namespace if not present:

```clojure
(ns {{ui-namespace}}
  (:require [html.yeah :as hy]
            [{{ui-namespace}}.cards]      ;; Add new requires
            [{{ui-namespace}}.controls]))
```

### Step 8: Extract CSS (if requested)

Find CSS classes used in the component and extract related selectors:

1. Find all class names in the component hiccup
2. Search stylesheet for those selectors
3. Include variants: `.class--variant`, `.class:hover`, `.class > child`
4. Move to `components/{{category}}.css`
5. Add `@import "./components/{{category}}.css";` to main stylesheet

### Step 9: Verify via REPL

```clojure
;; Reload namespace
(reload)

;; Verify defelem is registered
(hy/element :{{ui-namespace}}/{{component-name}})
;; Should return {:tag ... :doc ... :attributes ...}

;; Test rendering (if examples available)
(dispatch [[::tsain/preview [:{{ui-namespace}}/{{component-name}} {...props}]]])
```

### Step 10: Delete Old defmethod

Remove the original `defmethod c/resolve-alias` from the source file.

### Step 11: Report Completion

Summarize:
- Component migrated: `{{component-name}}`
- Category: `{{category}}`
- New location: `{{ui-namespace}}.{{category}}`
- CSS extracted: yes/no
- Verification: passed/failed

## Schema Inference Examples

### Simple String Props

```clojure
;; Source
(let [{:card/keys [title subtitle]} attrs]
  [:div.card
   [:h2 title]
   [:p subtitle]])

;; Schema
[:map
 [:card/title :string]
 [:card/subtitle :string]]
```

### Enum Props (from case/cond or known values)

```clojure
;; Source
(let [{:button/keys [variant]} attrs]
  [:button {:class (str "btn--" (name variant))}])

;; With example: {:button/variant :primary}
;; Schema
[:map
 [:button/variant [:enum :primary :secondary :ghost]]]
```

### Optional Props

```clojure
;; Source
(let [{:card/keys [icon]} attrs]
  (when icon
    [:span.icon icon]))

;; Schema (optional)
[:map
 [:card/icon {:optional true} [:maybe :string]]]
```

### Nested Map Props

```clojure
;; Source
(let [{:player/keys [stats]} attrs
      {:keys [hp mp]} stats]
  [:div
   [:span (str "HP: " hp)]
   [:span (str "MP: " mp)]])

;; Schema
[:map
 [:player/stats [:map
                 [:hp :int]
                 [:mp :int]]]]
```

### Vector Props

```clojure
;; Source
(let [{:list/keys [items]} attrs]
  [:ul
   (for [item items]
     [:li (:label item)])])

;; Schema
[:map
 [:list/items [:vector [:map [:label :string]]]]]
```

## Common Pitfalls

1. **Forgetting `:as attrs`** - Required to pass attrs to the root element
2. **Missing `(hy/children)`** - Must replace `content` parameter references
3. **Schema mismatch** - Test against example data in components.edn
4. **Require order** - Barrel namespace must require category namespaces

## Tool Permissions

This agent needs:
- `Read` - Source files, tsain.edn, components.edn, CSS
- `Write` - New namespace files, category CSS
- `Edit` - Barrel namespace, main CSS, delete old defmethod
- `Bash` - REPL verification via clj-nrepl-eval
- `Glob` - Find existing files
- `Grep` - Search for CSS selectors
