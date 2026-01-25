# Better Component Storage - Research

## Problem Statement

The current `components.edn` flat-file storage becomes unwieldy as component libraries grow. With only 9 components, the file is already 477 lines. A production design system with 50+ components would be thousands of lines of EDN - difficult to read, slow to parse, and lacking in queryability.

Additionally, the current storage model captures minimal metadata. Components stored today have:
- `:description` - free-form string
- `:examples` - vector of `{:label :hiccup}` maps
- `:created-at` - timestamp

Missing capabilities:
- **No searchability** - can't query by description, props, or patterns
- **No prop documentation** - Claude has to read component alias code to understand props
- **No categorization** - no way to group related components
- **No version tracking** - no history of component evolution
- **No explicit attr schema** - chassis aliases use destructuring with no formal schema

## Requirements

### Functional Requirements

1. Store components with rich, queryable metadata
2. Support component discovery via sandestin-style API (`describe`, `grep`, `props`)
3. Migrate existing `components.edn` without data loss
4. Work as a dev dependency in consumer projects (portable SQLite)
5. Support multiple examples per component with labels
6. Provide prop/attribute documentation from authoritative source

### Non-Functional Requirements

- **Performance:** Fast reads for sandbox browsing (<50ms)
- **Portability:** Single-file database checked into git or gitignored
- **Compatibility:** All 0.x.x versions maintain schema compatibility
- **Tooling:** Standard SQLite - viewable with any SQLite browser

## Options Considered

### Option A: SQLite Only (Props in Database)

**Description:**
Store everything in SQLite including prop schemas manually declared at commit time.

**Pros:**
- Single source of truth in database
- No additional dependencies beyond manse

**Cons:**
- Props can drift from actual component code
- Manual prop declaration is tedious
- Requires custom validation to detect drift

### Option B: Enhanced EDN with Index Files

**Description:**
Keep EDN but split into one file per component plus an index file.

**Pros:**
- No new dependencies
- Human-readable diffs

**Cons:**
- No built-in search/query capability
- Still requires full file reads for search
- No prop introspection

### Option C: Datascript (In-Memory with EDN Persistence)

**Description:**
Use Datascript for in-memory queryability with periodic EDN persistence.

**Pros:**
- Datalog queries
- Pure Clojure

**Cons:**
- All data in memory
- No native FTS
- No prop introspection

### Option D: SQLite + html.yeah (Recommended)

**Description:**
Use SQLite for examples/categories, but leverage html.yeah for authoritative prop/attribute schemas. html.yeah's `defelem` macro attaches malli schemas to components and provides a discoverability API.

**Architecture:**
```
┌──────────────────────────────────────────────────────────────┐
│                    html.yeah (source of truth)               │
│  defelem → malli schema + chassis alias + queryable metadata │
└──────────────────────────────┬───────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
    │ SQLite       │   │ tsain/describe│  │ malli.dev    │
    │ Stores:      │   │ Merges:       │  │ Validates at │
    │ - examples   │   │ - html.yeah   │  │ runtime      │
    │ - category   │   │   props/doc   │  │              │
    │ - timestamps │   │ - SQLite      │  │              │
    │              │   │   examples    │  │              │
    └──────────────┘   └──────────────┘   └──────────────┘
```

**Simplified SQLite Schema:**
```sql
-- Core component table (no props - html.yeah provides those)
CREATE TABLE components (
  id INTEGER PRIMARY KEY,
  tag TEXT UNIQUE NOT NULL,       -- e.g., "myapp.ui/button"
  category TEXT,                  -- organizational grouping
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

-- Component examples (1:many)
CREATE TABLE examples (
  id INTEGER PRIMARY KEY,
  component_id INTEGER NOT NULL REFERENCES components(id) ON DELETE CASCADE,
  label TEXT NOT NULL,
  hiccup TEXT NOT NULL,  -- EDN serialized
  sort_order INTEGER DEFAULT 0
);

-- Full-text search (includes html.yeah doc via trigger/sync)
CREATE VIRTUAL TABLE components_fts USING fts5(
  tag, category, doc,
  content='components',
  content_rowid='id'
);
```

**html.yeah Discovery:**
```clojure
(require '[html.yeah :as hy])

;; Get component metadata
(hy/element :myapp.ui/button)
;; => {:tag :myapp.ui/button
;;     :doc "Primary action button with variants"
;;     :attributes [:map
;;                  [:button/label :string]
;;                  [:button/variant [:enum :primary :secondary :ghost]]
;;                  [:button/size {:default :md} [:enum :sm :md :lg]]]
;;     :children [:* :any]
;;     :ns myapp.ui}

;; Search component docs
(hy/search-elements "button")
;; => [{:tag :myapp.ui/button :doc "Primary action button..."}
;;     {:tag :myapp.ui/icon-button :doc "Icon-only button..."}]
```

**Pros:**
- Props live in code with components (single source of truth)
- Malli schemas provide types, defaults, validation
- html.yeah discoverability API already exists
- Runtime validation via malli.dev instrumentation
- SQLite only stores what code can't provide (examples, categories)
- No prop drift - schemas are authoritative

**Cons:**
- Requires html.yeah adoption (pivot from raw `c/resolve-alias`)
- Additional dependency
- Projects must migrate existing aliases to `defelem`

## Recommendation

Based on the analysis above, we recommend **Option D: SQLite + html.yeah** because:

