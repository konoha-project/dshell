.SILENT:

INSTALL_PREFIX?="$(HOME)"
JAR_NAME="dshell.jar"
BIN_NAME="dshell"
TOOLS_DIR="./tools"

all: build

build:
	cd ./ext/libbun && git checkout master && git checkout 7ff0d237473a0354e807035bcaa379c602cfcec9
	cd ../../
	ant
	cd ./ext/libbun && git checkout master
	cd ../../

clean: clean-launcher
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
	cd ./tools/test-dshell/ && ant clean && ant && cp ./test-dshell.jar ../../
	cd ../../
	TEST_DIR=./test dshell ./test/run_test.ds

test-rec:
	#USAGE: make test-rec URL=http://www.ubicg.ynu.ac.jp/Rec/api/3.0
	TEST_DIR=./test ./test/test_all.sh $(URL)
.PHONY: all build clean install test
