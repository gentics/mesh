package com.gentics.mesh.error;


import com.gentics.mesh.core.rest.common.Permission;

/**
 * An exception to be thrown when user is missing required permissions
 */
public class MissingPermissionException extends RuntimeException {

    private final Permission permission;
    private final String elementUuid;

    public MissingPermissionException(Permission permission, String elementUuid) {
        this.permission = permission;
        this.elementUuid = elementUuid;
    }

    /**
     * @return The required permissions
     */
    public Permission getPermission() {
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
