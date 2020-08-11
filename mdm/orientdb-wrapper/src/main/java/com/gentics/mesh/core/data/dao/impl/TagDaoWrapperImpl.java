package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.TagWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagDaoWrapperImpl extends AbstractDaoWrapper implements TagDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(TagDaoWrapperImpl.class);

	@Inject
	public TagDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	@Override
	public String getAPIPath(Tag tag, InternalActionContext ac) {
		return boot.get().tagRoot().getAPIPath(tag, ac);
	}

	@Override
	public String getETag(Tag tag, InternalActionContext ac) {
		return boot.get().tagRoot().getETag(tag, ac);
	}

	// New Methods

	@Override
	public TraversalResult<? extends Tag> findAll(TagFamily tagFamily) {
		return tagFamily.findAll();
	}

	@Override
	public TraversalResult<? extends Tag> findAllGlobal() {
		TagRoot tagRoot = boot.get().tagRoot();
		return tagRoot.findAll();
	}

	@Override
	public Tag loadObjectByUuid(Branch branch, InternalActionContext ac, String tagUuid, GraphPermission perm) {
		TagRoot tagRoot = boot.get().tagRoot();
		Tag tag = branch.findTagByUuid(tagUuid);
		return TagWrapper.wrap(tagRoot.checkPerms(tag, tagUuid, ac, perm, true));
	}

	@Override
	public Tag loadObjectByUuid(Project project, InternalActionContext ac, String tagUuid, GraphPermission perm) {
		// TODO this is an old bug in mesh. The global tag root is used to load tags. Instead the project specific tags should be checked.
		// This code is used for branch tagging. The case makes incorrect usage of the root.
		TagRoot tagRoot = boot.get().tagRoot();
		return TagWrapper.wrap(tagRoot.loadObjectByUuid(ac, tagUuid, perm));
	}

	@Override
	public String getSubETag(Tag tag, InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(tag.getLastEditedTimestamp());
		keyBuilder.append(ac.getBranch(tag.getProject()).getUuid());
		return keyBuilder.toString();
	}

	@Override
	public boolean update(Tag tag, InternalActionContext ac, EventQueueBatch batch) {
		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		String newTagName = requestModel.getName();
		if (isEmpty(newTagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		} else {
			TagFamily tagFamily = tag.getTagFamily();

			// Check for conflicts
			Tag foundTagWithSameName = tagFamily.findByName(newTagName);
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
	public Tag findByName(TagFamily tagFamily, String name) {
		return tagFamily.findByName(name);
	}

	@Override
	public Tag findByUuidGlobal(String uuid) {
		TagRoot globalTagRoot = boot.get().tagRoot();
		return globalTagRoot.findByUuid(uuid);
	}

	@Override
	public TagResponse transformToRestSync(Tag tag, InternalActionContext ac, int level, String... languageTags) {
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
		setRolePermissions(tag, ac, restTag);
		return restTag;

	}

	@Override
	public void delete(Tag tag, BulkActionContext bac) {
		String uuid = tag.getUuid();
		String name = tag.getName();
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag {" + uuid + ":" + name + "}");
		}
		bac.add(tag.onDeleted());

		// For node which have been previously tagged we need to fire the untagged event.
		for (Branch branch : tag.getProject().getBranchRoot().findAll()) {
			for (Node node : getNodes(tag, branch)) {
				bac.add(node.onTagged(tag, branch, UNASSIGNED));
			}
		}
		tag.getElement().remove();
		bac.process();

	}

	@Override
	public TransformablePage<? extends Node> findTaggedNodes(Tag tag, HibUser requestUser, Branch branch, List<String> languageTags, ContainerType type, PagingParameters pagingInfo) {
		return boot.get().tagRoot().findTaggedNodes(tag, requestUser, branch, languageTags, type, pagingInfo);
	}

	@Override
	public TraversalResult<? extends Node> findTaggedNodes(Tag tag, InternalActionContext ac) {
		return boot.get().tagRoot().findTaggedNodes(tag, ac);
	}

	@Override
	public TraversalResult<? extends Node> getNodes(Tag tag, Branch branch) {
		return boot.get().tagRoot().getNodes(tag, branch);
	}

	@Override
	public void removeNode(Tag tag, Node node) {
		tag.unlinkIn(node, HAS_TAG);
	}

	@Override
	public Tag create(TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		return create(tagFamily, ac, batch, null);
	}

	@Override
	public Tag create(TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Project project = ac.getProject();
		TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
		String tagName = requestModel.getName();
		if (isEmpty(tagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		}

		UserDaoWrapper userDao= Tx.get().data().userDao();
		MeshAuthUser requestUser = ac.getUser();
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

		batch.add(newTag.onCreated());
		return newTag;
	}

	@Override
	public Tag create(TagFamily tagFamily, String name, Project project, HibUser creator) {
		return create(tagFamily, name, project, creator, null);
	}

	@Override
	public Tag create(TagFamily tagFamily, String name, Project project, HibUser creator, String uuid) {
		return boot.get().tagRoot().create(tagFamily, name, project, creator, uuid);
	}

	@Override
	public long computeGlobalCount() {
		return boot.get().tagRoot().computeCount();
	}
}
