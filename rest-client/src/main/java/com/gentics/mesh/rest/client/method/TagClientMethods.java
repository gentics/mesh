package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

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
	MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request);

	/**
	 * Load the tag with the given uuid.
	 * 
	 * @param projectName
	 * @param tagFamilyUuid
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters);

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
	MeshRequest<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request);

	/**
	 * Create the tag with the given uuid
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily in which the tag is stored
	 * @param uuid
	 *            Uuid of the new tag
	 * @param request
	 *            Create request
	 * @return
	 */
	MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, String uuid, TagCreateRequest request);

	/**
	 * Delete the tag.
	 *
	 * @param projectName   Name of the project
	 * @param tagFamilyUuid Uuid of the tagfamily
	 * @param uuid          Uuid of the tag
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid);

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
	MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters);

}
