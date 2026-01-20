(ns sandbox.ui
  "Chassis alias definitions for sandbox components.

   ## Alias-First Development

   Component structure lives here as chassis aliases. The `components.edn`
   file stores lean alias invocations with config props, not verbose hiccup.

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

   ;; Renders to full HTML with structure + config interpolated
   ```"
  (:require [dev.onionpancakes.chassis.core :as c]))

;; =============================================================================
;; Game Card Component
;; =============================================================================

(defmethod c/resolve-alias ::game-card
  [_ attrs _]
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

(defmethod c/resolve-alias ::combat-log-entry
  [_ attrs _]
  (let [{:combat-log-entry/keys [time message detail detail-type highlight]} attrs
        entry-class (when highlight (str "combat-log-entry--highlight-" (name highlight)))]
    [:div.combat-log-entry {:class entry-class}
     [:span.combat-log-time time]
     [:div
      [:div.combat-log-message message]
      (when detail
        [:div.combat-log-detail {:class (when detail-type (str "combat-log-" (name detail-type)))} detail])]]))

(defmethod c/resolve-alias ::combat-log
  [_ attrs _]
  (let [{:combat-log/keys [turn entries]} attrs]
    [:div.combat-log attrs
     [:div.scanline-overlay]
     [:div.combat-log-header
      [:span.combat-log-title "◆ Combat Log"]
      [:span.combat-log-turn (str "TURN " turn)]]
     [:div.combat-log-entries
      (for [{:keys [time message detail detail-type highlight]} entries]
        [::combat-log-entry
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

(defmethod c/resolve-alias ::badge
  [_ attrs _]
  (let [{:badge/keys [icon label type]} attrs]
    [:div.badge.clip-badge {:class (str "badge--" (name type))}
     [:span.badge-icon icon]
     [:span.badge-label label]]))

(defmethod c/resolve-alias ::badge-rarity
  [_ attrs _]
  (let [{:badge-rarity/keys [label rarity]} attrs
        dot-class (if (= rarity :legendary) "clip-diamond" nil)]
    [:div.badge-rarity {:class (str "badge-rarity--" (name rarity))}
     [:div.badge-rarity-dot {:class dot-class}]
     [:span.badge-rarity-label label]]))

(defmethod c/resolve-alias ::card-type-badges
  [_ attrs _]
  (let [{:card-type-badges/keys [badges rarities]} attrs]
    [:div attrs
     (for [{:keys [icon label type]} badges]
       [::badge {:badge/icon icon :badge/label label :badge/type type}])
     [:div {:style "margin-top: 16px; width: 100%; display: flex; gap: 12px;"}
      (for [{:keys [label rarity]} rarities]
        [::badge-rarity {:badge-rarity/label label :badge-rarity/rarity rarity}])]]))

;; =============================================================================
;; Player HUD Component
;; =============================================================================

(defmethod c/resolve-alias ::player-hud-bar
  [_ attrs _]
  (let [{:player-hud-bar/keys [label value percent type]} attrs]
    [:div.player-hud-bar
     [:div.player-hud-bar-header
      [:span.player-hud-bar-label {:class (str "player-hud-bar-label--" (name type))} label]
      [:span.player-hud-bar-value {:class (str "player-hud-bar-value--" (name type))} value]]
     [:div.player-hud-bar-track {:class (str "player-hud-bar-track--" (name type))}
      [:div.player-hud-bar-fill {:class (str "player-hud-bar-fill--" (name type))
                                 :style (str "width: " percent ";")}]
      [:div.player-hud-bar-segments]]]))

(defmethod c/resolve-alias ::player-hud-counter
  [_ attrs _]
  (let [{:player-hud-counter/keys [label value type]} attrs]
    [:div.player-hud-counter
     [:div.player-hud-counter-label label]
     [:div.player-hud-counter-value {:class (str "player-hud-counter-value--" (name type))} value]]))

(defmethod c/resolve-alias ::player-hud
  [_ attrs _]
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
     [::player-hud-bar {:player-hud-bar/label "◆ INTEGRITY"
                        :player-hud-bar/value health-value
                        :player-hud-bar/percent (:percent health)
                        :player-hud-bar/type :health}]
     [::player-hud-bar {:player-hud-bar/label "◆ ENERGY"
                        :player-hud-bar/value energy-value
                        :player-hud-bar/percent (:percent energy-data)
                        :player-hud-bar/type :energy}]
     [:div.player-hud-counters
      (for [{:keys [label value type]} counters]
        [::player-hud-counter {:player-hud-counter/label label
                               :player-hud-counter/value value
                               :player-hud-counter/type type}])]]))

;; =============================================================================
;; Action Buttons Component
;; =============================================================================

(defmethod c/resolve-alias ::btn
  [_ attrs content]
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
     content]))

(defmethod c/resolve-alias ::action-buttons
  [_ attrs _]
  (let [{:action-buttons/keys [primary secondary small]} attrs]
    [:div attrs
     (for [{:keys [label icon]} primary]
       [::btn {:btn/variant :primary} (str icon " " label)])
     (for [{:keys [label icon color]} secondary]
       [::btn {:btn/variant :secondary :btn/color color} (str icon " " label)])
     (when (seq small)
       [:div {:style "display: flex; gap: 8px; margin-top: 16px;"}
        (for [{:keys [label color]} small]
          [::btn {:btn/size :small :btn/color color} label])])]))

;; =============================================================================
;; Resource Display Component
;; =============================================================================

(defmethod c/resolve-alias ::energy-orbs
  [_ attrs _]
  (let [{:energy-orbs/keys [active total]} attrs]
    [:div.energy-orbs
     (for [i (range total)]
       [:div.energy-orb {:class (if (< i active) "energy-orb--active" "energy-orb--empty")}])]))

(defmethod c/resolve-alias ::credit-chips
  [_ attrs _]
  (let [{:credit-chips/keys [active total]} attrs]
    [:div.credits-chips
     (for [i (range total)]
       [:div.credit-chip.clip-hexagon {:class (if (< i active) "credit-chip--active" "credit-chip--empty")}])]))

(defmethod c/resolve-alias ::resource-display
  [_ attrs _]
  (let [{:resource-display/keys [energy credits]} attrs
        {:keys [active total]} energy
        {:keys [active total value] :as credits-data} credits]
    [:div attrs
     [:div.resource-panel.resource-panel--energy
      [:div.resource-label.resource-label--energy "◆ Available Energy"]
      [::energy-orbs {:energy-orbs/active (:active energy) :energy-orbs/total (:total energy)}]]
     [:div.resource-panel.resource-panel--credits
      [:div.credits-header
       [:span.resource-label.resource-label--credits "◆ Credits"]
       [:span.credits-value (str "₵ " (:value credits-data))]]
      [::credit-chips {:credit-chips/active (:active credits-data) :credit-chips/total (:total credits-data)}]]]))

;; =============================================================================
;; Toast Component
;; =============================================================================

(defmethod c/resolve-alias ::toast
  [_ attrs _]
  (let [{:toast/keys [variant label message icon]} attrs
        variant-class (str "toast--" (name (or variant :info)))]
    [:div.toast {:class variant-class}
     [:div.toast-accent]
     [:div.toast-icon icon]
     [:div.toast-content
      [:div.toast-label (str "◆ " label)]
      [:div.toast-message message]]
     [:button.toast-dismiss "✕"]]))