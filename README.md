## tag

tagging apps with git and other intel

[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Ftolitius%2Ftag%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/tag/releases)
[![<! clojars](https://img.shields.io/clojars/v/tolitius/tag.svg)](https://clojars.org/tolitius/tag)

- [why](#why)
- [how](#how)
- [show me](#show-me)
  - [look inside](#look-inside)
- [composing tags](#composing-tags)
- [yes, but why not in config?](#yes-but-why-not-in-config)
- [license](#license)

## why

once the app is built it is an immutable artifact. this artifact usually has quite a limitted way to describe itself:

> `name-version.jar` is not exactly very telling.

`tag` is run right _before_ building an artifact to include additional useful intel about it.

by the time artifact is built it would have an `about.edn` file that can be looked at by the app at runtime:

<img src="doc/img/hubble-about.png" width="600px">

`described-at` in this case would be a build time.

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
  :repo-url "git@github.com:tolitius/hubble.git",
  :commit-time "Wed May 6 20:26:03 2020 -0400",
  "commit human (or not)" "'anatoly'",
  :commit-message "readjust a low gain antena for the cat's eye nebula"},
 :described-at #inst "2020-05-07T00:55:03.260-00:00"}
```

and how to export it:

```clojure
=> (-> (t/describe "hubble")
       (t/export-intel {:app-name "tolitius/hubble"}))

{:intel-exported-to "target/about/META-INF/tolitius/hubble/about.edn"}
```

once the jar is built the `edn` above will (by default) live _inside_ the jar in `./META-INF/[app-name]/about.edn`.

so for example if your, say http, app has an `/about` endpoint, it could read this file on start or at runtime and display the immutable intel above.

## show me

`tag` _uses itself_ to include the intel in itself right before it is built. so let's look at what it does.

it has [an alias](https://github.com/tolitius/tag/blob/master/deps.edn#L4) in deps.edn:

```clojure
:tag {:main-opts ["-m" "tag.core" "tolitius/tag" "I tag apps with immutable intel"]}
```

which calls out to `tag` and exports the intel.

it also has a `:jar` alias with [an addition](https://github.com/tolitius/tag/blob/2bf572c5cb3fa95d1868ea2e0b2814670e21a648/deps.edn#L6) of an `:extra-paths`:

```clojure
:extra-paths ["target/about"]
```

`tag` will place "`about.edn`" to `target/about/META-INF` in order for a `jar` task to pick it up during the build.

hence in order to tag and build we can do:

```bash
$ clj -A:tag
$ clj -A:jar
```

### look inside

let's look at what inside this newly built `tag.jar`:

```bash
$ jar -tvf tag.jar
     0 Thu May 07 00:17:10 EDT 2020 META-INF/
     0 Thu May 07 00:17:10 EDT 2020 META-INF/tolitius/tag/
   321 Thu May 07 00:17:06 EDT 2020 META-INF/tolitius/tag/about.edn     ## <<< oh.. look who is here
     0 Thu May 07 00:17:10 EDT 2020 tag/
  1769 Thu May 07 00:16:58 EDT 2020 tag/core.clj
    58 Thu May 07 00:17:10 EDT 2020 META-INF/MANIFEST.MF
     0 Thu May 07 00:17:10 EDT 2020 META-INF/maven/
     0 Thu May 07 00:17:10 EDT 2020 META-INF/maven/tolitius/
     0 Thu May 07 00:17:10 EDT 2020 META-INF/maven/tolitius/tag/
   110 Thu May 07 00:17:10 EDT 2020 META-INF/maven/tolitius/tag/pom.properties
  1711 Thu May 07 00:17:10 EDT 2020 META-INF/maven/tolitius/tag/pom.xml
```

```bash
$ jar -xvf tag.jar META-INF/tolitius/tag/about.edn
 inflated: META-INF/tolitius/tag/about.edn
```

```bash
$ cat META-INF/tolitius/tag/about.edn

{:about {:app-name "tolitius/tag", "what do I do?" "I tag apps with immutable intel"}, :git {:commit-id "58df09d", :version/tag "v0.1.0", :repo-url "git@github.com:tolitius/tag.git", :commit-time "Wed May 6 23:41:33 2020 -0400", "commit human (or not)" "'Anatoly'", :commit-message "[docs]: add lein :prep-tasks example"}, :described-at #inst "2020-05-07T04:17:06.081-00:00"}
```

great success.

this way `tag` can also be used with lein:

```clojure
:prep-tasks [["run" "-m" "tag.core/-main"
                         "hubble" "I explore new worlds"]
             ["compile"]]


:jar {:resource-paths ["target/about"] ;; ... }
;; or and uberjar:
:uberjar {:resource-paths ["target/about"] ;; ... }
```

boot, make, and other build tools.

## composing tags

one intersing side effect of tagging libs or dependencies with `tag` is that the final app jar / uberjar has them all:

```bash
$ jar -tvf target/hubble-standalone.jar | grep about
   369 Thu May 07 01:18:32 EDT 2020 META-INF/tolitius/hubble/about.edn
   331 Thu May 07 01:06:21 EDT 2020 META-INF/tolitius/tag/about.edn
   301 Thu May 07 01:02:25 EDT 2020 META-INF/tolitius/mount/about.edn
   395 Thu May 07 01:01:42 EDT 2020 META-INF/tolitius/cprop/about.edn
   384 Thu May 07 01:01:57 EDT 2020 META-INF/tolitius/lasync/about.edn
   ...
```

hence building something like `/about/[dependency-name]` app endponts is straightforward and allows to have reliable intel about not just the app itself but its dependencies as well.

## access intel

a likely http route (in this case [reitit](https://github.com/metosin/reitit)) would look like this:

```clojure
["/about"
 {:get (constantly
         {:status 200
          :body   (tools/edn-resource "META-INF/hubble/about.edn")})}]]
```

`tools/edn-resource` might be something like this:

```clojure
(ns app.tools
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn slurp-resource [path]
  (-> path
      io/resource
      slurp))

(defn edn-resource [path]
  (-> (slurp-resource path)
      edn/read-string))
```

## yes, but why not in config?

configuration is usually overriden by system props, ENV vars, consul, etcd, etc.

the idea behind `tag` is to make sure the built artifact "describes itself consistently", or [constantly](https://clojuredocs.org/clojure.core/constantly) as we say in Clojure circles.

## License

Copyright © 2020 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
