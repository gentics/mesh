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
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.gentics.mesh.cache.NameCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persisting extension to {@link TagDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingTagDao extends TagDao, PersistingDaoGlobal<Tag>, PersistingNamedEntityDao<Tag> {

	Logger log = LoggerFactory.getLogger(PersistingTagDao.class);

	@Override
	default Tag loadObjectByUuid(Branch branch, InternalActionContext ac, String tagUuid, InternalPermission perm) {
		Tag tag = branch.findTagByUuid(tagUuid);
		return checkPerms(tag, tagUuid, ac, perm, true);
	}

	@Override
	default Tag findByUuid(Project project, String uuid) {
		return StreamSupport.stream(Tx.get().tagFamilyDao().findAll(project).spliterator(), false)
			.map(tagFamily -> findByUuid(tagFamily, uuid))
			.filter(Objects::nonNull)
			.findAny()
			.orElse(null);
	}

	@Override
	default Tag loadObjectByUuid(Project project, InternalActionContext ac, String uuid, InternalPermission perm) {
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
	default Tag loadObjectByUuid(Project project, InternalActionContext ac, String uuid, InternalPermission perm,
			boolean errorIfNotFound) {
		return Tx.get().tagFamilyDao().findAllStream(project, ac, perm, ac.getPagingParameters(), Optional.empty())
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
	default boolean update(TagFamily tagFamily, Tag tag, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the item, if it does not belong to the requested root.
		if (!tagFamily.getUuid().equals(tag.getProject().getUuid())) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", tag.getUuid());
		}
		return update(tag, ac, batch);
	}

	default String getSubETag(Tag tag, InternalActionContext ac) {
		Tx tx = Tx.get();
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(tag.getLastEditedTimestamp());
		keyBuilder.append(tx.getBranch(ac, tag.getProject()).getUuid());
		return keyBuilder.toString();
	}

	@Override
	default Tag create(TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Tx tx = Tx.get();
		Project project = tx.getProject(ac);
		TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
		String tagName = requestModel.getName();
		if (isEmpty(tagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		}

		UserDao userDao = Tx.get().userDao();
		User requestUser = ac.getUser();
		if (!userDao.hasPermission(requestUser, tagFamily, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", tagFamily.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		Tag conflictingTag = findByName(tagFamily, tagName);
		if (conflictingTag != null) {
			throw conflict(conflictingTag.getUuid(), tagName, "tag_create_tag_with_same_name_already_exists", tagName, tagFamily.getName());
		}

		Tag newTag = create(tagFamily, requestModel.getName(), project, requestUser, uuid);
		userDao.inheritRolePermissions(ac.getUser(), tagFamily, newTag);

		tagFamily.addTag(newTag);

		return newTag;
	}


	@Override
	default Tag create(TagFamily tagFamily, String name, Project project, User creator) {
		return create(tagFamily, name, project, creator, null);
	}

	@Override
	default Tag create(TagFamily tagFamily, String name, Project project, User creator, String uuid) {
		Tag tag = createPersisted(uuid, t -> {
			t.setName(name);
			t.setCreated(creator);
			t.setProject(project);
		});

		tag.generateBucketId();

		// And to the tag family
		tagFamily.addTag(tag);

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);
		addBatchEvent(tag.onCreated());
		uncacheSync(tag);
		return mergeIntoPersisted(tag);
	}

	@Override
	default boolean update(Tag tag, InternalActionContext ac, EventQueueBatch batch) {
		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		String newTagName = requestModel.getName();
		if (isEmpty(newTagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		} else {
			TagFamily tagFamily = tag.getTagFamily();

			// Check for conflicts
			Tag foundTagWithSameName = findByName(tagFamily, newTagName);
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
	default TagResponse transformToRestSync(Tag tag, InternalActionContext ac, int level, String... languageTags) {
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
			TagFamily tagFamily = tag.getTagFamily();
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
	default void delete(TagFamily tagFamily, Tag tag, BulkActionContext bac) {
		delete(tag, bac);
	}

	@Override
	default void delete(Tag tag, BulkActionContext bac) {
		String uuid = tag.getUuid();
		String name = tag.getName();
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag {" + uuid + ":" + name + "}");
		}
		bac.add(tag.onDeleted());

		NodeDao nodeDao = Tx.get().nodeDao();
		// For node which have been previously tagged we need to fire the untagged event.
		for (Branch branch : Tx.get().branchDao().findAll(tag.getProject())) {
			for (Node node : getNodes(tag, branch)) {
				bac.add(nodeDao.onTagged(node, tag, branch, UNASSIGNED));
			}
		}
		deletePersisted(tag);
		bac.process();
	}

	@Override
	default void addTag(Node node, Tag tag, Branch branch) {
		node.addTag(tag, branch);
	}

	@Override
	default Optional<NameCache<Tag>> maybeGetCache() {
		return Tx.maybeGet().map(CommonTx.class::cast).map(tx -> tx.data().mesh().tagNameCache());
	}
}
