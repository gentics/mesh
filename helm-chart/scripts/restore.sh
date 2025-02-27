#!/bin/bash

set -o nounset
set -o errexit

NAME=${1:-current}
BASE=/snapshots/$NAME
KEYSTORE=/keystore
UPLOADS=/uploads

function prepare {
  if [ ! -e $BASE ] ; then
    echo "[FAIL] - Did not find selected snapshot \"$NAME\" at $BASE"
    exit 1
  else
    echo "[OK] - Found snapshot directory for \"$NAME\""
  fi

  # Pre-Checks
  {{- if .Values.backup.allowedParts.keystore }}
  if [ -e $KEYSTORE/keystore.jceks ] ; then
    echo "[FAIL] Found existing keystore in $KEYSTORE. Aborting restore"
    ls -la $KEYSTORE
    exit 2
  else
    echo "[OK] - Target does not contain an existing keystore"
  fi 
  {{- end }}

  {{- if .Values.backup.allowedParts.uploads }}
  # Check if the backup contains uploads
  if [ ! -z "$(ls -A $UPLOADS)" ] ; then
    echo "[FAIL] - Found files within the upload folder $UPLOADS. Restore is only allowed in empty setups"
    ls -la $UPLOADS
    exit 4
  else
    echo "[OK] - No existing uploads in $UPLOADS found."
  fi
  {{- end }}

  echo
  echo "Invoking restore of snapshot \"$NAME\""
  echo "Size: $(du -h -c $BASE | grep total)"
  echo
}

function restore {
  {{- if .Values.backup.allowedParts.keystore }}
  # Restore Keystore file
  echo
  echo "Restoring keystore into $KEYSTORE"

  {{- if .Values.backup.useTar }}
    time tar xfv $BASE/keystore.tar -C $KEYSTORE
  {{- else }}
    time cp -ra $BASE/keystore/keystore/keystore.jceks $KEYSTORE/keystore.jceks
  {{- end }}
  {{- else }}
  echo "No keystore restoring requested"
  {{- end }}

  {{- if .Values.backup.allowedParts.uploads }}
  # Restore Uploads
  if [ -z "$(ls -A $BASE/uploads/uploads)" ] && [ -z "$(ls -A $BASE/uploads.tar)" ] ; then
    echo "[WARN]: Skipping restore of uploads. No uploads were found in the snapshot"
  else
    echo "Restoring uploads into $UPLOADS"

    {{- if .Values.backup.useTar }}
      time tar xfv $BASE/uploads.tar -C $UPLOADS
    {{- else }}
      time cp -ra $BASE/uploads/uploads/* $UPLOADS
    {{- end }}

  fi
  {{- else }}
  echo "No uploads restoring requested"
  {{- end }}
}

function finalize {
  echo
  echo "Restore complete"
}

prepare
restore
finalize
