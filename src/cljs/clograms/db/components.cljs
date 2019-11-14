(ns clograms.db.components)

(defn accordion-active-item [db accordion-id]
  (get-in db [:clograms.ui.components.general :accordion accordion-id :active-item]))

(defn accordion-activate-item [db accordion-id item-id]
  (assoc-in db [:clograms.ui.components.general :accordion accordion-id :active-item] item-id))