1. **Authoritative schemas** - Props defined in code via `defelem`, not manually at commit time
2. **No drift** - Malli schemas are the source of truth; validation catches misuse
3. **Built-in discovery** - `hy/element`, `hy/search-elements` already exist
4. **Simpler SQLite** - Database only stores examples/categories, not prop schemas
5. **Runtime validation** - malli.dev provides helpful error messages
6. **Compiles to chassis** - `defelem` produces `defmethod c/resolve-alias` (drop-in replacement)

## Open Questions

- [x] Where should the SQLite file live?
  - **Answer:** Configurable via `:database-file` in `tsain.edn`, default `"tsain.db"` in project root

- [x] How to handle schema migrations?
  - **Answer:** Use a `schema_version` table. On startup, check version and run migrations if needed.

- [x] Should hiccup be stored as EDN text or as structured data?
  - **Answer:** EDN text. Hiccup is opaque to the database.

- [x] How to expose prop schemas from component definitions?
  - **Answer:** Use html.yeah. `defelem` attaches malli schemas queryable via `hy/element`.
  - No manual prop declaration needed - schemas live in code
  - tsain queries html.yeah at runtime for prop info

- [x] What exploration API should tsain expose?
  - **Answer:** Plain functions that merge html.yeah + SQLite data:
    - `tsain/describe` - merges html.yeah props/doc with SQLite examples/category
    - `tsain/grep` - searches both html.yeah docs and SQLite categories
    - `tsain/props` - delegates to html.yeah schema introspection

## SQLite Portability in Dev Tools

### How It Works

When tsain is installed as a dev dependency, the SQLite file is created in the **consumer project**:

```clojure
;; tsain.edn in consumer project
{:database-file "dev/tsain.db"   ;; Consumer chooses location
 :ui-namespace myapp.ui
 ...}
```

The file path is resolved relative to the consumer project's working directory.

### Initialization Flow

```
1. (tsain/registry) called during system startup
2. Read :database-file from tsain.edn (or use default)
3. Resolve path relative to project root
4. If file doesn't exist: create + run all migrations
5. If file exists: check schema_version, run pending migrations
6. Return datasource in registry for manse effects
```

### Migration from components.edn

Tsain will include a migration effect:

```clojure
[::tsain/migrate-from-edn "path/to/components.edn"]
```

This:
1. Reads the EDN file
2. Inserts each component into SQLite (examples, timestamps)
3. Descriptions migrate only if component lacks html.yeah `:doc`
4. Reports migration status

## tsain.edn Changes

```clojure
;; Current
{:ui-namespace sandbox.ui
 :components-file "resources/components.edn"  ;; DEPRECATED
 :stylesheet "dev/resources/public/styles.css"
 :port 3000}

;; Proposed
{:ui-namespace sandbox.ui
 :database-file "tsain.db"                    ;; NEW - SQLite location
 :components-file "resources/components.edn"  ;; DEPRECATED - for migration only
 :stylesheet "dev/resources/public/styles.css"
 :port 3000}
```

## Component Metadata for Claude

### Simplified Commit API

Props/descriptions come from html.yeah; commit only provides examples and category:

```clojure
[::tsain/commit :myapp.ui/button
 {:category :forms
  :examples [{:label "Primary" :hiccup [...]}
             {:label "With Icon" :hiccup [...]}]}]
```

### Discovery API (Merges html.yeah + SQLite)

```clojure
;; List all components
(tsain/describe dispatch)
;; => [{:tag :myapp.ui/button
;;      :doc "Primary action button..."      ;; from html.yeah
;;      :category :forms                      ;; from SQLite
;;      :attributes [:map [:button/label :string] ...]}  ;; from html.yeah
;;     ...]

;; Inspect specific component
(tsain/describe dispatch :myapp.ui/button)
;; => {:tag :myapp.ui/button
;;     :doc "Primary action button with multiple variants"
;;     :category :forms
;;     :attributes [:map
;;                  [:button/label :string]
;;                  [:button/variant [:enum :primary :secondary :ghost]]
;;                  [:button/size {:default :md} [:enum :sm :md :lg]]]
;;     :examples [{:label "Primary" :hiccup [...]}
;;                {:label "With Icon" :hiccup [...]}]}

;; Search by text (searches html.yeah docs + SQLite categories)
(tsain/grep dispatch "button")
;; => [{:tag :myapp.ui/button :match-context "Primary action button..."}
;;     {:tag :myapp.ui/icon-button :match-context "Icon-only button..."}]

;; Find by attribute name
(tsain/props dispatch :size)
;; => [{:tag :myapp.ui/button :attribute [:button/size {:default :md} [:enum :sm :md :lg]]}
;;     {:tag :myapp.ui/input :attribute [:input/size [:enum :sm :md :lg]]}]
```

### Claude Integration Pattern

In project CLAUDE.md:

```markdown
## Components

Components are defined with html.yeah's `defelem`. Use tsain discovery to explore:

\`\`\`clojure
(tsain/describe (dispatch))              ;; List all with props
(tsain/describe (dispatch) :ui/button)   ;; Full details
(tsain/grep (dispatch) "button")         ;; Search docs
(tsain/props (dispatch) :variant)        ;; Find by attribute
\`\`\`

Prop schemas are authoritative - defined in code via `defelem`, not manually documented.
```

## References

- [html.yeah](https://github.com/brianium/html.yeah) - Schema-driven HTML components with discoverability
- [manse](https://github.com/brianium/manse) - Sandestin effects for next.jdbc
- [SQLite FTS5](https://www.sqlite.org/fts5.html) - Full-text search extension
- [spectacular CLAUDE.md](/Users/brian/projects/spectacular/CLAUDE.md) - Example of manual component documentation
