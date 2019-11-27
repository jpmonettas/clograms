.PHONY: ui release clean help

clean:
	-rm clograms.jar
	-rm pom.xml
	-rm -rf resources/public/js/*

clograms.jar:
	clj -A:jar clograms.jar

pom.xml:
	clj -Spom
	mvn versions:set -DnewVersion=$(version)

release-ui: clean
	npx shadow-cljs release app

release: release-ui clograms.jar pom.xml
	mvn deploy:deploy-file -Dfile=clograms.jar -DrepositoryId=clojars -DpomFile=pom.xml -Durl=https://clojars.org/repo

tag-release:
	git add CHANGELOG.md && \
	git commit -m "Updating CHANGELOG after $(version) release" && \
	git tag "v$(version)" && \
	git push origin master

help:
	@echo "For releasing to clojars run"
	@echo "make version=x.y.z release"
