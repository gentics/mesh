package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;

public interface ETaggable {

    /**
     * Return the etag for the element.
     *
     * @param ac
     * @return Generated etag
     */
    String getETag(InternalActionContext ac);
}
