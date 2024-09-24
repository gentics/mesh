#!/bin/bash
# Name: Gentics Mesh API docs Upload Script
# Description: This script will upload the apidocs to the website

function print_usage() {
  echo "Usage: $0"
  echo " Example: $0"
  echo " This will upload the previous build apidocs"
}

# Generic error check and abort method
function handleError() {
  if [ "$1" != "0" ]; then
     echo -e "\n\nERROR: $2"
     echo -e "Aborting with errorcode $1 \n\n"
     exit $1
  fi
}

SCRIPT="`readlink -f $0`"
BASEDIR="`dirname "$SCRIPT"`"

REMOTE_HOST=gentics@www.gentics.com
REMOTE_DEST=/var/www/vhosts/www.gentics.com/gcn
TMPDIR=/tmp

version=SNAPSHOT

function uploadGuides() {
  echo -e "\n * API docs"
    echo -e "\n * Cleanup"
      echo "Deleting: $REMOTE_DEST/apidocs-history/$version/"
      echo "Press enter to continue"
      read 
      ssh $REMOTE_HOST "rm -rf $REMOTE_DEST/apidocs-history/$version/"
    echo "Done."

    echo -e "\n * Transfering and extracting apidocs"
      ssh $REMOTE_HOST mkdir -p $REMOTE_DEST/apidocs-history/$version
      scp target/mesh-doc*.zip  $REMOTE_HOST:$REMOTE_DEST/apidocs-history/$version
      handleError $? "Could not transfer apidocs $version"
      ssh $REMOTE_HOST "cd $REMOTE_DEST/apidocs-history/$version ; unzip -o *.zip"
      echo "Creating symlink from latest to $version"
      ssh $REMOTE_HOST "cd $REMOTE_DEST/apidocs-history ; unlink latest ; ln -s $version latest"
      handleError $? "Could not extract apidocs"
    echo "Done."

  echo "Done."
}

uploadGuides
