FROM docker.elastic.co/elasticsearch/elasticsearch-oss:%VERSION%

RUN bin/elasticsearch-plugin install -b ingest-attachment
EXPOSE 9200
EXPOSE 9300
