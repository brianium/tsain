(ns sandbox.ui
  "Chassis alias definitions for sandbox components using html.yeah.

   ## defelem Components

   Components are defined with `hy/defelem`, which provides:
   - Malli schemas for attribute validation
   - Queryable metadata via `(hy/element :sandbox.ui/component)`
   - Automatic Chassis alias registration

   ## Naming Conventions

   - Namespaced attrs (`:game-card/title`) = config props (elided from HTML)
   - Regular attrs (`:data-on:click`, `:class`) = pass-through to HTML
   - Namespace config by component name for self-documenting code

   ## Usage

   ```clojure
   ;; In components.edn - lean config
   [:sandbox.ui/game-card
    {:game-card/title \"Neural Phantom\"
     :game-card/cost \"4\"
     :game-card/attack \"3\"
     :data-signals:selected \"false\"}]

   ;; Query component metadata
   (hy/element :sandbox.ui/game-card)
   ```"
  (:require [html.yeah :as hy]))

;; =============================================================================
;; Game Card Component
;; =============================================================================

(hy/defelem game-card
  [:map {:doc "Cyberpunk-styled game card with cost, stats, and ability text"
         :as attrs}
   [:game-card/title :string]
   [:game-card/type :string]
   [:game-card/cost :string]
   [:game-card/attack :string]
   [:game-card/defense :string]
   [:game-card/icon :string]
   [:game-card/ability :string]
   [:game-card/flavor :string]]
  (let [{:game-card/keys [title type cost attack defense icon ability flavor]} attrs]
    [:div.game-card.clip-corners-lg attrs
     [:div.game-card-corner.game-card-corner--top-h]
     [:div.game-card-corner.game-card-corner--top-v]
     [:div.game-card-corner.game-card-corner--bottom-h]
     [:div.game-card-corner.game-card-corner--bottom-v]
     [:div.scanline-overlay]
     [:div.game-card-inner-border.clip-corners-md]
     [:div.game-card-cost.clip-hexagon
      [:div.game-card-cost-value cost]]
     [:div.game-card-header
      [:div.game-card-title title]
      [:div.game-card-type type]]
     [:div.game-card-art
      [:div.game-card-art-grid]
      [:div.game-card-art-icon icon]
      [:div.game-card-art-line]
      [:div.game-card-art-bracket.game-card-art-bracket--tl]
      [:div.game-card-art-bracket.game-card-art-bracket--tr]
      [:div.game-card-art-bracket.game-card-art-bracket--bl]
      [:div.game-card-art-bracket.game-card-art-bracket--br]]
     [:div.game-card-text
      [:div.game-card-text-accent]
      [:div.game-card-ability
       [:span.game-card-ability-keyword "▶ Stealth Protocol: "]
       ability]
      [:div.game-card-flavor flavor]]
     [:div.game-card-footer
      [:div.game-card-set
       [:div.game-card-set-icon.clip-diamond]
       [:div.game-card-set-id "SET-01 // 042"]]
      [:div.game-card-stats
       [:div.game-card-stat.game-card-stat--attack.clip-stat-hex attack]
       [:div.game-card-stat.game-card-stat--defense.clip-stat-hex defense]]]]))

;; =============================================================================
;; Combat Log Component
;; =============================================================================

(hy/defelem combat-log-entry
  [:map {:doc "Single entry in a combat log with timestamp and optional detail"
         :as attrs}
   [:combat-log-entry/time :string]
   [:combat-log-entry/message :string]
   [:combat-log-entry/detail {:optional true} [:maybe :string]]
   [:combat-log-entry/detail-type {:optional true} [:maybe :keyword]]
   [:combat-log-entry/highlight {:optional true} [:maybe :keyword]]]
  (let [{:combat-log-entry/keys [time message detail detail-type highlight]} attrs
        entry-class (when highlight (str "combat-log-entry--highlight-" (name highlight)))]
    [:div.combat-log-entry {:class entry-class}
     [:span.combat-log-time time]
     [:div
      [:div.combat-log-message message]
      (when detail
        [:div.combat-log-detail
         {:class (when detail-type (str "combat-log-" (name detail-type)))}
         detail])]]))

