(ns webgl-reagent.core
    (:require ))

(enable-console-print!)

(println "This text is printed from src/webgl-reagent/core.cljs. Go ahead and edit it and see reloading in action.")

(def vertex-shader-src
  " attribute vec4 aVertexPosition;
    attribute vec2 aTextureCoord;

    uniform mat4 uModelViewMatrix;
    uniform mat4 uProjectionMatrix;

    varying highp vec2 vTextureCoord;

    void main(void) {
      gl_Position = uProjectionMatrix * uModelViewMatrix * aVertexPosition;
      vTextureCoord = aTextureCoord;
    }")

#_(def fragment-shader-src
  "varying highp vec2 vTextureCoord;

   uniform sampler2D uSampler;

   void main(void) {
      gl_FragColor = texture2D(uSampler, vTextureCoord);
    }")

(def fragment-shader-src
  "precision mediump float;
  uniform sampler2D uSampler;
  uniform vec4 u_color;
  uniform float u_buffer;
  uniform float u_gamma;
  varying vec2 vTextureCoord;
  void main() {
  float dist = texture2D(uSampler, vTextureCoord).r;
  float alpha = smoothstep(u_buffer - u_gamma, u_buffer + u_gamma, dist);
  gl_FragColor = vec4(u_color.rgb, alpha * u_color.a);
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

    (if-not (.getProgramParameter gl shader-program (.-LINK_STATUS gl))
      (.error js/console (str "ERR: " (.getProgramInfoLog gl shader-program)))
      shader-program)
    ))

(defn init-buffers [gl]
  (let [position-buffer (.createBuffer gl)
        texture-coord-buffer (.createBuffer gl)
        index-buffer (.createBuffer gl)
        positions #js   [
                         ;; Front face
                         -1.0, -1.0,  1.0,
                         1.0, -1.0,  1.0,
                         1.0,  1.0,  1.0,
                         -1.0,  1.0,  1.0,

                         ;; Back face
                         -1.0, -1.0, -1.0,
                         -1.0,  1.0, -1.0,
                         1.0,  1.0, -1.0,
                         1.0, -1.0, -1.0,

                         ;; Top face
                         -1.0,  1.0, -1.0,
                         -1.0,  1.0,  1.0,
                         1.0,  1.0,  1.0,
                         1.0,  1.0, -1.0,

                         ;; Bottom face
                         -1.0, -1.0, -1.0,
                         1.0, -1.0, -1.0,
                         1.0, -1.0,  1.0,
                         -1.0, -1.0,  1.0,

                         ;; Right face
                         1.0, -1.0, -1.0,
                         1.0,  1.0, -1.0,
                         1.0,  1.0,  1.0,
                         1.0, -1.0,  1.0,

                         ;; Left face
                         -1.0, -1.0, -1.0,
                         -1.0, -1.0,  1.0,
                         -1.0,  1.0,  1.0,
                         -1.0,  1.0, -1.0,
                         ]
        texture-coordinates #js [
                                 ;; Front
                                 0.0,  0.0,
                                 1.0,  0.0,
                                 1.0,  1.0,
                                 0.0,  1.0,
                                 ;; Back
                                 0.0,  0.0,
                                 1.0,  0.0,
                                 1.0,  1.0,
                                 0.0,  1.0,
                                 ;; Top
                                 0.0,  0.0,
                                 1.0,  0.0,
                                 1.0,  1.0,
                                 0.0,  1.0,
                                 ;; Bottom
                                 0.0,  0.0,
                                 1.0,  0.0,
                                 1.0,  1.0,
                                 0.0,  1.0,
                                 ;; Right
                                 0.0,  0.0,
                                 1.0,  0.0,
                                 1.0,  1.0,
                                 0.0,  1.0,
                                 ;; Left
                                 0.0,  0.0,
                                 1.0,  0.0,
                                 1.0,  1.0,
                                 0.0,  1.0,
                                 ]

        indices [0,  1,  2,      0,  2,  3, ;; front
                 4,  5,  6,      4,  6,  7, ;; back
                 8,  9,  10,     8,  10, 11, ;; top
                 12, 13, 14,     12, 14, 15, ;; bottom
                 16, 17, 18,     16, 18, 19, ;; right
                 20, 21, 22,     20, 22, 23, ;; left
                 ]
        ]
    (.bindBuffer gl (.-ARRAY_BUFFER gl) position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl)
                 (js/Float32Array. positions)
                 (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ARRAY_BUFFER gl) texture-coord-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl)
                 (js/Float32Array. texture-coordinates)
                 (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) index-buffer)
    (.bufferData gl (.-ELEMENT_ARRAY_BUFFER gl)
                 (js/Uint16Array. indices)
                 (.-STATIC_DRAW gl))

    {:position position-buffer
     :texture-coord texture-coord-buffer
     :indices index-buffer}))

