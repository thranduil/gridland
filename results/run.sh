#!/bin/sh

for i in `ls -d */`
do
    cd $i
    echo "Running" $i
    ./run.sh
    wait
    cd ..
done