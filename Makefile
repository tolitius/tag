.PHONY: clean aot jar outdated install deploy tree test repl

clean:
	rm -rf target
	rm -rf classes
	rm -rf tag.jar

aot:
	mkdir classes

jar: clean aot
	clojure -A:jar

outdated:
	clojure -M:outdated

install: jar
	clojure -A:install

deploy: jar
	clojure -A:deploy

tree:
	mvn dependency:tree

test:
	clojure -X:test :patterns '[".*test"]'

repl:
	clojure -A:dev -A:repl
