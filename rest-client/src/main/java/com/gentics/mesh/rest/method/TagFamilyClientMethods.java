package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.query.impl.PagingParameter;

public interface TagFamilyClientMethods {

	/**
	 * Load the tag family using its uuid.
	 * 
	 * @param projectName
	 * @param uuid
	 * @return
	 */
	Future<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid);

	/**
	 * Load multiple tag families.
	 * 
	 * @param projectName
	 * @param pagingInfo
	 * @return
	 */
	Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameter pagingInfo);

	/**
	 * Create a new tag family.
	 * 
	 * @param projectName
	 * @param request
	 * @return
	 */
	Future<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest request);

	/**
	 * Delete the tag family.
	 * 
	 * @param projectName
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteTagFamily(String projectName, String uuid);

	/**
	 * Update the tag family.
	 * 
	 * @param projectName
	 * @param tagFamilyUuid
	 * @param request
	 * @return
	 */
	Future<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest request);

	/**
	 * Load multiple tag families.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	Future<TagFamilyListResponse> findTagFamilies(String projectName, QueryParameterProvider... parameters);

	/**
	 * Load multiple tags of a given tag family.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<TagListResponse> findTagsForTagFamilies(String projectName, String uuid, QueryParameterProvider... parameters);

}
