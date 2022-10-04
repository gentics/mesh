package com.gentics.mesh.error;

import com.gentics.mesh.core.data.relationship.GraphPermission;

/**
 * An exception to be thrown when user is missing required permissions
 */
public class MissingPermissionException extends RuntimeException {

    private final GraphPermission permission;
    private final String elementUuid;

    public MissingPermissionException(GraphPermission permission, String elementUuid) {
        this.permission = permission;
        this.elementUuid = elementUuid;
    }

    /**
     * @return The required permissions
     */
    public GraphPermission getPermission() {
        return permission;
    }

    /**
     *
     * @return the elementUuid for which user is missing permissions
     */
    public String getElementUuid() {
        return elementUuid;
    }

}
