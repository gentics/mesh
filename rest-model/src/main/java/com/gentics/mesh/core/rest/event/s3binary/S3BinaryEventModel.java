package com.gentics.mesh.core.rest.event.s3binary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;

/**
 * POJO for s3 binary events.
 */
public class S3BinaryEventModel extends AbstractProjectEventModel {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Object key for the selected S3 binary field in AWS.")
    private String s3ObjectKey;

    public S3BinaryEventModel() {
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    public void setS3ObjectKey(String s3ObjectKey) {
        this.s3ObjectKey = s3ObjectKey;
    }
}