(hy/defelem combat-log
  [:map {:doc "Combat log panel showing turn-based game events"
         :as attrs}
   [:combat-log/turn :int]
   [:combat-log/entries [:vector [:map
                                  [:time :string]
                                  [:message :string]
                                  [:detail {:optional true} :string]
                                  [:detail-type {:optional true} :keyword]
                                  [:highlight {:optional true} :keyword]]]]]
  (let [{:combat-log/keys [turn entries]} attrs]
    [:div.combat-log attrs
     [:div.scanline-overlay]
     [:div.combat-log-header
      [:span.combat-log-title "◆ Combat Log"]
      [:span.combat-log-turn (str "TURN " turn)]]
     [:div.combat-log-entries
      (for [{:keys [time message detail detail-type highlight]} entries]
        [:sandbox.ui/combat-log-entry
         {:combat-log-entry/time time
          :combat-log-entry/message message
          :combat-log-entry/detail detail
          :combat-log-entry/detail-type detail-type
          :combat-log-entry/highlight highlight}])]
     [:div.combat-log-input
      [:div.combat-log-input-field "> Type command..."]
      [:div.combat-log-submit "⏎"]]]))

;; =============================================================================
;; Badge Components
;; =============================================================================

(hy/defelem badge
  [:map {:doc "Small icon+label badge with type-based styling"
         :as attrs}
   [:badge/icon :string]
   [:badge/label :string]
   [:badge/type [:enum :attack :defense :speed :stealth]]]
  (let [{:badge/keys [icon label type]} attrs]
    [:div.badge.clip-badge {:class (str "badge--" (name type))}
     [:span.badge-icon icon]
     [:span.badge-label label]]))

(hy/defelem badge-rarity
  [:map {:doc "Rarity indicator badge with colored dot"
         :as attrs}
   [:badge-rarity/label :string]
   [:badge-rarity/rarity [:enum :common :uncommon :rare :legendary]]]
  (let [{:badge-rarity/keys [label rarity]} attrs
        dot-class (if (= rarity :legendary) "clip-diamond" nil)]
    [:div.badge-rarity {:class (str "badge-rarity--" (name rarity))}
     [:div.badge-rarity-dot {:class dot-class}]
     [:span.badge-rarity-label label]]))

(hy/defelem card-type-badges
  [:map {:doc "Collection of type badges and rarity indicators for a card"
         :as attrs}
   [:card-type-badges/badges [:vector [:map
                                       [:icon :string]
                                       [:label :string]
                                       [:type :keyword]]]]
   [:card-type-badges/rarities [:vector [:map
                                         [:label :string]
                                         [:rarity :keyword]]]]]
  (let [{:card-type-badges/keys [badges rarities]} attrs]
    [:div attrs
     (for [{:keys [icon label type]} badges]
       [:sandbox.ui/badge {:badge/icon icon :badge/label label :badge/type type}])
     [:div {:style "margin-top: 16px; width: 100%; display: flex; gap: 12px;"}
      (for [{:keys [label rarity]} rarities]
        [:sandbox.ui/badge-rarity {:badge-rarity/label label :badge-rarity/rarity rarity}])]]))

;; =============================================================================
;; Player HUD Component
;; =============================================================================

(hy/defelem player-hud-bar
  [:map {:doc "Progress bar for player stats (health, energy)"
         :as attrs}
   [:player-hud-bar/label :string]
   [:player-hud-bar/value :string]
   [:player-hud-bar/percent :string]
   [:player-hud-bar/type [:enum :health :energy]]]
  (let [{:player-hud-bar/keys [label value percent type]} attrs]
    [:div.player-hud-bar
     [:div.player-hud-bar-header
      [:span.player-hud-bar-label {:class (str "player-hud-bar-label--" (name type))} label]
      [:span.player-hud-bar-value {:class (str "player-hud-bar-value--" (name type))} value]]
     [:div.player-hud-bar-track {:class (str "player-hud-bar-track--" (name type))}
      [:div.player-hud-bar-fill {:class (str "player-hud-bar-fill--" (name type))
                                 :style (str "width: " percent ";")}]
      [:div.player-hud-bar-segments]]]))

(hy/defelem player-hud-counter
  [:map {:doc "Numeric counter display for player resources"
         :as attrs}
   [:player-hud-counter/label :string]
   [:player-hud-counter/value :string]
   [:player-hud-counter/type [:enum :shield :cards]]]
  (let [{:player-hud-counter/keys [label value type]} attrs]
    [:div.player-hud-counter
     [:div.player-hud-counter-label label]
     [:div.player-hud-counter-value {:class (str "player-hud-counter-value--" (name type))} value]]))

