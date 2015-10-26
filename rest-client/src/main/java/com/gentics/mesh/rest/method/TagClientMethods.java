package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.query.QueryParameterProvider;

public interface TagClientMethods {

	/**
	 * Create a new tag.
	 * 
	 * @param projectName
	 * @param request
	 * @return
	 */
	Future<TagResponse> createTag(String projectName, TagCreateRequest request);

	/**
	 * Load the given tag.
	 * 
	 * @param projectName
	 * @param uuid
	 * @return
	 */
	Future<TagResponse> findTagByUuid(String projectName, String uuid);

	/**
	 * Update the tag
	 * 
	 * @param projectName
	 * @param uuid
	 * @param request
	 * @return
	 */
	Future<TagResponse> updateTag(String projectName, String uuid, TagUpdateRequest request);

	/**
	 * Delete the tag.
	 * 
	 * @param projectName
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteTag(String projectName, String uuid);

	/**
	 * Load multiple tags.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	Future<TagListResponse> findTags(String projectName, QueryParameterProvider... parameters);

	//TODO keep this?
	/**
	 * Load a tag using its name.
	 * 
	 * @param projectName
	 * @param name
	 * @return
	 */
	Future<TagResponse> findTagByName(String projectName, String name);

	/**
	 * Load multiple tags that were assigned to a given node.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param parameters
	 * @return
	 */
	Future<TagListResponse> findTagsForNode(String projectName, String nodeUuid, QueryParameterProvider... parameters);

}
