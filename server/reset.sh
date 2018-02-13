#!/bin/bash

rm -rf data
rm -f mesh.lock
cp -ra $1 data

echo "Reset Done"
