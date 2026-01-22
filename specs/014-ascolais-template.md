# Spec 014: Ascolais Project Template

## Status: Draft

## Overview

Create a deps-new template for scaffolding web applications powered by the sandestin effect ecosystem. The template produces projects optimized for Claude-assisted development with pre-configured tooling, conventions, and comprehensive documentation.

## Goals

1. **Quick scaffold** - `clj -Tnew create :template brianium/ascolais :name myorg/myapp` produces a working app
2. **Effect-driven architecture** - Sandestin effects as the primary abstraction for business logic
3. **Database-ready** - Manse integration with PostgreSQL and ragtime migrations out of the box
4. **Datastar frontend** - TWK for SSE-based reactivity, kaiin for declarative route generation
5. **Component sandbox** - Tsain integration for REPL-driven UI development
6. **Claude-optimized** - Comprehensive CLAUDE.md, skills, and conventions for maximum AI utility
7. **Production path** - Dev extends production; scaffolded projects work in both contexts

## Template Location

The template will live at `/Users/brian/projects/ascolais` as a standalone deps-new template library, following the structure of `clojure-claude-code`.

## Generated Project Structure

```
myorg-myapp/
├── deps.edn
├── docker-compose.yml                 # PostgreSQL dev database
├── CLAUDE.md                          # Comprehensive ecosystem documentation
├── README.md                          # Quick-start guide
├── .gitignore
├── .dir-locals.el                     # Emacs CIDER support
│
├── .claude/
│   ├── settings.json                  # Paren repair hooks
│   └── skills/
│       ├── clojure-eval/              # REPL evaluation skill
│       └── component-iterate/         # UI component workflow
│
├── src/clj/myorg/myapp/
│   ├── core.clj                       # Application entry point
│   ├── config.clj                     # Integrant system configuration
│   ├── fx/                            # Effect registries
│   │   └── example.clj                # Example domain effects
│   ├── routes.clj                     # Ring route handlers
│   └── views/                         # Hiccup view functions
│       └── layout.clj                 # Base layout/shell
│
├── dev/src/clj/
│   ├── user.clj                       # REPL initialization (clj-reload)
│   ├── dev.clj                        # Dev namespace (start/stop/reload)
│   ├── dev/
│   │   └── config.clj                 # Dev integrant config (extends app)
│   └── sandbox/
│       ├── ui.clj                     # Chassis alias definitions
│       └── views.clj                  # Re-exports + requires ui
│
├── resources/
│   ├── config.edn                     # Environment-specific config (optional)
│   ├── components.edn                 # Tsain component library
│   └── migrations/                    # Ragtime SQL migrations
│       └── .gitkeep
│
├── dev/resources/
│   └── public/
│       └── styles.css                 # Component styles (hot-reloadable)
│
├── test/src/clj/myorg/myapp/
│   └── core_test.clj                  # Test scaffold
│
└── tsain.edn                          # Tsain configuration
```

## Integrant System Design

### Philosophy

- **Initializer functions over defmethod** - `(ns myapp.db)` with `(defn datasource [opts])` auto-registers for `::db/datasource` key
- **Config.clj over config.edn** - Code-based configuration enables `ig/ref` and composition
- **Dev extends production** - Dev config merges into app config, can override or add components
- **Defmethods centralized** - All `ig/halt-key!`, `ig/suspend-key!`, `ig/resume-key!` in config namespace(s)

### App Components (config.clj)

