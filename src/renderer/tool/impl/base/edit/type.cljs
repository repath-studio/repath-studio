(ns renderer.tool.impl.base.edit.type
  (:require
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defmethod tool.hierarchy/help [::edit :type]
  []
  (i18n.views/t [::help-type "Enter your text."]))
