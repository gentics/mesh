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
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
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
	public String getAPIPath(HibTag tag, InternalActionContext ac) {
		return boot.get().tagRoot().getAPIPath(tag.toTag(), ac);
	}

	@Override
	public String getETag(HibTag tag, InternalActionContext ac) {
		return boot.get().tagRoot().getETag(tag.toTag(), ac);
	}

	// New Methods

	@Override
	public TraversalResult<? extends HibTag> findAll(HibTagFamily tagFamily) {
		return tagFamily.toTagFamily().findAll();
	}

	@Override
	public TraversalResult<? extends Tag> findAllGlobal() {
		TagRoot tagRoot = boot.get().tagRoot();
		return tagRoot.findAll();
	}

	@Override
	public HibTag loadObjectByUuid(HibBranch branch, InternalActionContext ac, String tagUuid, GraphPermission perm) {
		TagRoot tagRoot = boot.get().tagRoot();
		HibTag tag = branch.findTagByUuid(tagUuid);
		return tagRoot.checkPerms(tag.toTag(), tagUuid, ac, perm, true);
	}

	@Override
	public Tag loadObjectByUuid(HibProject project, InternalActionContext ac, String tagUuid, GraphPermission perm) {
		// TODO this is an old bug in mesh. The global tag root is used to load tags. Instead the project specific tags should be checked.
		// This code is used for branch tagging. The case makes incorrect usage of the root.
		TagRoot tagRoot = boot.get().tagRoot();
		return tagRoot.loadObjectByUuid(ac, tagUuid, perm);
	}

	@Override
	public String getSubETag(HibTag tag, InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(tag.getLastEditedTimestamp());
		keyBuilder.append(ac.getBranch(tag.getProject()).getUuid());
		return keyBuilder.toString();
	}

	@Override
	public boolean update(HibTag tag, InternalActionContext ac, EventQueueBatch batch) {
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
	public HibTag findByName(String name) {
		return boot.get().tagRoot().findByName(name);
	}

	@Override
	public HibTag findByName(HibTagFamily tagFamily, String name) {
		return boot.get().tagRoot().findByName(tagFamily, name);
	}

	@Override
	public Tag findByUuidGlobal(String uuid) {
		TagRoot globalTagRoot = boot.get().tagRoot();
		return globalTagRoot.findByUuid(uuid);
	}

	@Override
	public TagResponse transformToRestSync(HibTag tag, InternalActionContext ac, int level, String... languageTags) {
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
		setRolePermissions(tag, ac, restTag);
		return restTag;

	}

	@Override
	public void delete(HibTag tag, BulkActionContext bac) {
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
		tag.deleteElement();
		bac.process();

	}

	@Override
	public TransformablePage<? extends Node> findTaggedNodes(HibTag tag, HibUser requestUser, HibBranch branch, List<String> languageTags, ContainerType type, PagingParameters pagingInfo) {
		return boot.get().tagRoot().findTaggedNodes(tag, requestUser, branch, languageTags, type, pagingInfo);
	}

	@Override
	public TraversalResult<? extends Node> findTaggedNodes(HibTag tag, InternalActionContext ac) {
		return boot.get().tagRoot().findTaggedNodes(tag, ac);
	}

	@Override
	public TraversalResult<? extends Node> getNodes(HibTag tag, HibBranch branch) {
		return boot.get().tagRoot().getNodes(tag, branch);
	}

	@Override
	public void removeNode(HibTag tag, Node node) {
		tag.toTag().unlinkIn(node, HAS_TAG);
	}

	@Override
	public HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		return create(tagFamily, ac, batch, null);
	}

	@Override
	public HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibProject project = ac.getProject();
		TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
		String tagName = requestModel.getName();
		if (isEmpty(tagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		}

		UserDaoWrapper userDao = Tx.get().data().userDao();
		MeshAuthUser requestUser = ac.getUser();
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
	public HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator) {
		return create(tagFamily, name, project, creator, null);
	}

	@Override
	public HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator, String uuid) {
		return boot.get().tagRoot().create(tagFamily, name, project, creator, uuid);
	}

	@Override
	public long computeGlobalCount() {
		return boot.get().tagRoot().computeCount();
	}
}
