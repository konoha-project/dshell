.SILENT:

INSTALL_PREFIX?="$(HOME)"
BIN_NAME="dshell.jar"
SCRIPT_NAME="dshell"

all: build

build:
	ant

clean:
	ant clean

install:
	echo "install dshell to $(INSTALL_PREFIX)/bin"
	install -d $(INSTALL_PREFIX)/bin
	cp -f $(BIN_NAME) $(INSTALL_PREFIX)/bin/
	install -m 775 $(SCRIPT_NAME) $(INSTALL_PREFIX)/bin/

.PHONY: all build clean install
