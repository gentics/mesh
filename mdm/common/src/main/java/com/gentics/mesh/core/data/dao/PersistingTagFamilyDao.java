package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A persisting extension to {@link TagFamilyDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingTagFamilyDao extends TagFamilyDao, PersistingRootDao<HibProject, HibTagFamily> {

	Logger log = LoggerFactory.getLogger(PersistingTagFamilyDao.class);

	@Override
	default boolean update(HibProject project, HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the item, if it does not belong to the requested root.
		if (!project.getUuid().equals(tagFamily.getProject().getUuid())) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", tagFamily.getUuid());
		}
		return update(tagFamily, ac, batch);
	}

	@Override
	default HibTagFamily create(HibProject project, String name, HibUser user) {
		return create(project, name, user, null);
	}

	@Override
	default HibTagFamily create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibUser requestUser = ac.getUser();
		UserDao userDao = Tx.get().userDao();
		TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

		String name = requestModel.getName();
		if (StringUtils.isEmpty(name)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}
		HibBaseElement projectTagFamilyRoot = project.getTagFamilyPermissionRoot();

		// Check whether the name is already in-use.
		HibTagFamily conflictingTagFamily = findByName(project, name);
		if (conflictingTagFamily != null) {
			throw conflict(conflictingTagFamily.getUuid(), name, "tagfamily_conflicting_name", name);
		}

		if (!userDao.hasPermission(requestUser, projectTagFamilyRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectTagFamilyRoot.getUuid(),
					CREATE_PERM.getRestPerm().getName());
		}
		HibTagFamily tagFamily = create(project, name, requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, projectTagFamilyRoot, tagFamily);

		batch.add(tagFamily.onCreated());
		return tagFamily;
	}


	@Override
	default HibTagFamily create(HibProject project, String name, HibUser user, String uuid) {
		HibTagFamily tagFamily = createPersisted(project, uuid);
		tagFamily.setName(name);
		tagFamily.setCreated(user);

		tagFamily.setProject(project);
		tagFamily.generateBucketId();
		mergeIntoPersisted(project, tagFamily);

		return tagFamily;
	}

	@Override
	default boolean update(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Tx tx = Tx.get();
		HibProject project = tx.getProject(ac);
		String newName = requestModel.getName();

		if (isEmpty(newName)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}

		HibTagFamily tagFamilyWithSameName = findByName(project, newName);
		if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(tagFamily.getUuid())) {
			throw conflict(tagFamilyWithSameName.getUuid(), newName, "tagfamily_conflicting_name", newName);
		}
		if (!tagFamily.getName().equals(newName)) {
			tagFamily.setName(newName);
			batch.add(tagFamily.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	default void delete(HibTagFamily tagFamily, BulkActionContext bac) {
		TagDao tagDao = Tx.get().tagDao();

		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + tagFamily.getName() + "}");
		}

		// Delete all the tags of the tag root
		for (HibTag tag : tagDao.findAll(tagFamily).list()) {
			tagDao.delete(tag, bac);
		}

		bac.add(tagFamily.onDeleted());

		// Now delete the tag root element'
		deletePersisted(tagFamily.getProject(), tagFamily);
		bac.process();
	}

	@Override
	default TagFamilyResponse transformToRestSync(HibTagFamily tagFamily, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		TagFamilyResponse restTagFamily = new TagFamilyResponse();
		if (fields.has("uuid")) {
			restTagFamily.setUuid(tagFamily.getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return restTagFamily;
			}
		}

		if (fields.has("name")) {
			restTagFamily.setName(tagFamily.getName());
		}

		tagFamily.fillCommonRestFields(ac, fields, restTagFamily);

		if (fields.has("perms")) {
			Tx.get().roleDao().setRolePermissions(tagFamily, ac, restTagFamily);
		}

		return restTagFamily;
	}

	@Override
	default void delete(HibProject root, HibTagFamily element, BulkActionContext bac) {
		delete(element, bac);
	}

	@Override
	default void addTag(HibTagFamily tagFamily, HibTag tag) {
		tagFamily.addTag(tag);
	}

	@Override
	default void removeTag(HibTagFamily tagFamily, HibTag tag) {
		tagFamily.removeTag(tag);
	}
}
