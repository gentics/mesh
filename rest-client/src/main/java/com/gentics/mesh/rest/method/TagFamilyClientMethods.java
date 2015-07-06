package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;

public interface TagFamilyClientMethods {

	Future<TagFamilyResponse> findTagFamilyByUuid(String uuid);

	Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingInfo pagingInfo);

	Future<TagFamilyResponse> createTagFamily(String project, TagFamilyCreateRequest tagFamilyCreateRequest);

	Future<GenericMessageResponse> deleteTagFamily(String uuid);
	
	Future<TagFamilyResponse> updateTagFamily(TagFamilyUpdateRequest tagFamilyUpdateRequest);

}
