(ns sandbox.system)

(def ^:dynamic *system*
  "Dynamic var holding the running system.
   Contains :store, :dispatch, and :server keys."
  nil)