```clojure
(ns myorg.myapp.config
  (:require [myorg.myapp.fx.example :as example]
            [myorg.myapp.routes :as routes]
            [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.manse :as manse]
            [ascolais.kaiin :as kaiin]
            [integrant.core :as ig]))

;; Initializer functions (auto-discovered by integrant)

(defn datasource
  "HikariCP connection pool."
  [{:keys [jdbc-url username password]}]
  (hikari/make-datasource {...}))

(defn store
  "Sfere connection store."
  [{:keys [type duration-ms]}]
  (sfere/store {:type type :duration-ms duration-ms}))

(defn dispatch
  "Composed sandestin dispatch function."
  [{:keys [datasource store registries]}]
  (s/create-dispatch
    (into [(twk/registry)
           (sfere/registry store)
           (manse/registry {:datasource datasource})]
          registries)))

(defn router
  "Reitit router with middleware."
  [{:keys [dispatch routes]}]
  (reitit.ring/router
    (into routes (kaiin/routes dispatch))
    {:data {:middleware [...]}}))

(defn server
  "HTTP-kit server."
  [{:keys [handler port]}]
  (http-kit/run-server handler {:port port}))

;; Halt methods (centralized)

(defmethod ig/halt-key! ::datasource [_ ds]
  (.close ds))

(defmethod ig/halt-key! ::server [_ stop-fn]
  (stop-fn))

;; System configuration

(def config
  {::datasource {:jdbc-url "jdbc:postgresql://localhost:5432/myapp_dev"
                 :username "postgres"
                 :password "postgres"}

   ::store {:type :caffeine
            :duration-ms 30000}

   ::example/registry {:datasource (ig/ref ::datasource)}

   ::dispatch {:datasource (ig/ref ::datasource)
               :store (ig/ref ::store)
               :registries [(ig/ref ::example/registry)]}

   ::router {:dispatch (ig/ref ::dispatch)
             :routes (routes/routes)}

   ::server {:handler (ig/ref ::handler)
             :port 3000}})
```

### Dev Components (dev/config.clj)

```clojure
(ns dev.config
  (:require [myorg.myapp.config :as app]
            [ascolais.tsain :as tsain]
            [integrant.core :as ig]))

(defn tsain-registry
  "Tsain sandbox registry."
  [_opts]
  (tsain/registry))

(defn file-watcher
  "CSS hot-reload watcher."
  [{:keys [dispatch paths]}]
  (watcher/start dispatch paths))

;; Suspend/resume for REPL reloading

(defmethod ig/suspend-key! ::app/server [_ stop-fn]
  (stop-fn))

(defmethod ig/resume-key ::app/server [key opts old-opts old-impl]
  (if (= opts old-opts)
    (ig/init-key key opts)
    (ig/init-key key opts)))

;; Dev config extends app config

(def config
  (merge
    app/config

    ;; Override datasource for dev
    {::app/datasource {:jdbc-url "jdbc:postgresql://localhost:5432/myapp_dev"
                       :username "postgres"
                       :password "postgres"}}

    ;; Add dev-only components
    {::tsain-registry {}

     ::app/dispatch {:datasource (ig/ref ::app/datasource)
                     :store (ig/ref ::app/store)
                     :registries [(ig/ref ::app/example-registry)
                                  (ig/ref ::tsain-registry)]}

     ::file-watcher {:dispatch (ig/ref ::app/dispatch)
                     :paths ["dev/resources/public"]}}))
```

### System Dynamic Var

```clojure
;; dev/src/clj/dev.clj

(defonce ^:dynamic *system* nil)

(defn start []
  (alter-var-root #'*system* (constantly (ig/init dev.config/config))))

(defn stop []
  (when *system*
    (ig/halt! *system*)
    (alter-var-root #'*system* (constantly nil))))

(defn restart []
  (stop)
  (reload/reload)
  (start))

;; Dispatch access
(defn dispatch
  ([] (::app/dispatch *system*))
  ([effects] ((dispatch) effects)))
```

## Effect Organization

### Pattern

Each domain gets its own namespace in `fx/` exporting a `registry` function:

