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
    $ git checkout -b build ae78ff60e6717c283a31d9592335be56126058c6
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
