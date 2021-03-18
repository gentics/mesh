package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.user.UserResponse;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeshAuthUser extends UserResponse {

    @JsonProperty(required = true)
    @JsonPropertyDescription("List of group references to which the user belongs.")
    private List<RoleReference> roles = new ArrayList<>();

    public List<RoleReference> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleReference> roles) {
        this.roles = roles;
    }
}
