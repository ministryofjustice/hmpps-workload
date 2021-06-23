    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "aws,logstash,stdout"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: DATABASE_USERNAME
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: database_username

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: database_password

  - name: DATABASE_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: rds_instance_endpoint
{{- end -}}
