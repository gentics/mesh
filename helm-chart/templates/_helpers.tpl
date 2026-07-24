{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "mesh.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "mesh.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "mesh.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "mesh.labels" -}}
helm.sh/chart: {{ include "mesh.chart" . }}
{{ include "mesh.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "mesh.selectorLabels" -}}
app.kubernetes.io/name: {{ include "mesh.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "mesh.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (include "mesh.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/* Build Environment Variables with Overrides */}}
{{- define "mesh.envVars" }}
{{- $root := . -}}
{{- $extraEnv := .Values.extraEnv -}}
{{- $builtInEnv := (tpl ($.Files.Get "config/env.yaml") $root) | fromYaml }}
{{- range $builtInEnv.builtInEnvVariables -}}
{{- if eq "false" (include "hasEnvOverride" (list $extraEnv .name)) -}}
{{- range $k, $v := . -}}
{{ if eq $k "name" -}}
{{ print "- " $k ": " ($v | toYaml) }}
{{ else -}}
{{ print "  " $k ": " ($v | toJson) }}
{{ end -}}
{{- end -}}
{{- end -}}
{{- end -}}
## Extra Environment Variables
{{ toYaml $extraEnv }}
{{- end }}

{{/* Find a key/value pair in a list */}}
{{- define "hasKeyWithValue" -}}
  {{- $returnVal := false -}}
  {{- $list := index . 0 -}}
  {{- $key := index . 1 -}}
  {{- $value := index . 2 -}}
  {{/* Loop through the list */}}
  {{- range $list -}}
    {{/* Loop through the list items */}}
    {{- range $i, $val := . -}}
      {{/* Check if key and value matches */}}
      {{- if and (eq $i $key) (eq $val $value) -}}
        {{- $returnVal = true -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{/* Return the result */}}
{{- $returnVal -}}
{{- end -}}

{{/* Check if key name exists in the extraEnv list */}}
{{- define "hasEnvOverride" -}}
{{- $extraEnv := index . 0 -}}
{{- $inputVal := index . 1 -}}
{{- include "hasKeyWithValue" (list $extraEnv "name" $inputVal) -}}
{{- end -}}

{{/*
Database host for Mesh.
Uses explicit database.host if set; falls back to the bundled MariaDB service name
when global.mariadb.enabled is true; otherwise raises an error.
*/}}
{{- define "mesh.databaseHost" -}}
{{- include "gentics-common.databaseHost" (dict "root" . "db" .Values.database) -}}
{{- end -}}

{{/*
Database port for Mesh.
Uses explicit database.port if set; falls back to 3306 when global.mariadb.enabled.
*/}}
{{- define "mesh.databasePort" -}}
{{- include "gentics-common.databaseValue" (dict "root" . "db" .Values.database "key" "port" "default" 3306 "requiredMsg" "database.port is required when global.mariadb.enabled is false") -}}
{{- end -}}

{{/*
Database name for Mesh.
Uses explicit database.name if set; falls back to "mesh" when global.mariadb.enabled.
*/}}
{{- define "mesh.databaseName" -}}
{{- include "gentics-common.databaseValue" (dict "root" . "db" .Values.database "key" "name" "default" "mesh" "requiredMsg" "database.name is required when global.mariadb.enabled is false") -}}
{{- end -}}

{{/*
Database username for Mesh.
Uses explicit database.username if set; falls back to "mesh" when global.mariadb.enabled.
*/}}
{{- define "mesh.databaseUsername" -}}
{{- include "gentics-common.databaseValue" (dict "root" . "db" .Values.database "key" "username" "default" "mesh" "requiredMsg" "database.username is required when global.mariadb.enabled is false") -}}
{{- end -}}

{{/*
Secret name and key for the Mesh database password.
Priority: explicit database.passwordSecret → umbrella MariaDB secret → sub-chart local secret.
Returns a full secretKeyRef block.
*/}}
{{- define "mesh.databasePasswordRef" -}}
{{- include "gentics-common.databasePasswordRef" (dict "root" . "db" .Values.database "component" "mesh" "localSecretName" (printf "%s-database" (include "mesh.fullname" .)) "localSecretKey" "password") -}}
{{- end -}}

{{/*
Name of the secret containing the Mesh license key.
Priority: local license.secret (explicit override) → global license secret (umbrella chart)
         → mesh.fullname-secrets (sub-chart's own secret, when license.key is set locally).
Note: Mesh mounts the license from the shared PVC, not directly from this secret.
      This helper is provided for consistency and future use.
*/}}
{{- define "mesh.licenseSecretName" -}}
{{- include "gentics-common.licenseSecretName" (dict "root" . "localSecret" .Values.license.secret "fallbackName" (printf "%s-secrets" (include "mesh.fullname" .))) -}}
{{- end -}}

{{/*
Image pull secrets for the main Mesh image.
Local imagePullSecrets overrides global; global is the default. Both use the array format.
*/}}
{{- define "mesh.imagePullSecrets" -}}
{{- include "gentics-common.imagePullSecretsList" (dict "root" . "localList" .Values.imagePullSecrets) -}}
{{- end }}

{{/*
Fully qualified Mesh image (registry/repository:tag, global.imageRegistry aware).
Falls back to the chart appVersion when image.tag is unset.
*/}}
{{- define "mesh.image" -}}
{{- include "gentics-common.image" (dict "root" . "image" .Values.image "tag" (default .Chart.AppVersion .Values.image.tag)) -}}
{{- end -}}

{{/*
Fully qualified Mesh tools image (registry/repository:tag, global.imageRegistry aware).
*/}}
{{- define "mesh.toolsImage" -}}
{{- include "gentics-common.image" (dict "root" . "image" .Values.tools.image) -}}
{{- end -}}

{{/*
Secret key ref for the Mesh initial admin password.
Priority: existingSecret → chart-managed secret (explicit or auto-generated).
*/}}
{{- define "mesh.initialAdminPasswordRef" -}}
{{- if .Values.initialAdminPassword.existingSecret -}}
secretKeyRef:
  name: {{ .Values.initialAdminPassword.existingSecret }}
  key: {{ .Values.initialAdminPassword.existingSecretKey | default "password" }}
{{- else -}}
secretKeyRef:
  name: {{ include "mesh.fullname" . }}-admin
  key: password
{{- end -}}
{{- end -}}

{{/*
Image pull secrets for the Mesh tools image.
Local tools.image.pullSecret overrides global; global is the default.
Local accepts a plain secret name string; global uses the array format.
*/}}
{{- define "mesh.toolsImagePullSecrets" -}}
{{- include "gentics-common.imagePullSecrets" (dict "root" . "localName" .Values.tools.image.pullSecret) -}}
{{- end }}


{{/* ── MCP-Server helpers ─────────────────────────────────────────── */}}

{{/* Full name for MCP server resources: <release>-mcp-server */}}
{{- define "mesh.mcpServer.fullname" -}}
{{- printf "%s-mcp-server" (include "mesh.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels for MCP-server resources */}}
{{- define "mesh.mcpServer.labels" -}}
helm.sh/chart: {{ include "mesh.chart" . }}
{{ include "mesh.mcpServer.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: mcp-server
app.kubernetes.io/part-of: {{ include "mesh.name" . }}
{{- end -}}

{{- define "mesh.mcpServer.selectorLabels" -}}
app.kubernetes.io/name: {{ include "mesh.name" . }}-mcp-server
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Fully qualified MCP-server image (registry/repository:tag, global.imageRegistry aware).
Tag falls back to Chart.AppVersion (the MCP server is versioned with mesh-enterprise).
*/}}
{{- define "mesh.mcpServer.image" -}}
{{- include "gentics-common.image" (dict "root" . "image" .Values.mcpServer.image "tag" (default .Chart.AppVersion .Values.mcpServer.image.tag)) -}}
{{- end -}}

{{/*
Image pull secrets for the MCP-server image.
Local mcpServer.image.pullSecret (string) overrides global.imagePullSecrets.
*/}}
{{- define "mesh.mcpServer.imagePullSecrets" -}}
{{- include "gentics-common.imagePullSecrets" (dict "root" . "localName" .Values.mcpServer.image.pullSecret) -}}
{{- end -}}

{{/* Bearer-token secret name: existingSecret or chart-generated */}}
{{- define "mesh.mcpServer.authSecretName" -}}
{{- if .Values.mcpServer.transport.auth.existingSecret -}}
{{- .Values.mcpServer.transport.auth.existingSecret -}}
{{- else -}}
{{- printf "%s-auth" (include "mesh.mcpServer.fullname" .) -}}
{{- end -}}
{{- end -}}

{{/* Mesh-credentials secret name for the MCP-server */}}
{{- define "mesh.mcpServer.meshSecretName" -}}
{{- if .Values.mcpServer.mesh.existingSecret -}}
{{- .Values.mcpServer.mesh.existingSecret -}}
{{- else -}}
{{- printf "%s-mesh" (include "mesh.mcpServer.fullname" .) -}}
{{- end -}}
{{- end -}}

{{/*
Secret key ref for the Mesh password used by the MCP server.
Priority: mcpServer.mesh.existingSecret (key mcpServer.mesh.existingSecretKey,
default "MESH_PASSWORD") -> explicit mcpServer.mesh.password (chart-managed secret)
-> the release's Mesh admin secret (initialAdminPassword logic, user "admin").
*/}}
{{- define "mesh.mcpServer.meshPasswordRef" -}}
{{- if .Values.mcpServer.mesh.existingSecret -}}
secretKeyRef:
  name: {{ .Values.mcpServer.mesh.existingSecret }}
  key: {{ .Values.mcpServer.mesh.existingSecretKey | default "MESH_PASSWORD" }}
{{- else if .Values.mcpServer.mesh.password -}}
secretKeyRef:
  name: {{ printf "%s-mesh" (include "mesh.mcpServer.fullname" .) }}
  key: MESH_PASSWORD
{{- else -}}
{{- include "mesh.initialAdminPasswordRef" . -}}
{{- end -}}
{{- end -}}

{{/*
Default mesh URL: derived from the Mesh service of this release.
Operators can override via mcpServer.mesh.baseUrl.
*/}}
{{- define "mesh.mcpServer.meshBaseUrl" -}}
{{- if .Values.mcpServer.mesh.baseUrl -}}
{{- .Values.mcpServer.mesh.baseUrl -}}
{{- else -}}
{{- printf "http://%s:%v" (include "mesh.fullname" .) .Values.service.mesh.port -}}
{{- end -}}
{{- end -}}
