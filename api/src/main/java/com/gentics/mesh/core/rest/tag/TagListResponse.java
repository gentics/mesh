package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.ListResponse;

/**
 * Basic list response for tags.
 * @deprecated Use {@link ListResponse} with generics instead
 */
@Deprecated
public class TagListResponse extends ListResponse<TagResponse> {

	public TagListResponse() {
	}

}
