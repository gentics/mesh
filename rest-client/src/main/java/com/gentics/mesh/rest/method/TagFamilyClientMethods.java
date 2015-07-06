package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;

public interface TagFamilyClientMethods {

	Future<TagFamilyResponse> findTagFamilyByUuid(String uuid);

	Future<TagFamilyListResponse> findTagFamilies(String projectName);

	Future<TagFamilyResponse> createTagFamily(String project, TagFamilyCreateRequest tagFamilyCreateRequest);

	Future<GenericMessageResponse> deleteTagFamily(String uuid);

}
