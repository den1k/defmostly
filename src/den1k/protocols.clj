(ns den1k.protocols)

; https://nelsonmorris.net/2015/05/18/reloaded-protocol-and-no-implementation-of-method.html

(defprotocol MultiFn
  (add-method [this pattern fn])
  )
