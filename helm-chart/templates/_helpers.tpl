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
