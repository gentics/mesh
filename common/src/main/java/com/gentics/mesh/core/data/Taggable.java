package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.json.JsonUtil;

/**
 * A taggable element is a graph element that can reference Tags
 */
public interface Taggable {
	/**
	 * Project to which the element belongs.
	 * 
	 * @return Project of the element
	 */
	Project getProject();

	/**
	 * Extract the tags to be set from the TagListUpdateRequest which is expected to be in the body of the action context.
	 * 
	 * @param ac action context
	 * @param batch search queue batch
	 * @return list of tags
	 */
	default List<Tag> getTagsToSet(InternalActionContext ac, SearchQueueBatch batch) {
		List<Tag> tags = new ArrayList<>();
		Project project = getProject();
		TagListUpdateRequest request = JsonUtil.readValue(ac.getBodyAsString(), TagListUpdateRequest.class);
		TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
		User user = ac.getUser();
		for (TagReference tagReference : request.getTags()) {
			if (!tagReference.isSet()) {
				throw error(BAD_REQUEST, "tag_error_name_or_uuid_missing");
			}
			if (isEmpty(tagReference.getTagFamily())) {
				throw error(BAD_REQUEST, "tag_error_tagfamily_not_set");
			}
			// 1. Locate the tag family
			TagFamily tagFamily = tagFamilyRoot.findByName(tagReference.getTagFamily());
			// Tag Family could not be found so lets create a new one
			if (tagFamily == null) {
				throw error(NOT_FOUND, "object_not_found_for_name", tagReference.getTagFamily());
			}
			// 2. The uuid was specified so lets try to load the tag this way
			if (!isEmpty(tagReference.getUuid())) {
				Tag tag = tagFamily.findByUuid(tagReference.getUuid());
				if (tag == null) {
					throw error(NOT_FOUND, "object_not_found_for_uuid", tagReference.getUuid());
				}
				if (!user.hasPermission(tag, READ_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", tag.getUuid(), READ_PERM.getRestPerm().getName());
				}
				tags.add(tag);
			} else {
				Tag tag = tagFamily.findByName(tagReference.getName());
				// Tag with name could not be found so create it
				if (tag == null) {
					if (user.hasPermission(tagFamily, CREATE_PERM)) {
						tag = tagFamily.create(tagReference.getName(), project, user);
						user.addCRUDPermissionOnRole(tagFamily, CREATE_PERM, tag);
						batch.store(tag, false);
						batch.store(tagFamily, false);
					} else {
						throw error(FORBIDDEN, "tag_error_missing_perm_on_tag_family", tagFamily.getName(), tagFamily.getUuid(), tagReference
							.getName());
					}
				} else if (!user.hasPermission(tag, READ_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", tag.getUuid(), READ_PERM.getRestPerm().getName());
				}

				tags.add(tag);
			}
		}
		return tags;
	}
}