```clojure
(ns myorg.myapp.fx.users
  (:require [ascolais.sandestin :as s]
            [ascolais.manse :as manse]
            [ascolais.twk :as twk]
            [ascolais.kaiin :as kaiin]))

(defn registry
  "User management effects."
  [{:keys [datasource]}]
  {::s/effects
   {::create-user
    {::s/description "Create a new user in the database."
     ::s/schema [:tuple [:= ::create-user] [:map [:email :string] [:name :string]]]
     ::s/handler
     (fn [{:keys [dispatch]} _system user-data]
       (dispatch
         [[::manse/execute-one
           ["INSERT INTO users (email, name) VALUES (?, ?) RETURNING *"
            (:email user-data) (:name user-data)]]]))}}

   ::s/actions
   {::register-user
    {::s/description "Register user and send welcome email."
     ::s/schema [:tuple [:= ::register-user] [:map [:email :string] [:name :string]]]
     ::s/handler
     (fn [_state user-data]
       [[::create-user user-data]
        [::send-welcome-email (:email user-data)]])

     ;; Kaiin metadata for HTTP interface
     ::kaiin/path "/api/users/register"
     ::kaiin/method :post
     ::kaiin/signals [:map [:email :string] [:name :string]]
     ::kaiin/dispatch [::register-user
                       {:email [::kaiin/signal :email]
                        :name [::kaiin/signal :name]}]}}})
```

### Conventions

- **One registry per domain** - `fx/users.clj`, `fx/orders.clj`, etc.
- **Registry function takes deps** - `(defn registry [{:keys [datasource]}])` for integrant injection
- **Namespaced effect keys** - `::users/create-user` not `:create-user`
- **Kaiin metadata on actions** - Actions that need HTTP endpoints get `::kaiin/*` metadata
- **Descriptions always** - Every effect/action has `::s/description` for discoverability

## Route Patterns

### Mixed Approach

```clojure
(ns myorg.myapp.routes
  (:require [myorg.myapp.views.layout :as layout]))

(defn routes
  "Application routes. Kaiin routes merged separately."
  []
  [["/" {:get {:handler (fn [_] {:body (layout/home-page)})}}]

   ;; SSE connection endpoint (long-lived)
   ["/sse" {:post {:handler (fn [{:keys [dispatch] :as req}]
                              ;; Opens SSE, stores connection, syncs state
                              {:ascolais.twk/fx
                               [[:ascolais.sfere/store-connection ...]
                                [:myapp/sync-client]]})}}]

   ;; Static pages
   ["/about" {:get {:handler (fn [_] {:body (layout/about-page)})}}]])

;; Kaiin routes are generated from dispatch and merged in router config:
;; (into (routes/routes) (kaiin/routes dispatch))
```

### When to Use What

| Pattern | Use Case |
|---------|----------|
| Ring handler | Page shells, static content, SSE connection setup |
| Kaiin route | Actions dispatched over existing SSE connection |
| Mixed | Initial page load (ring) + subsequent interactions (kaiin) |

## Database Setup

### Dependencies

```clojure
;; deps.edn
{:deps
 {org.postgresql/postgresql {:mvn/version "42.7.4"}
  com.github.seancorfield/next.jdbc {:mvn/version "1.3.939"}
  com.zaxxer/HikariCP {:mvn/version "6.2.1"}
  io.github.brianium/manse {:git/tag "v0.1.0" :git/sha "..."}
  ragtime/ragtime {:mvn/version "0.9.4"}}}
```

### Migrations

```clojure
;; dev/src/clj/dev.clj

(require '[ragtime.jdbc :as jdbc]
         '[ragtime.repl :as ragtime])

(defn migration-config []
  {:datastore (jdbc/sql-database {:connection-uri (:jdbc-url (::app/datasource *system*))})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate! []
  (ragtime/migrate (migration-config)))

(defn rollback! []
  (ragtime/rollback (migration-config)))
```

Migration files in `resources/migrations/`:
```
001-create-users.up.sql
001-create-users.down.sql
```

### Docker Compose

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: myapp_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

**Workflow:**
```bash
# Start database
docker-compose up -d

# Stop database
docker-compose down

# Reset database (delete volume)
docker-compose down -v
```

### CLAUDE.md Documentation

