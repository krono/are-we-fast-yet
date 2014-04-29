#!/bin/bash

source script.inc

check_for_tools git ant tar make javac mv unzip uname

./build-graal.sh
./build-classic-benchmarks.sh
./build-trufflesom.sh
./build-rpysom.sh
./build-sompp.sh

OK done.
exit 0;