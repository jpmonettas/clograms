# clograms

Explore clojure projects by building diagrams

WIP

<img src="/docs/screenshot.png?raw=true"/>

## How to build and run

### Prerequisites

- Install [clojure cli tool](https://clojure.org/guides/getting_started)
- Install [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation)

### Build UI

```bash
cd clograms
npx shadow-cljs release app
```

### Run it

You can index a project by runnning :

```bash
clj -m clograms.server some-project-folder clj # you can also use cljs to index clojurescript projects
```
