.SILENT:

INSTALL_PREFIX?="$(HOME)"
JAR_NAME="dshell.jar"
BIN_NAME="dshell"
TOOLS_DIR="./tools"

all: build

build: preprocess
	ant

preprocess:
	python ./tools/gen-array.py ./src/dshell/lang/GenericArray.java
	java -jar ./lib/antlr-4.2.2-complete.jar ./dshell.g4 -o ./gensrc/dshell/internal/parser -no-listener -no-visitor -encoding UTF-8

clean: clean-launcher
	rm -rf ./gensrc
	rm -rf ./generated-array
	ant clean

clean-launcher:
	make -C $(TOOLS_DIR)/launcher clean

install:
	echo "install dshell to $(INSTALL_PREFIX)/bin"
	install -d $(INSTALL_PREFIX)/bin
	make -C $(TOOLS_DIR)/launcher JAR_PREFIX=$(INSTALL_PREFIX)/bin
	cp -f $(JAR_NAME) $(INSTALL_PREFIX)/bin/
	install -m 775 $(TOOLS_DIR)/launcher/$(BIN_NAME) $(INSTALL_PREFIX)/bin/

test:
	TEST_DIR=./test ./test/test_all.sh

self-test:
	cd ./tools/test-dshell/; ant clean; ant; cp ./test-dshell.jar ../../
	TEST_DIR=./test dshell ./test/run_test.ds

test-rec:
	#USAGE: make test-rec URL=http://www.ubicg.ynu.ac.jp/Rec/api/3.0
	TEST_DIR=./test ./test/test_all.sh $(URL)
.PHONY: all build clean install test
