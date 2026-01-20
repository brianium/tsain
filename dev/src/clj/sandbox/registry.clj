(ns sandbox.registry
  (:require [ascolais.sandestin :as s]
            [ascolais.sfere :as sfere]
            [ascolais.twk :as twk]
            [sandbox.state :as state]
            [sandbox.views :as views]))

(defn- broadcast-view!
  "Broadcast current view state to all connected clients."
  [dispatch state-atom]
  (dispatch {} {}
            [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
              [::twk/patch-elements (views/render-view @state-atom)]]]))

(defn registry
  "Create sandbox registry with state atom."
  [state-atom]
  {::s/effects
   {::preview
    {::s/description "Set preview content and broadcast to clients"
     ::s/schema [:tuple [:= ::preview] :any]
     ::s/handler
     (fn [{:keys [dispatch]} _system hiccup]
       (swap! state-atom assoc
              :preview {:hiccup hiccup}
              :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::preview-append
    {::s/description "Append content to preview"
     ::s/schema [:tuple [:= ::preview-append] :any]
     ::s/handler
     (fn [{:keys [dispatch]} _system hiccup]
       (swap! state-atom update-in [:preview :hiccup]
              (fn [existing]
                (if existing
                  [:div existing hiccup]
                  hiccup)))
       (swap! state-atom assoc :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::preview-clear
    {::s/description "Clear preview content"
     ::s/schema [:tuple [:= ::preview-clear]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc
              :preview {:hiccup nil}
              :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::commit
    {::s/description "Commit component to library with optional examples"
     ::s/schema [:tuple [:= ::commit] :keyword [:maybe [:or :string :map]]]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name opts]
       (let [{:keys [preview]} @state-atom
             hiccup (:hiccup preview)
             ;; Handle different opts formats:
             ;; - nil -> use current preview as single example
             ;; - string -> description, use current preview as single example
             ;; - map with :examples -> use provided examples
             ;; - map without :examples -> description only, use current preview
             component-data
             (cond
               ;; Map with explicit examples
               (and (map? opts) (:examples opts))
               {:description (or (:description opts) "")
                :examples (:examples opts)
                :created-at (java.util.Date.)}

               ;; String description (old API)
               (string? opts)
               {:description opts
                :examples [{:label "Default" :hiccup hiccup}]
                :created-at (java.util.Date.)}

               ;; Map without examples (description only)
               (map? opts)
               {:description (or (:description opts) "")
                :examples [{:label "Default" :hiccup hiccup}]
                :created-at (java.util.Date.)}

               ;; nil - no description, use preview
               :else
               {:description ""
                :examples [{:label "Default" :hiccup hiccup}]
                :created-at (java.util.Date.)})]
         (when (or hiccup (:examples opts))
           (swap! state-atom assoc-in [:library component-name] component-data)
           (state/save-library! (:library @state-atom))
           (broadcast-view! dispatch state-atom))))}

    ::uncommit
    {::s/description "Remove component from library"
     ::s/schema [:tuple [:= ::uncommit] :keyword]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name]
       (swap! state-atom update :library dissoc component-name)
       (state/save-library! (:library @state-atom))
       (broadcast-view! dispatch state-atom))}

    ::show
    {::s/description "Show a single component with optional example index"
     ::s/schema [:tuple [:= ::show] :keyword [:maybe :int]]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name example-idx]
       (swap! state-atom assoc :view {:type :component
                                      :name component-name
                                      :example-idx (or example-idx 0)})
       (broadcast-view! dispatch state-atom))}

    ::show-gallery
    {::s/description "Show component gallery"
     ::s/schema [:tuple [:= ::show-gallery]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc :view {:type :gallery})
       (broadcast-view! dispatch state-atom))}

    ::show-preview
    {::s/description "Switch to preview view"
     ::s/schema [:tuple [:= ::show-preview]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::sync-view
    {::s/description "Sync a client to current view state"
     ::s/schema [:tuple [:= ::sync-view]]
     ::s/handler
     (fn [_ctx _system]
       ;; Returns effects to be sent to the connecting client
       [[::twk/patch-elements (views/render-view @state-atom)]])}

    ::patch-signals
    {::s/description "Patch Datastar signals and broadcast to all clients"
     ::s/schema [:tuple [:= ::patch-signals] :map]
     ::s/handler
     (fn [{:keys [dispatch]} _system signals]
       (dispatch {} {}
                 [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
                   [::twk/patch-signals signals]]]))}

    ::toggle-sidebar
    {::s/description "Toggle sidebar collapsed state"
     ::s/schema [:tuple [:= ::toggle-sidebar]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom update :sidebar-collapsed? not)
       (broadcast-view! dispatch state-atom))}

    ::show-components
    {::s/description "Show components view (with sidebar)"
     ::s/schema [:tuple [:= ::show-components] [:maybe :keyword]]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name]
       (let [library (:library @state-atom)
             current-view (:view @state-atom)
             ;; Select first component if none specified or specified doesn't exist
             target-name (if (and component-name (contains? library component-name))
                           component-name
                           (first (sort (keys library))))
             ;; Preserve example-idx if viewing same component, otherwise reset to 0
             example-idx (if (and (= (:name current-view) target-name)
                                  (:example-idx current-view))
                           (:example-idx current-view)
                           0)]
         (swap! state-atom assoc :view {:type :components
                                        :name target-name
                                        :example-idx example-idx})
         (broadcast-view! dispatch state-atom)))}}})
