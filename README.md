## tag

tagging apps with git and other intel

[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Ftolitius%2Ftag%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/tag/releases)
[![<! clojars](https://img.shields.io/clojars/v/tolitius/tag.svg)](https://clojars.org/tolitius/tag)

## why

once the app is built it is an immutable artifact. this artifact usually has quite a limitted way to describe itself:

> `name-version.jar` is not exactly very telling.

`tag` could be run right _before_ building an artifact to include additional useful intel about it.

by the time artifact is built it would have an `about.edn` file, usually somewhere at "`META-INF/`", that can be looked at by the app at runtime.

## how

```clojure
=> (require '[tag.core :as t])
```

`tag` knows how to describe the app:

```clojure
=> (t/describe "hubble" {:about "I explore new worlds"})

{:about {:app-name "hubble",
         "what do I do?" "I explore new worlds"},
 :git
 {:commit-id "8b15218",
  :version/tag "v0.1.42",
  :commit-time "Wed May 6 20:26:03 2020 -0400",
  "commit human (or not)" "'anatoly'",
  :commit-message "readjust a low gain antena for the cat's eye nebula"},
 :described-at #inst "2020-05-07T00:55:03.260-00:00"}
```

and how to export it:

```clojure
=> (-> (t/describe "hubble")
       t/export-intel)
       
:intel-exported
```

once the jar is built the `edn` above will (by default) live under `META-INF/about.edn`.

so for example if your, say http, app has an `/about` endpoint, it could display the immutable intel above.

## License

Copyright © 2020 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
