#! /bin/bash

NCURSES="5.9"

wget http://ftp.gnu.org/pub/gnu/ncurses/ncurses-${NCURSES}.tar.gz
tar xzf ncurses-${NCURSES}.tar.gz
 
hg clone -r520 http://www.neuron.yale.edu/hg/neuron/nrn nrn-hg
cp nrn-android.patch nrn-hg
cd nrn-hg
patch -p1 < nrn-android.patch
./build.sh
cd ..
