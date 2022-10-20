package com.gentics.mesh.core.rest.error;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * This exception should be thrown when the database is not available due to a backup.
 */
public class BackupInProgressException extends AbstractRestException {

    private static final String TYPE = "backup_in_progress";

    public BackupInProgressException() {
        super(HttpResponseStatus.SERVICE_UNAVAILABLE, "backup_in_progress");
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