(hy/defelem player-hud
  [:map {:doc "Full player HUD panel with avatar, health, energy, and counters"
         :as attrs}
   [:player-hud/name :string]
   [:player-hud/rank :string]
   [:player-hud/avatar :string]
   [:player-hud/health [:map [:current :int] [:max :int] [:percent :string]]]
   [:player-hud/energy [:map [:current :int] [:max :int] [:percent :string]]]
   [:player-hud/counters [:vector [:map [:label :string] [:value :string] [:type :keyword]]]]]
  (let [{:player-hud/keys [name rank avatar health energy counters]} attrs
        {:keys [current max percent]} health
        health-value (str current "/" max)
        {:keys [current max percent] :as energy-data} energy
        energy-value (str current "/" max)]
    [:div.player-hud.clip-asymmetric attrs
     [:div.player-hud-corner.player-hud-corner--tl-h]
     [:div.player-hud-corner.player-hud-corner--tl-v]
     [:div.player-hud-corner.player-hud-corner--br]
     [:div.scanline-overlay]
     [:div.player-hud-info
      [:div.player-hud-avatar.clip-octagon avatar]
      [:div
       [:div.player-hud-name name]
       [:div.player-hud-rank (str "// RANK: " rank)]]]
     [:sandbox.ui/player-hud-bar {:player-hud-bar/label "◆ INTEGRITY"
                                  :player-hud-bar/value health-value
                                  :player-hud-bar/percent (:percent health)
                                  :player-hud-bar/type :health}]
     [:sandbox.ui/player-hud-bar {:player-hud-bar/label "◆ ENERGY"
                                  :player-hud-bar/value energy-value
                                  :player-hud-bar/percent (:percent energy-data)
                                  :player-hud-bar/type :energy}]
     [:div.player-hud-counters
      (for [{:keys [label value type]} counters]
        [:sandbox.ui/player-hud-counter {:player-hud-counter/label label
                                         :player-hud-counter/value value
                                         :player-hud-counter/type type}])]]))

;; =============================================================================
;; Action Buttons Component
;; =============================================================================

(hy/defelem btn
  [:map {:doc "Button component with variant, color, and size options"
         :as attrs}
   [:btn/variant {:optional true} [:maybe [:enum :primary :secondary]]]
   [:btn/color {:optional true} [:maybe [:enum :cyan :magenta :red]]]
   [:btn/size {:optional true} [:maybe [:enum :small]]]]
  (let [{:btn/keys [variant color size]} attrs
        base-class "btn"
        variant-class (when variant (str "btn--" (name variant)))
        color-class (when color (str "btn--" (name color)))
        size-class (when size (str "btn--" (name size)))
        small-color-class (when (and (= size :small) color) (str "btn--small-" (name color)))]
    [:button {:class (str base-class
                          (when variant-class (str " " variant-class))
                          (when color-class (str " " color-class))
                          (when size-class (str " " size-class))
                          (when small-color-class (str " " small-color-class))
                          " clip-corners-sm")}
     (hy/children)]))

(hy/defelem action-buttons
  [:map {:doc "Group of action buttons with primary, secondary, and small variants"
         :as attrs}
   [:action-buttons/primary [:vector [:map [:label :string] [:icon :string]]]]
   [:action-buttons/secondary [:vector [:map [:label :string] [:icon :string] [:color :keyword]]]]
   [:action-buttons/small {:optional true} [:maybe [:vector [:map [:label :string] [:color :keyword]]]]]]
  (let [{:action-buttons/keys [primary secondary small]} attrs]
    [:div attrs
     (for [{:keys [label icon]} primary]
       [:sandbox.ui/btn {:btn/variant :primary} (str icon " " label)])
     (for [{:keys [label icon color]} secondary]
       [:sandbox.ui/btn {:btn/variant :secondary :btn/color color} (str icon " " label)])
     (when (seq small)
       [:div {:style "display: flex; gap: 8px; margin-top: 16px;"}
        (for [{:keys [label color]} small]
          [:sandbox.ui/btn {:btn/size :small :btn/color color} label])])]))

;; =============================================================================
;; Resource Display Component
;; =============================================================================

(hy/defelem energy-orbs
  [:map {:doc "Visual energy orb display showing active/total"
         :as attrs}
   [:energy-orbs/active :int]
   [:energy-orbs/total :int]]
  (let [{:energy-orbs/keys [active total]} attrs]
    [:div.energy-orbs
     (for [i (range total)]
       [:div.energy-orb {:class (if (< i active) "energy-orb--active" "energy-orb--empty")}])]))

(hy/defelem credit-chips
  [:map {:doc "Visual credit chip display showing active/total"
         :as attrs}
   [:credit-chips/active :int]
   [:credit-chips/total :int]]
  (let [{:credit-chips/keys [active total]} attrs]
    [:div.credits-chips
     (for [i (range total)]
       [:div.credit-chip.clip-hexagon {:class (if (< i active) "credit-chip--active" "credit-chip--empty")}])]))

