#!/bin/bash

source ./script.inc
source ./config.inc

cd RTruffleSOM

get_pypy

INFO Build RTruffleSOM
make clean
make
OK RTruffleSOM Build Completed.