Include migration workflow:
```markdown
## Database Migrations

Migrations use ragtime with SQL files in `resources/migrations/`.

### Creating a Migration

1. Create numbered SQL files:
   - `resources/migrations/002-add-orders.up.sql`
   - `resources/migrations/002-add-orders.down.sql`

2. Run from REPL:
   ```clojure
   (migrate!)    ;; Apply pending migrations
   (rollback!)   ;; Undo last migration
   ```

### Resetting Database

```clojure
;; Roll back all, then migrate
(dotimes [_ 10] (rollback!))
(migrate!)
```
```

## Tsain Integration

### Configuration

```clojure
;; tsain.edn
{:ui-namespace sandbox.ui
 :components-file "resources/components.edn"
 :stylesheet "dev/resources/public/styles.css"
 :port 3000}
```

### Sandbox Setup

The template includes:
- `dev/src/clj/sandbox/ui.clj` - Starter chassis aliases
- `dev/src/clj/sandbox/views.clj` - Re-exports library views, requires ui
- `dev/resources/public/styles.css` - Component CSS
- `resources/components.edn` - Empty component library

### Dev Routes

Tsain routes are added to dev router only:
```clojure
;; dev/config.clj
::app/router {:dispatch (ig/ref ::app/dispatch)
              :routes (into (routes/routes)
                            (tsain.routes/routes ...))}
```

## CLAUDE.md Content

The generated CLAUDE.md includes:

### Sections

1. **Project Overview** - Tech stack, architecture diagram
2. **Development Setup** - REPL workflow, start/stop/reload
3. **Sandestin Effect System** - Registry authoring, effects, actions, placeholders
4. **REPL Discovery API** - describe, sample, grep, schemas
5. **Datastar Frontend** - Signals, attributes, actions, SSE events
6. **TWK Patterns** - Hiccup, handler responses, patch modes
7. **Sfere Connection Management** - Store, broadcast, patterns
8. **Kaiin Route Generation** - Metadata fields, tokens, SSE integration
9. **Manse Database Effects** - execute, execute-one, transactions
10. **Project Conventions** - Effect organization, route patterns
11. **Database Migrations** - Ragtime workflow
12. **Component Development** - Tsain sandbox workflow
13. **Code Style** - Namespaced keywords, git commits

### Size Target

~800-1000 lines. Dense but complete. This is the "institutional knowledge" for greenfield projects.

## Skills

### clojure-eval

From clojure-claude-code template. REPL evaluation via nREPL.

### component-iterate

Adapted from tsain. Workflow for developing UI components:
1. Preview with `(dispatch [[::tsain/preview [...]]])`
2. Iterate on design
3. Extract styles to CSS
4. Define chassis alias
5. Commit to library

## Dependencies

```clojure
;; deps.edn
{:paths ["src/clj" "resources"]

 :deps
 {org.clojure/clojure {:mvn/version "1.12.0"}

  ;; Effect ecosystem
  io.github.brianium/sandestin {:git/tag "v0.5.0" :git/sha "526d4c5"}
  io.github.brianium/twk {:git/tag "v0.2.1" :git/sha "e3c5011"}
  io.github.brianium/sfere {:git/tag "v0.6.0" :git/sha "2114511"}
  io.github.brianium/kaiin {:git/tag "v0.1.0" :git/sha "088a6af"}
  io.github.brianium/manse {:git/tag "v0.2.0" :git/sha "2f29123"}
  io.github.brianium/tsain {:git/tag "v0.1.0" :git/sha "f45d0be"}

  ;; Web
  http-kit/http-kit {:mvn/version "2.8.0"}
  metosin/reitit {:mvn/version "0.10.0"}
  ring/ring-core {:mvn/version "1.12.2"}
  dev.data-star.clojure/http-kit {:mvn/version "1.0.0-RC7"}

  ;; Database
  org.postgresql/postgresql {:mvn/version "42.7.4"}
  com.github.seancorfield/next.jdbc {:mvn/version "1.3.939"}
  com.zaxxer/HikariCP {:mvn/version "6.2.1"}

  ;; System
  integrant/integrant {:mvn/version "0.13.1"}

  ;; Utilities
  metosin/malli {:mvn/version "0.20.0"}
  dev.onionpancakes/chassis {:mvn/version "1.0.365"}}

 :aliases
 {:dev
  {:extra-paths ["dev/src/clj" "dev/resources"]
   :extra-deps
   {djblue/portal {:mvn/version "0.62.2"}
    io.github.tonsky/clj-reload {:mvn/version "1.0.0"}
    com.nextjournal/beholder {:mvn/version "1.0.3"}
    ragtime/ragtime {:mvn/version "0.9.4"}}
   :jvm-opts ["-Djdk.attach.allowAttachSelf"]}

  :test
  {:extra-paths ["test/src/clj" "test/resources"]
   :extra-deps {io.github.cognitect-labs/test-runner {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
   :main-opts ["-m" "cognitect.test-runner"]
   :exec-fn cognitect.test-runner.api/test}}}
```