(hy/defelem resource-display
  [:map {:doc "Combined energy and credits resource panel"
         :as attrs}
   [:resource-display/energy [:map [:active :int] [:total :int]]]
   [:resource-display/credits [:map [:active :int] [:total :int] [:value :int]]]]
  (let [{:resource-display/keys [energy credits]} attrs
        {:keys [active total]} energy
        {:keys [active total value] :as credits-data} credits]
    [:div attrs
     [:div.resource-panel.resource-panel--energy
      [:div.resource-label.resource-label--energy "◆ Available Energy"]
      [:sandbox.ui/energy-orbs {:energy-orbs/active (:active energy) :energy-orbs/total (:total energy)}]]
     [:div.resource-panel.resource-panel--credits
      [:div.credits-header
       [:span.resource-label.resource-label--credits "◆ Credits"]
       [:span.credits-value (str "₵ " (:value credits-data))]]
      [:sandbox.ui/credit-chips {:credit-chips/active (:active credits-data) :credit-chips/total (:total credits-data)}]]]))

;; =============================================================================
;; Toast Component
;; =============================================================================

(hy/defelem toast
  [:map {:doc "Notification toast with icon, message, and dismiss button"
         :as attrs}
   [:toast/variant {:optional true} [:enum :info :success :warning :error]]
   [:toast/label :string]
   [:toast/message :string]
   [:toast/icon :string]]
  (let [{:toast/keys [variant label message icon]} attrs
        variant-class (str "toast--" (name (or variant :info)))]
    [:div.toast {:class variant-class}
     [:div.toast-accent]
     [:div.toast-icon icon]
     [:div.toast-content
      [:div.toast-label (str "◆ " label)]
      [:div.toast-message message]]
     [:button.toast-dismiss "✕"]]))

;; =============================================================================
;; Player Portrait Component (16-bit Cyberpunk)
;; =============================================================================

(hy/defelem player-portrait-stat
  [:map {:doc "Single stat bar for player portrait"
         :as attrs}
   [:player-portrait-stat/label :string]
   [:player-portrait-stat/value :string]
   [:player-portrait-stat/percent :string]
   [:player-portrait-stat/type [:enum :hp :mp :exp]]]
  (let [{:player-portrait-stat/keys [label value percent type]} attrs
        type-class (str "player-portrait-stat--" (name type))]
    [:div.player-portrait-stat {:class type-class}
     [:span.player-portrait-stat-label label]
     [:div.player-portrait-stat-track
      [:div.player-portrait-stat-fill {:style (str "width: " percent)}]
      [:div.player-portrait-stat-segments]]
     [:span.player-portrait-stat-value value]]))

(hy/defelem player-portrait
  [:map {:doc "16-bit style player portrait with pixel art and stats"
         :as attrs}
   [:player-portrait/name :string]
   [:player-portrait/class :string]
   [:player-portrait/level :int]
   [:player-portrait/theme {:optional true} [:enum :cyan :magenta :green]]
   [:player-portrait/active? {:optional true} :boolean]
   [:player-portrait/pixels :string]
   [:player-portrait/stats [:vector [:map
                                     [:label :string]
                                     [:value :string]
                                     [:percent :string]
                                     [:type :keyword]]]]]
  (let [{:player-portrait/keys [name class level theme active? pixels stats]} attrs
        theme-class (str "player-portrait--" (clojure.core/name (or theme :cyan)))]
    [:div.player-portrait {:class theme-class}
     [:div.player-portrait-scanlines]
     [:div.player-portrait-corner.player-portrait-corner--tl]
     [:div.player-portrait-corner.player-portrait-corner--tr]
     [:div.player-portrait-corner.player-portrait-corner--bl]
     [:div.player-portrait-corner.player-portrait-corner--br]
     (when active?
       [:div.player-portrait-active])
     [:div.player-portrait-art
      [:div.player-portrait-pixels {:style (str "box-shadow: " pixels)}]]
     [:div.player-portrait-name
      [:span.player-portrait-name-text name]]
     [:div.player-portrait-stats
      (for [{:keys [label value percent type]} stats]
        [:sandbox.ui/player-portrait-stat
         {:player-portrait-stat/label label
          :player-portrait-stat/value value
          :player-portrait-stat/percent percent
          :player-portrait-stat/type type}])
      [:div.player-portrait-footer
       [:span.player-portrait-level (str "LV." level)]
       [:span.player-portrait-class class]]]]))