(defn draw-scene [gl program-info buffers texture]
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

    (.rotate js/mat4
             model-view-matrix
             model-view-matrix
             0.5
             #js [0 0 1])
    (.rotate js/mat4
             model-view-matrix
             model-view-matrix
             0.15
             #js [0 1 0])

    (doto gl
      (.bindBuffer (.-ARRAY_BUFFER gl) (:position buffers))
      (.vertexAttribPointer (-> program-info :attribLocations :vertexPosition)
                            3
                            type
                            normalize
                            stride
                            offset)
      (.enableVertexAttribArray (-> program-info :attribLocations :vertexPosition)))

    (doto gl
      (.bindBuffer (.-ARRAY_BUFFER gl) (:texture-coord buffers))
      (.vertexAttribPointer (-> program-info :attribLocations :textureCoord)
                            2
                            type
                            normalize
                            stride
                            offset)
      (.enableVertexAttribArray (-> program-info :attribLocations :textureCoord)))

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) (:indices buffers))

    (doto gl
      (.useProgram (:program program-info))
      (.uniformMatrix4fv (-> program-info :uniformLocations :projectionMatrix)
                         false
                         projection-matrix)
      (.uniformMatrix4fv (-> program-info :uniformLocations :modelViewMatrix)
                         false
                         model-view-matrix)
      )

    (doto gl
      (.activeTexture (.-TEXTURE0 gl))
      (.bindTexture (.-TEXTURE_2D gl) texture)

      (.uniform1i (-> program-info :uniformLocations :uSampler) 0)

      )



    (.drawElements gl (.-TRIANGLES gl) 36 (.-UNSIGNED_SHORT gl) 0)

    ))


(defn is-power-of-two? [value]
  (= 0 (bit-and value (dec value))))

(defn load-texture [gl tiny-sdf]
  (let [texture (.createTexture gl)
        level 0
        internal-format (.-RGBA gl)
        width 1
        height 1
        border 0
        src-format  (.-RGBA gl)
        src-type (.-UNSIGNED_BYTE gl)
        pixel (js/Uint8Array. #js [255 0 255 255])

        image (js/Image.)
        text-canvas (.-canvas tiny-sdf)

        ]

    (.bindTexture gl (.-TEXTURE_2D gl) texture)
    (.texImage2D gl (.-TEXTURE_2D gl)
                 level internal-format
                 width height border
                 src-format src-type pixel)

    (.addEventListener
       image "load"
     (fn [e]
       (.log js/console "IMAGE LOAD")
       (.bindTexture gl (.-TEXTURE_2D gl) texture)
       (.texImage2D gl (.-TEXTURE_2D gl)
                    level internal-format
                    src-format src-type image)

       (if (and (is-power-of-two? (.-width image))
                (is-power-of-two? (.-height image)))
         (.generateMipmap gl (.-TEXTURE_2D gl))
         (doto gl
           (.texParameteri (.-TEXTURE_2D gl)
                           (.-TEXTURE_WRAP_S gl)
                           (.-CLAMP_TO_EDGE gl))

           (.texParameteri (.-TEXTURE_2D gl)
                           (.-TEXTURE_WRAP_T gl)
                           (.-CLAMP_TO_EDGE gl))

           (.texParameteri (.-TEXTURE_2D gl)
                           (.-TEXTURE_MIN_FILTER gl)
                           (.-LINEAR gl))
           ))

       ))

    (set! (.-src image) (.toDataURL text-canvas))

    texture

    ))


(defn webgl-run! []
  (let [context-2d (-> (.querySelector js/document "#textCanvas")
                       (.getContext "2d"))
        gl (-> (.querySelector js/document "#glCanvas")
               (.getContext "webgl"))
        shader-program (init-shader-program gl vertex-shader-src fragment-shader-src)
        image-url "/img/cubetexture.png"
        ;; image-url "https://upload.wikimedia.org/wikipedia/commons/5/5c/Red-square.gif"
        tiny-sdf (js/TinySDF.)
        _ (.draw tiny-sdf "foobar")
        texture (load-texture gl tiny-sdf)

        program-info {:program shader-program
                      :attribLocations {:vertexPosition (.getAttribLocation gl shader-program "aVertexPosition")
                                        :textureCoord (.getAttribLocation gl shader-program "aTextureCoord")}
                      :uniformLocations {:projectionMatrix (.getUniformLocation gl shader-program "uProjectionMatrix")
                                         :modelViewMatrix (.getUniformLocation gl shader-program "uModelViewMatrix")
                                         :uSampler (.getUniformLocation gl shader-program "uSampler")
                                         }}]

    ;; (set! (.-font context-2d) "30px")
    ;; (.fillText context-2d "HEllo!" 10 50)

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.clear gl (.-COLOR_BUFFER_BIT gl))

    (.setTimeout js/window
                  (fn [_]
                    (draw-scene gl program-info (init-buffers gl) texture))
                  500)))

(webgl-run!)
;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
