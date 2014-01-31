D-Shell
======

## Build Requirement

* libzen (https://github.com/konoha-project/libzen)
* ant
* jdk 1.7

## How To Use

### Build libzen

    $ mkdir ~/working
    $ cd ~/working
    $ git clone git@github.com:konoha-project/libzen.git
    $ cd ./libzen
    $ git checkout -b build 3b2bed6c7cf14ae3cce25a3bc1696abcb2da48e2
    $ ant

### Build D-Shell

    $ cd ~/working
    $ git clone git@github.com:konoha-project/dshell.git
    $ cd ./dshell
    $ mkdir ./ext
    $ cp ~/working/libzen/libzen.jar ./ext
    $ make
    $ make install

### Run D-Shell

    $ dshell
