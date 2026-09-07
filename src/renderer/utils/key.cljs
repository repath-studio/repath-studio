(ns renderer.utils.key
  (:require
   [clojure.set :as set]
   [malli.core :as m]))

(m/=> arrow? [:-> string? boolean?])
(defn arrow?
  [k]
  (contains? #{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"} k))

(defn down-handler
  "Generic on-key-down handler for uncontrolled input elements, that dispatches
   an event `f` in order to update a db value on keyboard enter, or reset to the
   initial value `v` on escape.

   We need uncontrolled inputs to avoid updating the canvas with incomplete
   values while the user is typing, and also avoid polluting the history stack.

   The `default-value` attribute should be used to update the value reactively."
  [e v f & more]
  (let [target (.-target e)]
    (.stopPropagation e)

    (case (.-key e)
      "Enter" (do (apply f e more)
                  (.blur target))
      "Escape" (do (set! (.-value target) v)
                   (.blur target))
      nil)))

;; Copied from https://google.github.io/closure-library/api/goog.events.KeyCodes.html
;; https://github.com/google/closure-library, Apache License 2.0
;; We cannot read `goog.events.KeyCodes` at runtime with `:advanced`
;; optimizations, because Closure Compiler renames the properties.
(def codes
  {"WIN_KEY_FF_LINUX" 0
   "MAC_ENTER" 3
   "BACKSPACE" 8
   "TAB" 9
   "NUM_CENTER" 12
   "ENTER" 13
   "SHIFT" 16
   "CTRL" 17
   "ALT" 18
   "PAUSE" 19
   "CAPS_LOCK" 20
   "ESC" 27
   "SPACE" 32
   "PAGE_UP" 33
   "PAGE_DOWN" 34
   "END" 35
   "HOME" 36
   "LEFT" 37
   "UP" 38
   "RIGHT" 39
   "DOWN" 40
   "PLUS_SIGN" 43
   "PRINT_SCREEN" 44
   "INSERT" 45
   "DELETE" 46
   "ZERO" 48
   "ONE" 49
   "TWO" 50
   "THREE" 51
   "FOUR" 52
   "FIVE" 53
   "SIX" 54
   "SEVEN" 55
   "EIGHT" 56
   "NINE" 57
   "FF_SEMICOLON" 59
   "FF_EQUALS" 61
   "FF_DASH" 173
   "FF_HASH" 163
   "FF_JP_QUOTE" 58
   "FF_DE_PLUS" 171
   "QUESTION_MARK" 63
   "AT_SIGN" 64
   "A" 65
   "B" 66
   "C" 67
   "D" 68
   "E" 69
   "F" 70
   "G" 71
   "H" 72
   "I" 73
   "J" 74
   "K" 75
   "L" 76
   "M" 77
   "N" 78
   "O" 79
   "P" 80
   "Q" 81
   "R" 82
   "S" 83
   "T" 84
   "U" 85
   "V" 86
   "W" 87
   "X" 88
   "Y" 89
   "Z" 90
   "META" 91
   "WIN_KEY_RIGHT" 92
   "CONTEXT_MENU" 93
   "NUM_ZERO" 96
   "NUM_ONE" 97
   "NUM_TWO" 98
   "NUM_THREE" 99
   "NUM_FOUR" 100
   "NUM_FIVE" 101
   "NUM_SIX" 102
   "NUM_SEVEN" 103
   "NUM_EIGHT" 104
   "NUM_NINE" 105
   "NUM_MULTIPLY" 106
   "NUM_PLUS" 107
   "NUM_MINUS" 109
   "NUM_PERIOD" 110
   "NUM_DIVISION" 111
   "F1" 112
   "F2" 113
   "F3" 114
   "F4" 115
   "F5" 116
   "F6" 117
   "F7" 118
   "F8" 119
   "F9" 120
   "F10" 121
   "F11" 122
   "F12" 123
   "NUMLOCK" 144
   "SCROLL_LOCK" 145
   "FIRST_MEDIA_KEY" 166
   "LAST_MEDIA_KEY" 183
   "SEMICOLON" 186
   "DASH" 189
   "EQUALS" 187
   "COMMA" 188
   "PERIOD" 190
   "SLASH" 191
   "APOSTROPHE" 192
   "TILDE" 192
   "SINGLE_QUOTE" 222
   "OPEN_SQUARE_BRACKET" 219
   "BACKSLASH" 220
   "CLOSE_SQUARE_BRACKET" 221
   "WIN_KEY" 224
   "MAC_FF_META" 224
   "MAC_WK_CMD_LEFT" 91
   "MAC_WK_CMD_RIGHT" 93
   "WIN_IME" 229
   "VK_NONAME" 252
   "PHANTOM" 255})

(def key-chars
  (set/map-invert codes))

(m/=> code->key [:-> number? [:maybe string?]])
(defn code->key
  [key-code]
  (get key-chars key-code))
