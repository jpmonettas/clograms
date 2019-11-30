.PHONY: install deploy ui release clean help

clean:
	-rm clograms.jar
	-rm pom.xml
	-rm -rf resources/public/js/*
	-rm target -rf

clograms.jar:
	clj -A:jar clograms.jar

pom.xml:
	clj -Spom
	mvn versions:set -DnewVersion=$(version)

watch-ui:
	npx shadow-cljs watch app

watch-css:
	clj -e "(require '[garden-watcher.core :as gw]) (require '[com.stuartsierra.component :as component]) (component/start (gw/new-garden-watcher '[clograms.styles.main]))"

release-ui: clean
	npx shadow-cljs release app

release: release-ui clograms.jar pom.xml

install: clograms.jar pom.xml
	mvn install:install-file -Dfile=clograms.jar -DpomFile=pom.xml

deploy:
	mvn deploy:deploy-file -Dfile=clograms.jar -DrepositoryId=clojars -DpomFile=pom.xml -Durl=https://clojars.org/repo

tag-release:
	git add CHANGELOG.md && \
	git commit -m "Updating CHANGELOG after $(version) release" && \
	git tag "v$(version)" && \
	git push origin master

help:
	@echo "For releasing to clojars run"
	@echo "make version=x.y.z release"
