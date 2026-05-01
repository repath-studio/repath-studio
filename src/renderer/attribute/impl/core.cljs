(ns renderer.attribute.impl.core
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.impl.color]
   [renderer.attribute.impl.crossorigin]
   [renderer.attribute.impl.d]
   [renderer.attribute.impl.decoding]
   [renderer.attribute.impl.font-family]
   [renderer.attribute.impl.font-size]
   [renderer.attribute.impl.font-style]
   [renderer.attribute.impl.font-weight]
   [renderer.attribute.impl.href]
   [renderer.attribute.impl.length]
   [renderer.attribute.impl.overflow]
   [renderer.attribute.impl.points]
   [renderer.attribute.impl.range]
   [renderer.attribute.impl.stroke-linecap]
   [renderer.attribute.impl.stroke-linejoin]
   [renderer.attribute.impl.style]
   [renderer.attribute.impl.transform]
   [renderer.element.hierarchy :as-alias element.hierarchy]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element :x]
  []
  [::x "The x attribute defines an x-axis coordinate in the user coordinate
        system."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :y]
  []
  [::y "The y attribute defines a y-axis coordinate in the user coordinate
        system."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :x1]
  []
  [::x1 "The x1 attribute is used to specify the first x-coordinate for drawing
         an SVG element that requires more than one coordinate. Elements that
         only need one coordinate use the x attribute instead."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :y1]
  []
  [::y1 "The y1 attribute is used to specify the first y-coordinate for drawing
         an SVG element that requires more than one coordinate. Elements that
         only need one coordinate use the y attribute instead."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :x2]
  []
  [::x2 "The x2 attribute is used to specify the second x-coordinate for drawing
         an SVG element that requires more than one coordinate. Elements that
         only need one coordinate use the x attribute instead."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :y2]
  []
  [::y2 "The y2 attribute is used to specify the second y-coordinate for drawing
         an SVG element that requires more than one coordinate. Elements that
         only need one coordinate use the y attribute instead."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :cx]
  []
  [::cx "The cx attribute define the x-axis coordinate of a center point."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :cy]
  []
  [::cy "The cy attribute define the y-axis coordinate of a center point."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :dx]
  []
  [::dx "The dx attribute indicates a shift along the x-axis on the position of
         an element or its content."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :dy]
  []
  [::dy "The dy attribute indicates a shift along the y-axis on the position of
         an element or its content."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :width]
  []
  [::width "The width attribute defines the horizontal length of an element in
            the user coordinate system."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :height]
  []
  [::height "The height attribute defines the vertical length of an element in
             the user coordinate system."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :rx]
  []
  [::rx "The rx attribute defines a radius on the x-axis."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :ry]
  []
  [::ry "The ry attribute defines a radius on the y-axis."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :r]
  []
  [::r "The r attribute defines the radius of a circle."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :rotate]
  []
  [::rotate "The rotate attribute specifies how the animated element rotates as
             it travels along a path specified in an <animateMotion> element."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :stroke]
  []
  [::stroke "The stroke attribute is a presentation attribute defining the color
             (or any SVG paint servers like gradients or patterns) used to paint
             the outline of the shape."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :fill]
  []
  [::fill "The fill attribute has two different meanings. For shapes and text
           it's a presentation attribute that defines the color (or any SVG
           paint servers like gradients or patterns) used to paint the element;
           for animation it defines the final state of the animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :stroke-width]
  []
  [::stroke-width "The stroke-width attribute is a presentation attribute
                   defining the width of the stroke to be applied to the
                   shape."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :stroke-dasharray]
  []
  [::stroke-dasharray "The stroke-dasharray attribute is a presentation
                       attribute defining the  pattern of dashes and gaps used
                       to paint the outline of the shape."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :opacity]
  []
  [::opacity "The opacity attribute specifies the transparency of an object or
              of a group of objects, that is, the degree to which the background
              behind the element is overlaid."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :id]
  []
  [::id "The id attribute assigns a unique name to an element."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :class]
  []
  [::class "Assigns a class name or set of class names to an element. You may
            assign the same class name or names to any number of elements,
            however, multiple class names must be separated by whitespace
            characters."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :tabindex]
  []
  [::tabindex "The tabindex attribute allows you to control whether an element
               is focusable and to define the relative order of the element for
               the purposes of sequential focus navigation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :style]
  []
  [::style "The style attribute allows to style an element using CSS
            declarations. It functions identically to the style attribute in
            HTML."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :href]
  []
  [::href "The href attribute defines a link to a resource as a reference URL.
           The exact meaning of that link depends on the context of each element
           using it."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :attributeName]
  []
  [::attribute-name "The attributeName attribute indicates the name of the CSS
                     property or attribute of the target element that is going
                     to be changed during an animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :begin]
  []
  [::begin "The begin attribute defines when an animation should begin."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :end]
  []
  [::end
   "The end attribute defines an end value for the animation that can constrain
    the active duration."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :dur]
  []
  [::dur "The dur attribute indicates the simple duration of an animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :min]
  []
  [::min "The min attribute specifies the minimum value of the active animation
          duration."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :max]
  []
  [::max "The max attribute specifies the maximum value of the active animation
          duration."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :restart]
  []
  [::restart "The restart attribute specifies whether or not an animation can
              restart."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :repeatCount]
  []
  [::repeat-count "The repeatCount attribute indicates the number of times an
                   animation will take place."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :repeatDur]
  []
  [::repeat-dur "The repeatDur attribute specifies the total duration for
                 repeating an animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :calcMode]
  []
  [::calc-mode "The calcMode attribute specifies the interpolation mode for the
                animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :values]
  []
  [::values "The values attribute has different meanings, depending upon the
             context where it's used, either it defines a sequence of values
             used over the course of an animation, or it's a list of numbers for
             a color matrix, which is interpreted differently depending on the
             type of color change to be performed."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :keyTimes]
  []
  [::key-times "The keyTimes attribute represents a list of time values used to
                control the pacing of the animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :keySplines]
  []
  [::key-splines "The keySplines attribute defines a set of Bézier curve control
                  points associated with the keyTimes list, defining a cubic
                  Bézier function that controls interval pacing"])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :from]
  []
  [::from "The from attribute indicates the initial value of the attribute that
           will be modified during the animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :to]
  []
  [::to "The to attribute indicates the final value of the attribute that will
         be modified during the animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element :by]
  []
  [::by "The by attribute specifies a relative offset value for an attribute
         that will be modified during an animation."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :additive]
  []
  [::additive "The additive attribute controls whether or not an animation is
               additive."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :accumulate]
  []
  [::accumulate "The accumulate attribute controls whether or not an animation
                 is cumulative."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :viewBox]
  []
  [::view-box "The viewBox attribute defines the position and dimension, in
               user space, of an SVG viewport."])

(defmethod attribute.hierarchy/description [::element.hierarchy/element
                                            :preserveAspectRatio]
  []
  [::preserve-aspect-ratio "The preserveAspectRatio attribute indicates how an
                            element with a viewBox providing a given aspect
                            ratio must fit into a viewport with a different
                            aspect ratio."])
