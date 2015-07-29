package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;

public interface TagFamilyClientMethods {

	Future<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid);

	Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingInfo pagingInfo);

	Future<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest tagFamilyCreateRequest);

	Future<GenericMessageResponse> deleteTagFamily(String projectName, String uuid);

	Future<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest tagFamilyUpdateRequest);

	Future<TagFamilyListResponse> findTagFamilies(String projectName, QueryParameterProvider... parameters);

	Future<TagListResponse> findTagsForTagFamilies(String projectName, String uuid, QueryParameterProvider... parameters);

}
