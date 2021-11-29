package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.event.EventQueueBatch;
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
	HibProject getProject();

	/**
	 * Extract the tags to be set from the TagListUpdateRequest which is expected to be in the body of the action context.
	 * 
	 * @param ac action context
	 * @param batch search queue batch
	 * @return list of tags
	 */
	default List<HibTag> getTagsToSet(InternalActionContext ac, EventQueueBatch batch) {
		TagListUpdateRequest request = JsonUtil.readValue(ac.getBodyAsString(), TagListUpdateRequest.class);
		return getTagsToSet(request.getTags(), ac, batch);
	}

	/**
	 * Try to load the tags which should be set.
	 * @param list List of references which should be loaded
	 * @param ac
	 * @param batch
	 * @return
	 */
	default List<HibTag> getTagsToSet(List<TagReference> list, InternalActionContext ac, EventQueueBatch batch) {
		List<HibTag> tags = new ArrayList<>();
		HibProject project = getProject();
		UserDao userDao = Tx.get().userDao();
		TagDao tagDao = Tx.get().tagDao();
		TagFamilyDao tagFamilyDao = Tx.get().tagFamilyDao();

		HibUser user = ac.getUser();
		for (TagReference tagReference : list) {
			if (!tagReference.isSet()) {
				throw error(BAD_REQUEST, "tag_error_name_or_uuid_missing");
			}
			if (isEmpty(tagReference.getTagFamily())) {
				throw error(BAD_REQUEST, "tag_error_tagfamily_not_set");
			}
			// 1. Locate the tag family
			HibTagFamily tagFamily = tagFamilyDao.findByName(project, tagReference.getTagFamily());
			// Tag Family could not be found so lets create a new one
			if (tagFamily == null) {
				throw error(NOT_FOUND, "tagfamily_not_found", tagReference.getTagFamily());
			}
			// 2. The uuid was specified so lets try to load the tag this way
			if (!isEmpty(tagReference.getUuid())) {
				HibTag tag = tagDao.findByUuid(tagFamily, tagReference.getUuid());
				if (tag == null) {
					throw error(NOT_FOUND, "tag_not_found", tagReference.getUuid());
				}
				if (!userDao.hasPermission(user, tag, READ_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", tag.getUuid(), READ_PERM.getRestPerm().getName());
				}
				tags.add(tag);
			} else {
				HibTag tag = tagDao.findByName(tagFamily, tagReference.getName());
				// Tag with name could not be found so create it
				if (tag == null) {
					if (userDao.hasPermission(user, tagFamily, CREATE_PERM)) {
						tag = tagDao.create(tagFamily, tagReference.getName(), project, user);
						userDao.inheritRolePermissions(user, tagFamily, tag);
						batch.add(tag.onCreated());
					} else {
						throw error(FORBIDDEN, "tag_error_missing_perm_on_tag_family", tagFamily.getName(), tagFamily.getUuid(), tagReference
							.getName());
					}
				} else if (!userDao.hasPermission(user, tag, READ_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", tag.getUuid(), READ_PERM.getRestPerm().getName());
				}

				tags.add(tag);
			}
		}
		return tags;
	}
}
