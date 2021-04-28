package com.gentics.mesh.core.rest.node.field.s3binary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class S3BinaryMetadataRequest implements RestModel {
    @JsonProperty(required = true)
    @JsonPropertyDescription("Version number which must be provided in order to handle and detect concurrent changes to the node content.")
    private String version;

    @JsonProperty(required = true)
    @JsonPropertyDescription("ISO 639-1 language tag of the node which provides the image which should be transformed.")
    private String language;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
