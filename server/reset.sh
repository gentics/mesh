#!/bin/bash

rm -rf data
rm -f mesh.lock
cp -ra data-$1 data

echo "Reset Done"
