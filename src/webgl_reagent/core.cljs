(ns webgl-reagent.core
    (:require ))

(enable-console-print!)

(println "This text is printed from src/webgl-reagent/core.cljs. Go ahead and edit it and see reloading in action.")

(def vertex-shader-src
  "attribute vec4 aVertexPosition;

   uniform mat4 uModelViewMatrix;
   uniform mat4 uProjectionMatrix;

   void main() {
     gl_Position = uProjectionMatrix * uModelViewMatrix * aVertexPosition;
   }")

(def fragment-shader-src
  "void main() {
      gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
   }")

(defn load-shader [gl type source]
  (let [shader (.createShader gl type)]
    (doto gl
      (.shaderSource shader source)
      (.compileShader shader))
    shader))

(defn init-shader-program [gl vs-source fs-source]
  (let [vertex-shader (load-shader gl (.-VERTEX_SHADER gl) vs-source)
        fragment-shader (load-shader gl (.-FRAGMENT_SHADER gl) fs-source)
        shader-program (.createProgram gl)]
    (doto gl
      (.attachShader shader-program vertex-shader)
      (.attachShader shader-program fragment-shader)
      (.linkProgram shader-program))
    shader-program))

(defn init-buffers [gl]
  (let [position-buffer (.createBuffer gl)
        positions #js [1.0 1.0
                       -1.0 1.0
                       1.0 -1.0
                       -1.0 -1.0]]
    (.bindBuffer gl (.-ARRAY_BUFFER gl) position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl)
                 (js/Float32Array. positions)
                 (.-STATIC_DRAW gl))

    {:position position-buffer}))

(defn draw-scene [gl program-info buffers]
  (let [field-of-view (* 45 (/ (.-PI js/Math) 180))
        aspect (/ (-> gl .-canvas .-clientWidth)
                  (-> gl .-canvas .-clientHeight))
        z-near 0.1
        z-far 100.0

        projection-matrix (.create js/mat4)
        model-view-matrix (.create js/mat4)

        num-components 2
        type (.-FLOAT gl)
        normalize false
        stride 0
        offset 0

        ]
    (doto gl
      (.clearColor 0.0 0.0 0.0 1.0)
      (.clearDepth 1.0)
      (.enable (.-DEPTH_TEST gl))
      (.depthFunc (.-LEQUAL gl))
      (.clear (bit-or (.-COLOR_BUFFER_BIT gl)
                      (.-DEPTH_BUFFER_BIT gl))))

    (.perspective js/mat4
                  projection-matrix
                  field-of-view
                  aspect
                  z-near
                  z-far)

    (.translate js/mat4
                model-view-matrix
                model-view-matrix
                #js [-0.0 0.0 -6.0])

    (doto gl
      (.bindBuffer (.-ARRAY_BUFFER gl) (:position buffers))
      (.vertexAttribPointer (-> program-info :attribLocations :vertexPosition)
                            num-components
                            type
                            normalize
                            stride
                            offset)
      (.enableVertexAttribArray (-> program-info :attribLocations :vertexPosition)))

    (doto gl
      (.useProgram (:program program-info))
      (.uniformMatrix4fv (-> program-info :uniformLocations :projectionMatrix)
                         false
                         projection-matrix)
      (.uniformMatrix4fv (-> program-info :uniformLocations :modelViewMatrix)
                         false
                         model-view-matrix)
      )


    (.drawArrays gl (.-TRIANGLE_STRIP gl) 0 4)

    ))

(defn webgl-run! []
  (let [gl (-> (.querySelector js/document "#glCanvas")
               (.getContext "webgl"))
        shader-program (init-shader-program gl vertex-shader-src fragment-shader-src)
        program-info {:program shader-program
                      :attribLocations {:vertexPosition (.getAttribLocation gl shader-program "aVertexPosition")}
                      :uniformLocations {:projectionMatrix (.getUniformLocation gl shader-program "uProjectionMatrix")
                                         :modelViewMatrix (.getUniformLocation gl shader-program "uModelViewMatrix")
                                         }}]

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.clear gl (.-COLOR_BUFFER_BIT gl))

    (draw-scene gl program-info (init-buffers gl))))

(webgl-run!)
;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
