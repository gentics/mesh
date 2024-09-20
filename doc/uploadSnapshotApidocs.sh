#!/bin/bash
# Name: Gentics CMS Guide Upload Script
# Description: This script will upload the guides to the website

function print_usage() {
  echo "Usage: $0"
  echo " Example: $0"
  echo " This will upload the previous build guides"
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
  echo -e "\n * Guides"
    echo -e "\n * Cleanup"
      echo "Deleting: $REMOTE_DEST/guides-history/$version/"
      echo "Press enter to continue"
      read 
      ssh $REMOTE_HOST "rm -rf $REMOTE_DEST/guides-history/$version/"
    echo "Done."

    echo -e "\n * Transfering and extracting guides"
      ssh $REMOTE_HOST mkdir -p $REMOTE_DEST/guides-history/$version
      scp target/contentnode-doc*.zip  $REMOTE_HOST:$REMOTE_DEST/guides-history/$version
      handleError $? "Could not transfer guides $version"
      ssh $REMOTE_HOST "cd $REMOTE_DEST/guides-history/$version ; unzip -o *.zip"
      echo "Creating symlink from latest to $version"
      ssh $REMOTE_HOST "cd $REMOTE_DEST/guides-history ; unlink latest ; ln -s $version latest"
      handleError $? "Could not extract guides"
    echo "Done."

  echo "Done."
}

uploadGuides