;; =============================================================================
;; Accordion Component
;; =============================================================================

(hy/defelem accordion-item
  [:map {:doc "Single collapsible accordion item with header and content"
         :as attrs}
   [:accordion-item/title :string]
   [:accordion-item/icon {:optional true} [:maybe :string]]
   [:accordion-item/index :int]
   [:accordion-item/default-open {:optional true} [:maybe :boolean]]]
  (let [{:accordion-item/keys [title icon index default-open]} attrs
        signal-name (str "accordionOpen" index)]
    [:div.accordion-item attrs
     [:button.accordion-item-header
      {:data-signals (str "{" signal-name ": " (if default-open "true" "false") "}")
       :data-on:click (str "$" signal-name " = !$" signal-name)}
      [:div.accordion-item-icon-wrapper
       (when icon [:span.accordion-item-icon icon])]
      [:span.accordion-item-title title]
      [:span.accordion-item-chevron
       {:data-class (str "{\"accordion-item-chevron--open\": $" signal-name "}")}
       "▶"]]
     [:div.accordion-item-content
      {:data-class (str "{\"accordion-item-content--open\": $" signal-name "}")}
      [:div.accordion-item-content-inner
       (hy/children)]]]))

(hy/defelem accordion
  [:map {:doc "Cyberpunk-styled accordion container for collapsible sections"
         :as attrs}
   [:accordion/variant {:optional true} [:maybe [:enum :cyan :magenta :pink]]]]
  (let [{:accordion/keys [variant]} attrs
        variant-class (when variant (str "accordion--" (name variant)))]
    [:div.accordion {:class variant-class}
     [:div.accordion-corner.accordion-corner--tl-h]
     [:div.accordion-corner.accordion-corner--tl-v]
     [:div.accordion-corner.accordion-corner--br-h]
     [:div.accordion-corner.accordion-corner--br-v]
     [:div.scanline-overlay]
     (hy/children)]))

;; =============================================================================
;; Event Modal Component
;; =============================================================================

(hy/defelem event-modal
  [:map {:doc "Modal dialog for game events with icon, message, and action buttons"
         :as attrs}
   [:event-modal/title :string]
   [:event-modal/subtitle {:optional true} [:maybe :string]]
   [:event-modal/icon :string]
   [:event-modal/message :string]
   [:event-modal/actions {:optional true} [:maybe [:vector [:map
                                                            [:label :string]
                                                            [:primary? {:optional true} :boolean]]]]]
   [:event-modal/variant {:optional true} [:enum :info :warning :success :error]]]
  (let [{:event-modal/keys [title subtitle icon message actions variant]} attrs
        variant-class (str "event-modal--" (clojure.core/name (or variant :info)))]
    [:div.event-modal {:class variant-class}
     [:div.event-modal-backdrop]
     [:div.event-modal-container.clip-corners-lg
      ;; Corner accents
      [:div.event-modal-corner.event-modal-corner--tl-h]
      [:div.event-modal-corner.event-modal-corner--tl-v]
      [:div.event-modal-corner.event-modal-corner--br-h]
      [:div.event-modal-corner.event-modal-corner--br-v]
      ;; Scanline effect
      [:div.scanline-overlay]
      ;; Header
      [:div.event-modal-header
       [:div.event-modal-icon icon]
       [:div.event-modal-titles
        [:div.event-modal-title title]
        (when subtitle
          [:div.event-modal-subtitle subtitle])]]
      ;; Body
      [:div.event-modal-body
       [:div.event-modal-message message]]
      ;; Actions
      (when (seq actions)
        [:div.event-modal-actions
         (for [{:keys [label primary?]} actions]
           [:button.event-modal-btn
            {:class (when primary? "event-modal-btn--primary")}
            label])])]]))

;; =============================================================================
;; Status Indicator Component
;; =============================================================================

(hy/defelem status-indicator
  [:map {:doc "Glowing status indicator with pulsing animation.
               Use for showing system status, connection state, or health indicators.
               Supports online/offline/warning/error states with matching colors."
         :as attrs}
   [:status-indicator/label :string]
   [:status-indicator/status [:enum :online :offline :warning :error]]
   [:status-indicator/pulse {:optional true} [:maybe :boolean]]]
  (let [{:status-indicator/keys [label status pulse]} attrs
        status-class (str "status-indicator--" (name status))
        pulse-class (when (not= false pulse) "status-indicator--pulse")]
    [:div.status-indicator {:class [status-class pulse-class]}
     [:div.status-indicator-light]
     [:span.status-indicator-label label]]))
