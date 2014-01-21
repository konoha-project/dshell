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
    $ git checkout -b 73ffcdc02f9a92d4e2a421080252849a2dc04aa4 build
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
