.SILENT:

JAR_PREFIX?="$(HOME)"
BIN_NAME="dshell"

build-launcher:
	gcc -DJAR_PATH="$(JAR_PREFIX)" -o ${BIN_NAME} ${BIN_NAME}.c

clean:
	rm -f ${BIN_NAME}


.PHONY: build-launcher clean
