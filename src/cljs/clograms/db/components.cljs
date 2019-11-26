(ns clograms.db.components)

(defn accordion-active-item [db accordion-id]
  (get-in db [:clograms.ui.components/general :accordion accordion-id :active-item]))

(defn accordion-activate-item [db accordion-id item-id]
  (assoc-in db [:clograms.ui.components/general :accordion accordion-id :active-item] item-id))

(defn text-edit-modal-create [db event]
  (assoc-in db [:clograms.ui.components/general :text-edit-modal :on-text-set-event] event))

(defn text-edit-modal-event [db]
  (get-in db [:clograms.ui.components/general :text-edit-modal :on-text-set-event]))

(defn text-edit-modal-kill [db]
  (update db :clograms.ui.components/general dissoc :text-edit-modal))
