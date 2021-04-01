package com.gentics.mesh.core.rest.node.field.s3binary;

import com.gentics.mesh.core.rest.common.RestModel;

import java.util.List;
import java.util.Map;

public class S3RestResponse implements RestModel {

    String presignedUrl;
    String httpRequestMethod;
    Map<String, List<String>> signedHeaders;

    public S3RestResponse(String presignedUrl, String httpRequestMethod, Map<String, List<String>> signedHeaders) {
        this.presignedUrl = presignedUrl;
        this.httpRequestMethod = httpRequestMethod;
        this.signedHeaders = signedHeaders;
    }
}
