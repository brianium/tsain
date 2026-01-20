# 007: Copy Hiccup to Clipboard

## Status: Complete

## Overview

Add a copy button to the single component view that copies the hiccup for the currently visible variant to the clipboard. This makes it easy to extract component code for use elsewhere.

## Design Principles

1. **One-click copy** - Single button copies the visible variant's hiccup
2. **Works with/without variants** - Handles both legacy single-hiccup components and new multi-example components
3. **Visual feedback** - Button shows "Copied!" briefly after successful copy
4. **Formatted output** - Hiccup is pretty-printed for readability

## User Experience

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← card-type-badges        [Dark ▼]        action-buttons →         │
├──────────────────────────────────────────────────────────────────────┤
│  Card type and rarity indicator badges                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│              Rendered Component                                      │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│  [← Back to Gallery]                     [Copy]  [Delete]            │
└──────────────────────────────────────────────────────────────────────┘
```

After clicking Copy:
- Button text changes to "Copied!" for ~1.5 seconds
- Clipboard contains the formatted hiccup string

## Architecture

Two implementation approaches:

### Option A: Server-side (Recommended)

Click triggers POST to `/sandbox/copy/:name?idx=N`, server returns hiccup as text:

```clojure
;; In sandbox/app.clj routes
["/sandbox/copy/:name"
 {:post (fn [{:keys [path-params query-params]}]
          (let [name (keyword (:name path-params))
                idx (parse-long (get query-params "idx" "0"))
                component (get @state/library name)
                hiccup (get-example-hiccup component idx)]
            {:status 200
             :headers {"Content-Type" "text/plain"}
             :body (with-out-str (clojure.pprint/pprint hiccup))}))}]
```

Browser uses `fetch` + `navigator.clipboard.writeText()`:

```html
<button data-on:click="
  fetch('/sandbox/copy/card-type-badges?idx=0')
    .then(r => r.text())
    .then(t => navigator.clipboard.writeText(t))
">Copy</button>
```

### Option B: Client-side via data attribute

Encode hiccup in a data attribute, copy directly:

```clojure
[:button {:data-hiccup (pr-str hiccup)
          :data-on:click "navigator.clipboard.writeText(this.dataset.hiccup)"}
 "Copy"]
```

**Trade-offs:**
- Option A: Cleaner HTML, formatted output, but requires server round-trip
- Option B: Instant copy, but hiccup in HTML is raw (not pretty-printed), larger page size

## Implementation: Server-side Approach

### New Registry Effect

```clojure
;; In sandbox/registry.clj
::get-hiccup
{::s/description "Return hiccup for a component example as formatted text"
 ::s/schema [:tuple [:= ::get-hiccup] :keyword [:maybe :int]]
 ::s/handler
 (fn [_ _ name example-idx]
   (let [component (get @state/library name)
         hiccup (get-example-hiccup component (or example-idx 0))]
     (when hiccup
       (with-out-str (clojure.pprint/pprint hiccup)))))}
```

### New Route

```clojure
["/sandbox/copy/:name"
 {:get {:handler (fn [{:keys [path-params query-params]}]
                   (let [name (keyword (:name path-params))
                         idx (some-> (get query-params "idx") parse-long)
                         text (dispatch [[::registry/get-hiccup name idx]])]
                     (if text
                       {:status 200
                        :headers {"Content-Type" "text/plain; charset=utf-8"}
                        :body text}
                       {:status 404 :body "Component not found"})))}}]
```

### Updated component-view

```clojure
(defn component-view
  [{:keys [library view]}]
  (let [component-name (:name view)
        example-idx (or (:example-idx view) 0)
        ...]
    [:div.component-view
     ...
     [:div.component-actions
      [:button {:data-on:click "@post('/sandbox/view/gallery')"}
       "← Back to Gallery"]
      [:button.copy-btn
       {:data-on:click (str "
         fetch('/sandbox/copy/" (name component-name) "?idx=" example-idx "')
           .then(r => r.text())
           .then(t => { navigator.clipboard.writeText(t); evt.target.textContent = 'Copied!'; setTimeout(() => evt.target.textContent = 'Copy', 1500); })
       ")}
       "Copy"]
      [:button {:data-on:click (str "@post('/sandbox/uncommit/" (name component-name) "')")}
       "Delete"]]]))
```

## CSS

```css
.copy-btn {
  /* Match existing button styles */
}

.copy-btn:active {
  /* Visual feedback on click */
}
```

## Success Criteria

- [x] Copy button visible on `/sandbox/c/:name` view
- [x] Clicking Copy puts formatted hiccup on clipboard
- [x] Button shows "Copied!" feedback after successful copy
- [x] Works for components with single hiccup (legacy format)
- [x] Works for components with `:examples` (selects current variant)
- [x] Handles missing components gracefully (no crash)

## File Changes

```
dev/src/clj/sandbox/
├── views.clj       # Add Copy button to component-actions
├── app.clj         # Add /sandbox/copy/:name route
└── registry.clj    # Add ::get-hiccup effect (optional, could inline)

dev/resources/public/
└── sandbox.css     # Style for copy button (if needed)
```

## Testing

From browser:
1. Navigate to `/sandbox/c/game-card`
2. Select "Light" variant from dropdown
3. Click "Copy"
4. Paste into editor - should see formatted hiccup for Light variant

From REPL (after implementation):
```clojure
;; Verify hiccup retrieval
(get-example-hiccup (get @state/library :game-card) 1)
```

## Relationship to Other Specs

- **006-dynamic-components**: Uses the `:examples` structure this spec adds copy support for
- **004-component-library**: Core component storage that this reads from
