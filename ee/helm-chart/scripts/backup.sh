#!/bin/bash

set -o nounset
set -o errexit

# Target directory for volume for backup data
DEST=/target

# Sources
UPLOADS=/uploads
KEYSTORE=/keystore

# Name of the final backup folder
NAME=current

# Name of the backup staging folder
STAGE_NAME=current_tmp

function backup {
  {{- if .Values.backup.allowedParts.uploads }}
  echo "Syncing Gentics Mesh uploads"
  mkdir -p $DEST/$STAGE_NAME/uploads

  {{- if .Values.backup.useTar }}
    cd $UPLOADS && tar cfv $DEST/$STAGE_NAME/uploads.tar ./
  {{- else }}
    rsync -rav $UPLOADS $DEST/$STAGE_NAME/uploads
  {{- end }}
  {{- else }}
  echo "No uploads backup requested"
  {{- end }}

  {{- if .Values.backup.allowedParts.keystore }}
  echo "Syncing Gentics Mesh keystore"
  mkdir -p $DEST/$STAGE_NAME/keystore
  {{- if .Values.backup.useTar }}
    cd $KEYSTORE && tar cfv $DEST/$STAGE_NAME/keystore.tar ./
  {{- else }}
    rsync -rav $KEYSTORE $DEST/$STAGE_NAME/keystore
  {{- end }}
  {{- else }}
  echo "No keystore backup requested"
  {{- end }}
}

function prepare {
  echo "Running backup with user $(id)"
  if [ -e $DEST/${NAME}_old ] ; then
    echo "Removing old staging folder"
    rm -rf $DEST/${NAME}_old
  fi
  mkdir -p $DEST/$STAGE_NAME
  mkdir -p $DEST/$NAME
}

function finalize {
  echo "New Backup size: $(du -hs $DEST/$STAGE_NAME)"
  echo "Old Backup size: $(du -hs $DEST/$NAME)"

  echo "Moving current backup away"
  mv $DEST/$NAME/ $DEST/${NAME}_old

  echo "Moving new backup in-place"
  mv $DEST/$STAGE_NAME $DEST/$NAME

  echo "Removing old backup"
  rm -rf $DEST/${NAME}_old

  echo "Backup process completed."
}

prepare
backup
finalize
