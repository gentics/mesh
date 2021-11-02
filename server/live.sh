#!/bin/sh

file=mesh.live
maxage=60

if [ ! -r $file ]
    then
        echo "file $file does not exist"
        exit 1
fi

now=`date +%s`
touched=`stat -c %Y $file`
age=`expr $now - $touched`

if [ "$age" -gt "$maxage" ]
    then
        echo "$file was last modified $age s ago"
        exit 2
fi
