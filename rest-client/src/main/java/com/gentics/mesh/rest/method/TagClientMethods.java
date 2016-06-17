package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;

public interface TagClientMethods {

	/**
	 * Create a new tag.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily in which the tag should be created
	 * @param request
	 *            Create request
	 * @return
	 */
	Future<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request);

	/**
	 * Load the tag with the given uuid.
	 * 
	 * @param projectName
	 * @param tagFamilyUuid
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters);

	//
	/**
	 * Update the tag.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily in which the tag is stored
	 * @param uuid
	 *            Uuid of the tag
	 * @param request
	 *            Update request
	 * @return
	 */
	Future<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request);

	/**
	 * Delete the tag.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily
	 * @param uuid
	 *            Uuid of the tag
	 * @return
	 */
	Future<GenericMessageResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid);

	/**
	 * Load multiple tags of a given tag family.
	 * 
	 * @param projectName
	 *            Project name
	 * @param tagFamilyUuid
	 *            Uuid of the parent tag family
	 * @param parameters
	 *            Additional query parameters
	 * @return
	 */
	Future<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters);

	//
	//	//TODO keep this?
	//	/**
	//	 * Load a tag using its name.
	//	 * 
	//	 * @param projectName
	//	 * @param name
	//	 * @return
	//	 */
	//	Future<TagResponse> findTagByName(String projectName, String name);
	//


}
