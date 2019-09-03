(ns clograms.diagrams)


(defmacro def-storm-custom-node [node-type-name & args]
  (let [fns-map (into {} (map vec (partition 2 args)))
        node-model-builder-name (:node-model-builder fns-map)
        node-factory-builder-name (:node-factory-builder fns-map)
        node-model-obj (gensym)
        event-symb (gensym)
        entity-symb (gensym)]

    `(do
       (defn ~node-model-builder-name [~entity-symb]
         (let [~node-model-obj (js/Reflect.construct ~(:node-model-base fns-map)  (cljs.core/clj->js [{:type ~node-type-name :color "blue"}]) ~node-model-builder-name)]
           (set! (.-entity ~node-model-obj) ~entity-symb)
           ~node-model-obj))

       (js/Reflect.setPrototypeOf (.-prototype ~node-model-builder-name) (.-prototype ~(:node-model-base fns-map)))
       (js/Reflect.setPrototypeOf ~node-model-builder-name ~(:node-model-base fns-map))


       (defn ~node-factory-builder-name []
         (js/Reflect.construct ~(:node-factory-base fns-map) (cljs.core/clj->js [~node-type-name]) ~node-factory-builder-name))

       (set! (-> ~node-factory-builder-name .-prototype .-generateModel) (fn [] (~node-model-builder-name nil)))
       (set! (-> ~node-factory-builder-name .-prototype .-generateReactWidget) (fn [~event-symb]
                                                                                 (reagent.core/create-element
                                                                                  (reagent.core/reactify-component ~(:render fns-map))
                                                                                  (cljs.core/js-obj
                                                                                   "node" (.-model ~event-symb)
                                                                                   "engine" (:storm/engine @clograms.diagrams/storm-atom)))))

       (js/Reflect.setPrototypeOf (.-prototype ~node-factory-builder-name)  (.-prototype ~(:node-factory-base fns-map)))
       (js/Reflect.setPrototypeOf ~node-factory-builder-name ~(:node-factory-base fns-map)))
    ))
