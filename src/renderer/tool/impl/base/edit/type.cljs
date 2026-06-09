(ns renderer.tool.impl.base.edit.type
  (:require
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.core :as-alias edit]))

(defmethod tool.hierarchy/help [::edit/edit :type]
  []
  (i18n.views/t [::help-type "Enter your text."]))
