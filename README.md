# clograms

**Clograms** explore and document any Clojure or ClojureScript project by drawing diagrams.

**Clograms** tries to close the gap between code and diagrams by creating a diagram tool that is aware of your code.

Diagrams about your code can be helpful in lots of situations like reverse engeneering an unknown code base, document your project high level architecture or planning for a redesign.

**Clograms** tries to help with this tasks by combining the visualization capabilities of a diagram application with the navigation/exploration features of IDEs.

## How does it look?

<img src="/docs/screenshot-zoom-out.png?raw=true"/>

<img src="/docs/screenshot-zoom-in.png?raw=true"/>

## Features

- **Scan** and **index** your lein and deps.edn projects
- **Draw** diagrams by using **your project entities** (projects, namespaces, vars, specs, etc ...)
- **Draw shapes**  to document or explain any part of your system
- **Link** your entities to express relations
- Quickly **search** any system entity and add it to your diagram
- **Explore** your system and all its dependencies using the **project browser**
- Easily add more entities using clograms **source code navigation**
- Quickly figure out **multimethod implementations** and **functions specs**
- Any **function x-references** is one click away
- **Re-frame aware**, quickly explore your subscriptions, events, fxs and cofxs
- **Hot code reload**, if you change any clj, cljs or cljc file in the project you are indexing, changes should flow to the UI auto-magically
- **Save** and **share** your diagrams as .edn files

## Can I see a demo?

Screencast coming, stay tuned!

## Give it a try!

**Clograms** is available as a Maven artifact from Clojars.

The latest released version is: [![Clojars Project](https://img.shields.io/clojars/v/clograms.svg)](https://clojars.org/clograms)<br>

### Prerequisites

- Install [clojure cli tool](https://clojure.org/guides/getting_started)

### Index a Clojure project :

```bash
clj -Sdeps '{:deps {clograms {:mvn/version "0.1.10"}}}' -m clograms.server my-clojure-project-folder clj
```

### Index a ClojureScript project :

```bash
clj -Sdeps '{:deps {clograms {:mvn/version "0.1.10"}}}' -m clograms.server my-clojurescript-project-folder cljs
```

When indexing finishes, point your browser to http://localhost:3000 and wait a little bit for it to load, it needs to transfer the entire index to the browser.

You can also run your browser in app mode like `google-chrome --app=http://localhost:3000`.

## Micro Manual

### Search

You can add nodes to your diagram by using the top bar search. If you remember the name of a function, namespace, project, spec or whatever you can type it there.
Once you select a option, the node will be added to the center of the diagram.

### Nodes

There are multiple types of nodes you can use in your diagram. You can drag them from the right side bar.
All nodes support at least the remove context menu option and they can be filtered by using the input at the top of the right side bar.

Multiple nodes can be selected by holding the CTRL key. When multiple nodes are selected, dragging will drag all selected nodes.

Currently supported nodes :

*Shapes* : circle, rectangle, database, group and user
*Source code nodes* : projects, namespaces, vars, specs, re-frame(events, subs, fxs, cofxs)

### Shape Nodes

Shapes also support the `Edit text` context menu which you can use to change the shape label and are also resizable by dragging the bottom right corner.

### Links

Links can be created by dragging from one node port to another node port.

Arrows direction can be changed by using the arrows controls at the top.

Links also support label edition by using the `Edit text` context menu and can be removed like nodes.

### Functions and Vars nodes

Clicking on any function call link will add another function node in front of it.

You can find function call references by using the `Find references` context menu. Double clicking on any funcition reference will automatically add a node
behind it.

### Coloring

You can color nodes by project or namespace they belong to by using the context menu on any source code node. The current selected color will be used.

### Saving your diagram

The current diagrams save functionality is not ideal but you can save and restore diagrams from edn files.

When you click the save button in the ui **Clograms** will create a `diagram.edn` file inside the folder you are running it from, and will read it every time you load the ui.

You can also save your diagram as a pdf file using the browser print functionality. It will hide all the toolbars so you can print only the diagram.

## Developers info

Clograms is based on [clindex](https://github.com/jpmonettas/clindex) a Clojure[Script] source code indexer.

### Build UI

#### Prerequisites

- Install [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation)

```bash
cd clograms

make watch-ui # will start shadow-cljs and watch the ui, open your browser at http://localhost:9500/clograms.html

make watch-css # will watch garden files and build css files
```

### Run it

You can index a project by runnning :

```bash
clj -m clograms.server some-project-folder clj # you can also use cljs to index clojurescript projects
```

### Docs

You can find a diagram.edn file in this repo that documents some aspects of Clograms. It is also [exported as pdf](/docs/clograms.pdf)

## Related work

- [The FC4 Framework](https://github.com/FundingCircle/fc4-framework) A framework for authoring, publishing, and maintaining C4 software architecture diagrams.

## Roadmap

- More shapes, arrows and diagrams tools in general
- Add support for deps tree diagrams like https://github.com/clojure/tools.deps.graph
- Multiscale diargams
