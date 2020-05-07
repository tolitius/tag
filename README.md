## tag

tagging apps with git and other intel

[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Ftolitius%2Ftag%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/tag/releases)
[![<! clojars](https://img.shields.io/clojars/v/tolitius/tag.svg)](https://clojars.org/tolitius/tag)

## why

once the app is built it is an immutable artifact. this artifact usually has quite a limitted way to describe itself:

> `name-version.jar` is not exactly very telling.

`tag` is run right _before_ building an artifact to include additional useful intel about it.

by the time artifact is built it would have an `about.edn` file that can be looked at by the app at runtime.

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

once the jar is built the `edn` above will (by default) live _inside_ the jar in `./about.edn`.

so for example if your, say http, app has an `/about` endpoint, it could read this file on start or at runtime and display the immutable intel above.

## show me

`tag` _uses itself_ to include the intel in itself right before it is built. so let's look at what it does.

it has [an alias](https://github.com/tolitius/tag/blob/7384178e9b6228c653a7f4cf014962f533752829/deps.edn#L4) in deps.edn:

```clojure
:tag {:main-opts ["-m" "tag.core" "tag" "I tag apps with immutable intel"]}
```

which calls out to `tag` and exports the intel.

it also has a `:jar` alias with [an addition](https://github.com/tolitius/tag/blob/7384178e9b6228c653a7f4cf014962f533752829/deps.edn#L6) of an `:extra-paths`:

```clojure
:extra-paths ["target/META-INF"]
```

`tag` will place "`about.edn`" to `target/META-INF` in order for a `jar` task to pick it up during the build.

hence in order to tag, build and deploy we can do:

```bash
$ clojure -A:tag -A:jar -A:deploy
```

### look inside

let's look at what inside this newly built `tag.jar`:

```bash
$ jar -tvf tag.jar
   322 Wed May 06 21:38:22 EDT 2020 about.edn            ## <<< oh.. look who is here
     0 Wed May 06 21:47:32 EDT 2020 tag/
  1346 Wed May 06 21:35:25 EDT 2020 tag/core.clj
     0 Wed May 06 21:47:33 EDT 2020 META-INF/
    58 Wed May 06 21:47:32 EDT 2020 META-INF/MANIFEST.MF
     0 Wed May 06 21:47:33 EDT 2020 META-INF/maven/
     0 Wed May 06 21:47:33 EDT 2020 META-INF/maven/tolitius/
     0 Wed May 06 21:47:33 EDT 2020 META-INF/maven/tolitius/tag/
   110 Wed May 06 21:47:32 EDT 2020 META-INF/maven/tolitius/tag/pom.properties
  1711 Wed May 06 21:47:32 EDT 2020 META-INF/maven/tolitius/tag/pom.xml
```

```bash
$ jar -xvf tag.jar about.edn
 inflated: about.edn
```

```bash
$ cat about.edn

{:about {:app-name "tag", "what do I do?" "I tag apps with immutable intel"}, :git {:commit-id "45c33d3", :version/tag "v0.1.0", :commit-time "Wed May 6 21:35:25 2020 -0400", "commit human (or not)" "'anatoly'", :commit-message "support apps with no tags/other parts"}, :described-at #inst "2020-05-07T01:38:22.880-00:00"}
```

great success.

this way `tag` can also be used with lein:

```clojure
:prep-tasks [["run" "-m" "tag.core/-main"
                         "hubble" "I explore new worlds"]]

```

boot, make, and other build tools.

## License

Copyright © 2020 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
