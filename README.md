# Substantiation

> additional proof that something that was believed (some fact or hypothesis or theory) is correct


Substantiation is an opinionated simple nested map validation library:

 * Predicates and description kept separate.
 * Validation description map follows validated input structure.
 * Pure data structures to describe validations. 
 * Composability of validations is trivial.
 * Validation predicates scope is limited (can only access the checked value).
 * High level decisions such as when to activate a group of validations should happen on calling layer.
 * Non strict, only described items checked.
 
[![Build Status](https://travis-ci.org/narkisr/substantiation.png?branch=master)](https://travis-ci.org/narkisr/substantiation)

## Usage

```clojure
  [substantiation "0.0.6"]
```

Description separated from actions:

```clojure
(def machine-validations {
   :machine {
     :ip #{:String :required} :names #{:Vector}
    }
   :vcenter {
    :pool #{:String}
   }})   
```
Basic types validations baked in (part of the library), validations are pure functions:
 
```clojure
(def ^:private base {
  :String (when-not-nil string? "must be a string")
  :Integer  (when-not-nil integer? "must be a integer")
  :Vector  (when-not-nil vector? "must be a vector")
  :Map  (when-not-nil map? "must be a map")
  :Set  (when-not-nil set? "must be a set")
  :Keyword  (when-not-nil keyword? "must be a keyword")
  :sequential  (when-not-nil sequential? "must be sequential")
  :required  (when* empty? "must be present")
  })
```

Adding validations is easy:

```clojure
(validation :level 
  (when-not-nil #{:info :debug :error} "must be either info debug or error"))
```

Composing is trivial:
```clojure
(def v1 {
  :machine {
    :ip #{:String :required} 
    :names #{:Vector}
  }
  :vcenter {
    :pool #{:String}
  }
})

(def v2 {
  :machine {
    :ip #{:String :required} 
    :names #{:required}
    :level #{:level}}
})

(validate! {:machine {:names {:foo 1}}} (combine v2 v1))

; a fail fast version
(validate! {:machine {:names {:foo 1}}} (combine v2 v1) :error ::non-valid-machine)
```

Erros are simple data structures as well:

```clojure
(fact "composition"
  (let [ v1 {:machine {:ip #{:String :required} :names #{:Vector}} :vcenter {:pool #{:String}}} 
         v2 {:machine {:ip #{:String :required} :names #{:required} }}]
    (validate! {:machine {:names {:foo 1} :ip 1}} (combine v2 v1)) => 
         {:machine {:ip '("must be a string"), :names '("must be a vector")}}  ))
```

Validations for sequences are just functions too:

```clojure
; k-v types, every-kv helper is in subs.core
(validation :node* (every-kv {:ip #{:required}}))

; {:nodes '(({:master {:ip ("must be present")}})}
(validate! {:nodes {:master {}}} {:nodes #{:node*}})

; sequence types, every-v helper is in subs.core
(validation :name* (every-v #{:String :required}))

; {:names '(({0 ("must be a string")}))}
(validate! {:names [1 "1"]} {:names #{:name*}})
```
See [docs](http://narkisr.github.io/substantiation/)

# Copyright and license

Copyright [2013] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