## Acceptance Criteria

### Template Generation

- [ ] `clj -Tnew create :template brianium/ascolais :name myorg/myapp` succeeds
- [ ] Generated project compiles without errors
- [ ] `clj -M:dev` starts REPL with all dependencies

### Development Workflow

- [ ] `(dev)` loads dev namespace
- [ ] `(start)` starts system (server at localhost:3000)
- [ ] `(stop)` cleanly shuts down
- [ ] `(reload)` reloads changed namespaces
- [ ] `(restart)` does full cycle
- [ ] Portal opens automatically

### Effect System

- [ ] `(dispatch)` returns composed dispatch function
- [ ] `(s/describe (dispatch))` lists all effects
- [ ] `(s/sample (dispatch) ::example/some-effect)` generates examples
- [ ] Manse effects work with database

### Database

- [ ] `docker-compose up -d` starts PostgreSQL
- [ ] PostgreSQL connection pool configured
- [ ] `(migrate!)` applies pending migrations
- [ ] `(rollback!)` undoes last migration
- [ ] Manse effects execute queries

### Sandbox

- [ ] `/sandbox` serves tsain component browser
- [ ] `(dispatch [[::tsain/preview [:h1 "Hello"]]])` updates browser
- [ ] CSS hot-reload works
- [ ] Component commit/uncommit works

### Routes

- [ ] Ring routes serve pages
- [ ] Kaiin routes generated from dispatch
- [ ] SSE connections work

### Claude Integration

- [ ] CLAUDE.md comprehensive and accurate
- [ ] Paren repair hooks configured
- [ ] clojure-eval skill works
- [ ] component-iterate skill works

## Resolved Questions

1. **Template naming** - `brianium/ascolais`
2. **System management** - Integrant with initializer function pattern
3. **Default port** - 3000
4. **Docker compose** - Yes, include for dev PostgreSQL

## Implementation Notes

### Template Placeholder Syntax

Use `<<` `>>` delimiters in build/ files to avoid conflicts with Clojure `{{}}` syntax (if any).

### File Copying

```clojure
;; template.edn
{:description "Sandestin-powered web application template"
 :transform
 [["build" ""]
  ["src/clj" "src/clj/{{top/file}}/{{main/file}}" ".clj" "clj"]
  ["dev/src/clj" "dev/src/clj"]
  ["test/src/clj" "test/src/clj/{{top/file}}/{{main/file}}" ".clj" "clj"]
  ["resources" "resources"]
  ["dev/resources" "dev/resources"]
  [".claude" ".claude"]]}
```

### Testing the Template

```bash
# Generate to temp directory
clojure -Sdeps '{:deps {io.github.brianium/ascolais {:local/root "."}}}' \
  -Tnew create :template brianium/ascolais :name test/myapp :target-dir /tmp/test-myapp

# Verify
cd /tmp/test-myapp
clj -M:dev
```
