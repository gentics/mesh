package com.gentics.mesh.core.rest.node.field.s3binary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

import java.util.List;
import java.util.Map;

/**
 * RestModel for the S3 binary upload response
 */
public class S3RestResponse implements RestModel {

    @JsonProperty(required = true)
    @JsonPropertyDescription("presignedUrl")
    String presignedUrl;

    @JsonProperty(required = true)
    @JsonPropertyDescription("httpRequestMethod")
    String httpRequestMethod;

    @JsonProperty(required = true)
    @JsonPropertyDescription("signedHeaders")
    Map<String, List<String>> signedHeaders;

    @JsonProperty(required = false)
    @JsonPropertyDescription("version")
    String version;

    public S3RestResponse(){
    }

    public S3RestResponse(String presignedUrl, String httpRequestMethod, Map<String, List<String>> signedHeaders) {
        this.presignedUrl = presignedUrl;
        this.httpRequestMethod = httpRequestMethod;
        this.signedHeaders = signedHeaders;
    }

    public Map<String, List<String>> getSignedHeaders() {
        return signedHeaders;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
