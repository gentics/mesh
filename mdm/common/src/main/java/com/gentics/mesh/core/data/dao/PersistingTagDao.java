package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A persisting extension to {@link TagDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingTagDao extends TagDao, PersistingDaoGlobal<HibTag> {

	Logger log = LoggerFactory.getLogger(PersistingTagDao.class);

	@Override
	default HibTag loadObjectByUuid(HibBranch branch, InternalActionContext ac, String tagUuid, InternalPermission perm) {
		HibTag tag = branch.findTagByUuid(tagUuid);
		return checkPerms(tag, tagUuid, ac, perm, true);
	}

	@Override
	default HibTag findByUuid(HibProject project, String uuid) {
		return StreamSupport.stream(Tx.get().tagFamilyDao().findAll(project).spliterator(), false)
			.map(tagFamily -> findByUuid(tagFamily, uuid))
			.filter(Objects::nonNull)
			.findAny()
			.orElse(null);
	}

	@Override
	default HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm) {
		return loadObjectByUuid(project, ac, uuid, perm, true);
	}

	/**
	 * Find the tag with given UUID among the tag families of a given project.
	 * 
	 * @param project
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	default HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm,
			boolean errorIfNotFound) {
		return Tx.get().tagFamilyDao().findAllStream(project, ac, perm)
				.map(tagFamily -> loadObjectByUuid(tagFamily, ac, uuid, perm, false))
				.filter(Objects::nonNull)
				.map(tag -> checkPerms(tag, uuid, ac, perm, errorIfNotFound))
				.findAny()
				.orElseGet(() -> {
					if (errorIfNotFound) {
						throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
					} else {
						return null;
					}
				});
	}

	@Override
	default boolean update(HibTagFamily tagFamily, HibTag tag, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the item, if it does not belong to the requested root.
		if (!tagFamily.getUuid().equals(tag.getProject().getUuid())) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", tag.getUuid());
		}
		return update(tag, ac, batch);
	}

	default String getSubETag(HibTag tag, InternalActionContext ac) {
		Tx tx = Tx.get();
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(tag.getLastEditedTimestamp());
		keyBuilder.append(tx.getBranch(ac, tag.getProject()).getUuid());
		return keyBuilder.toString();
	}

	@Override
	default HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Tx tx = Tx.get();
		HibProject project = tx.getProject(ac);
		TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
		String tagName = requestModel.getName();
		if (isEmpty(tagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		}

		UserDao userDao = Tx.get().userDao();
		HibUser requestUser = ac.getUser();
		if (!userDao.hasPermission(requestUser, tagFamily, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", tagFamily.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		HibTag conflictingTag = findByName(tagFamily, tagName);
		if (conflictingTag != null) {
			throw conflict(conflictingTag.getUuid(), tagName, "tag_create_tag_with_same_name_already_exists", tagName, tagFamily.getName());
		}

		HibTag newTag = create(tagFamily, requestModel.getName(), project, requestUser, uuid);
		userDao.inheritRolePermissions(ac.getUser(), tagFamily, newTag);

		tagFamily.addTag(newTag);

		batch.add(newTag.onCreated());
		return newTag;
	}


	@Override
	default HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator) {
		return create(tagFamily, name, project, creator, null);
	}

	@Override
	default HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator, String uuid) {
		HibTag tag = createPersisted(uuid);

		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);
		tag.generateBucketId();

		// And to the tag family
		tagFamily.addTag(tag);

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);
		mergeIntoPersisted(tag);
		return tag;
	}

	@Override
	default boolean update(HibTag tag, InternalActionContext ac, EventQueueBatch batch) {
		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		String newTagName = requestModel.getName();
		if (isEmpty(newTagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		} else {
			HibTagFamily tagFamily = tag.getTagFamily();

			// Check for conflicts
			HibTag foundTagWithSameName = findByName(tagFamily, newTagName);
			if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(tag.getUuid())) {
				throw conflict(foundTagWithSameName.getUuid(), newTagName, "tag_create_tag_with_same_name_already_exists", newTagName, tagFamily
						.getName());
			}

			if (!newTagName.equals(tag.getName())) {
				tag.setEditor(ac.getUser());
				tag.setLastEditedTimestamp();
				tag.setName(newTagName);
				batch.add(tag.onUpdated());
				return true;
			}
		}
		return false;
	}

	@Override
	default TagResponse transformToRestSync(HibTag tag, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		TagResponse restTag = new TagResponse();
		if (fields.has("uuid")) {
			restTag.setUuid(tag.getUuid());
			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return restTag;
			}
		}
		if (fields.has("tagFamily")) {
			HibTagFamily tagFamily = tag.getTagFamily();
			if (tagFamily != null) {
				TagFamilyReference tagFamilyReference = new TagFamilyReference();
				tagFamilyReference.setName(tagFamily.getName());
				tagFamilyReference.setUuid(tagFamily.getUuid());
				restTag.setTagFamily(tagFamilyReference);
			}
		}
		if (fields.has("name")) {
			restTag.setName(tag.getName());
		}

		tag.fillCommonRestFields(ac, fields, restTag);
		Tx.get().roleDao().setRolePermissions(tag, ac, restTag);
		return restTag;

	}

	@Override
	default void delete(HibTagFamily tagFamily, HibTag tag, BulkActionContext bac) {
		delete(tag, bac);
	}

	@Override
	default void delete(HibTag tag, BulkActionContext bac) {
		String uuid = tag.getUuid();
		String name = tag.getName();
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag {" + uuid + ":" + name + "}");
		}
		bac.add(tag.onDeleted());

		NodeDao nodeDao = Tx.get().nodeDao();
		// For node which have been previously tagged we need to fire the untagged event.
		for (HibBranch branch : Tx.get().branchDao().findAll(tag.getProject())) {
			for (HibNode node : getNodes(tag, branch)) {
				bac.add(nodeDao.onTagged(node, tag, branch, UNASSIGNED));
			}
		}
		deletePersisted(tag);
		bac.process();
	}

	@Override
	default void addTag(HibNode node, HibTag tag, HibBranch branch) {
		node.addTag(tag, branch);
	}
}
