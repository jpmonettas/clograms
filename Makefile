.PHONY: clean help

watch-ui:
	npx shadow-cljs watch app

watch-css:
	clj -e "(require '[garden-watcher.core :as gw]) (require '[com.stuartsierra.component :as component]) (component/start (gw/new-garden-watcher '[clograms.styles.main]))"

release-ui: clean
	npx shadow-cljs release app

clean:
	clj -T:build clean
	-rm -rf resources/public/js/*

clograms.jar:
	clj -T:build jar

install: clograms.jar
	mvn install:install-file -Dfile=target/clograms.jar -DpomFile=target/classes/META-INF/maven/com.github.jpmonettas/clograms/pom.xml

deploy:
	mvn deploy:deploy-file -Dfile=target/clograms.jar -DrepositoryId=clojars -DpomFile=target/classes/META-INF/maven/com.github.jpmonettas/clograms/pom.xml -Durl=https://clojars.org/repo

tag-release:
	git add CHANGELOG.md && \
	git commit -m "Updating CHANGELOG after $(version) release" && \
	git tag "v$(version)" && \
	git push origin master

help:
	@echo "For releasing to clojars run"
	@echo "make version=x.y.z release"
