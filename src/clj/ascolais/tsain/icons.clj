(ns ascolais.tsain.icons
  "Lucide icons as hiccup vectors for the sandbox UI.

  Usage:
    (require '[ascolais.tsain.icons :as icons])

    ;; Get icon hiccup
    (icons/icon :copy)
    (icons/icon :copy {:width 20 :height 20})

  All icons use stroke=\"currentColor\" so they inherit text color.")

(def ^:private base-attrs
  {:xmlns "http://www.w3.org/2000/svg"
   :width 16
   :height 16
   :viewBox "0 0 24 24"
   :fill "none"
   :stroke "currentColor"
   :stroke-width 2
   :stroke-linecap "round"
   :stroke-linejoin "round"})

(def icons
  {:chevron-left
   [[:path {:d "m15 18-6-6 6-6"}]]

   :chevron-right
   [[:path {:d "m9 18 6-6-6-6"}]]

   :chevrons-left
   [[:path {:d "m11 17-5-5 5-5"}]
    [:path {:d "m18 17-5-5 5-5"}]]

   :chevrons-right
   [[:path {:d "m6 17 5-5-5-5"}]
    [:path {:d "m13 17 5-5-5-5"}]]

   :copy
   [[:rect {:x 8 :y 8 :width 14 :height 14 :rx 2 :ry 2}]
    [:path {:d "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"}]]

   :trash-2
   [[:path {:d "M3 6h18"}]
    [:path {:d "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"}]
    [:path {:d "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"}]
    [:path {:d "M10 11v6"}]
    [:path {:d "M14 11v6"}]]

   :save
   [[:path {:d "M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"}]
    [:path {:d "M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7"}]
    [:path {:d "M7 3v4a1 1 0 0 0 1 1h7"}]]

   :x
   [[:path {:d "M18 6 6 18"}]
    [:path {:d "m6 6 12 12"}]]

   :search
   [[:circle {:cx 11 :cy 11 :r 8}]
    [:path {:d "m21 21-4.34-4.34"}]]

   :palette
   [[:path {:d "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z"}]
    [:circle {:cx 13.5 :cy 6.5 :r 0.5 :fill "currentColor"}]
    [:circle {:cx 17.5 :cy 10.5 :r 0.5 :fill "currentColor"}]
    [:circle {:cx 6.5 :cy 12.5 :r 0.5 :fill "currentColor"}]
    [:circle {:cx 8.5 :cy 7.5 :r 0.5 :fill "currentColor"}]]

   :check
   [[:path {:d "M20 6 9 17l-5-5"}]]

   :eye
   [[:path {:d "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0"}]
    [:circle {:cx 12 :cy 12 :r 3}]]

   :grid-3x3
   [[:rect {:width 18 :height 18 :x 3 :y 3 :rx 2}]
    [:path {:d "M3 9h18"}]
    [:path {:d "M3 15h18"}]
    [:path {:d "M9 3v18"}]
    [:path {:d "M15 3v18"}]]})

(defn icon
  "Get icon hiccup by name. Optionally override attrs.

  (icon :copy)
  (icon :copy {:width 20 :height 20})
  (icon :copy {:class \"my-icon\"})"
  ([icon-name]
   (icon icon-name nil))
  ([icon-name opts]
   (when-let [children (get icons icon-name)]
     (into [:svg (merge base-attrs opts)] children))))
