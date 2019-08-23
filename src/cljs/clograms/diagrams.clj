(ns clograms.diagrams)


(defmacro def-storm-custom-node [node-type-name & args]
  (let [fns-map (into {} (map vec (partition 2 args)))
        node-model-builder-name (first (:node-model-builder fns-map))
        node-model-builder-args (second (:node-model-builder fns-map))
        node-factory-builder-name (first (:node-factory-builder fns-map))
        node-model-obj (gensym)
        event-symb (gensym)]
    `(do
       (defn ~node-model-builder-name ~node-model-builder-args
         (let [~node-model-obj (js/Reflect.construct ~(:node-model-base fns-map)  (cljs.core/clj->js [{:type ~node-type-name}]) ~node-model-builder-name)]
           ~@(for [arg node-model-builder-args]
               `(set! (~(symbol (str ".-" arg)) ~node-model-obj) ~arg))
           ~node-model-obj))

       (js/Reflect.setPrototypeOf (.-prototype ~node-model-builder-name) (.-prototype ~(:node-model-base fns-map)))
       (js/Reflect.setPrototypeOf ~node-model-builder-name ~(:node-model-base fns-map))


       (defn ~node-factory-builder-name []
         (js/Reflect.construct ~(:node-factory-base fns-map) (cljs.core/clj->js [~node-type-name]) ~node-factory-builder-name))

       (set! (-> ~node-factory-builder-name .-prototype .-generateModel) (fn [] (~node-model-builder-name nil nil)))
       (set! (-> ~node-factory-builder-name .-prototype .-generateReactWidget) (fn [~event-symb]
                                                                                 (reagent.core/create-element
                                                                                  (reagent.core/reactify-component ~(:render fns-map))
                                                                                  (cljs.core/clj->js ~(->> node-model-builder-args
                                                                                                           (map (fn [arg]
                                                                                                                  [(keyword arg) `(~(symbol (str ".-" arg)) (.-model ~event-symb))]))
                                                                                                           (into {}))))))

       (js/Reflect.setPrototypeOf (.-prototype ~node-factory-builder-name)  (.-prototype ~(:node-factory-base fns-map)))
       (js/Reflect.setPrototypeOf ~node-factory-builder-name ~(:node-factory-base fns-map)))))
