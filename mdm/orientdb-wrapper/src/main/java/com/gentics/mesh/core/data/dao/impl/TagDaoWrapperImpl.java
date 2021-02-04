package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBTagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagDaoWrapperImpl extends AbstractDaoWrapper<HibTag> implements OrientDBTagDao {

	private static final Logger log = LoggerFactory.getLogger(TagDaoWrapperImpl.class);

	@Inject
	public TagDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@Override
	public String getAPIPath(HibTag tag, InternalActionContext ac) {
		Tag graphTag = toGraph(tag);
		return graphTag.getAPIPath(ac);
	}

	@Override
	public String getETag(HibTag tag, InternalActionContext ac) {
		Tag graphTag = toGraph(tag);
		return graphTag.getETag(ac);
	}

	// New Methods

	@Override
	public Result<? extends HibTag> findAll(HibTagFamily tagFamily) {
		TagFamily graphTagFamily = toGraph(tagFamily);
		return graphTagFamily.findAll();
	}

	@Override
	public Result<? extends Tag> findAllGlobal() {
		TagRoot tagRoot = boot.get().tagRoot();
		return tagRoot.findAll();
	}

	@Override
	public HibTag loadObjectByUuid(HibBranch branch, InternalActionContext ac, String tagUuid, InternalPermission perm) {
		TagRoot tagRoot = boot.get().tagRoot();
		HibTag tag = branch.findTagByUuid(tagUuid);
		return tagRoot.checkPerms(toGraph(tag), tagUuid, ac, perm, true);
	}

	@Override
	public Tag loadObjectByUuid(HibProject project, InternalActionContext ac, String tagUuid, InternalPermission perm) {
		// TODO this is an old bug in mesh. The global tag root is used to load tags. Instead the project specific tags should be checked.
		// This code is used for branch tagging. The case makes incorrect usage of the root.
		TagRoot tagRoot = boot.get().tagRoot();
		return tagRoot.loadObjectByUuid(ac, tagUuid, perm);
	}

	@Override
	public String getSubETag(HibTag tag, InternalActionContext ac) {
		Tx tx = Tx.get();
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(tag.getLastEditedTimestamp());
		keyBuilder.append(tx.getBranch(ac, tag.getProject()).getUuid());
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
		return HibClassConverter.toGraph(tagFamily).findByName(name);
	}

	@Override
	public HibTag findByUuid(HibProject project, String uuid) {
		Project graphProject = HibClassConverter.toGraph(project);
		// TODO this is actually wrong. We should actually search within the tag familes of the project instead.
		Tag tag = boot.get().tagRoot().findByUuid(uuid);
		return tag;
	}

	@Override
	public HibTag findByUuid(HibTagFamily tagFamily, String uuid) {
		TagFamily graphTagFamily = HibClassConverter.toGraph(tagFamily);
		return graphTagFamily.findByUuid(uuid);
	}

	@Override
	public Page<? extends HibTag> findAll(HibTagFamily tagFamily, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibTag> extraFilter) {
		TagFamily graphTagFamily = HibClassConverter.toGraph(tagFamily);
		return graphTagFamily.findAll(ac, pagingInfo, tag -> {
			return extraFilter.test(tag);
		});
	}

	@Override
	public Page<? extends HibTag> findAll(HibTagFamily tagFamily, InternalActionContext ac, PagingParameters pagingParameters) {
		TagFamily graphTagFamily = HibClassConverter.toGraph(tagFamily);
		return graphTagFamily.findAll(ac, pagingParameters);
	}

	@Override
	public Tag findByUuidGlobal(String uuid) {
		TagRoot globalTagRoot = boot.get().tagRoot();
		return globalTagRoot.findByUuid(uuid);
	}

	@Override
	public TagResponse transformToRestSync(HibTag tag, InternalActionContext ac, int level, String... languageTags) {
		Tag graphTag = toGraph(tag);
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

		graphTag.fillCommonRestFields(ac, fields, restTag);
		setRolePermissions(graphTag, ac, restTag);
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
		Project graphProject = toGraph(tag.getProject());
		for (Branch branch : graphProject.getBranchRoot().findAll()) {
			for (HibNode node : getNodes(tag, branch)) {
				bac.add(toGraph(node).onTagged(tag, branch, UNASSIGNED));
			}
		}
		tag.deleteElement();
		bac.process();

	}

	@Override
	public Page<? extends Node> findTaggedNodes(HibTag tag, HibUser requestUser, HibBranch branch, List<String> languageTags,
		ContainerType type, PagingParameters pagingInfo) {
		return boot.get().tagRoot().findTaggedNodes(toGraph(tag), requestUser, toGraph(branch), languageTags, type, pagingInfo);
	}

	@Override
	public Result<? extends HibNode> findTaggedNodes(HibTag tag, InternalActionContext ac) {
		return boot.get().tagRoot().findTaggedNodes(tag, ac);
	}

	@Override
	public Result<? extends HibNode> getNodes(HibTag tag, HibBranch branch) {
		return boot.get().tagRoot().getNodes(toGraph(tag), branch);
	}

	@Override
	public void removeNode(HibTag tag, HibNode node) {
		toGraph(tag).unlinkIn(toGraph(node), HAS_TAG);
	}

	@Override
	public HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		return create(tagFamily, ac, batch, null);
	}

	@Override
	public HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Tx tx = Tx.get();
		TagFamily graphTagFamily = toGraph(tagFamily);
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

		Tag newGraphTag = toGraph(newTag);
		graphTagFamily.addTag(newGraphTag);

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
	public long globalCount() {
		return boot.get().tagRoot().globalCount();
	}

	@Override
	public long computeCount(HibTagFamily tagFamily) {
		TagFamily graphTagFamily = toGraph(tagFamily);
		return graphTagFamily.computeCount();
	}

	@Override
	public void addTag(HibNode node, HibTag tag, HibBranch branch) {
		toGraph(node).addTag(tag, branch);
	}

	@Override
	public void removeTag(HibNode node, HibTag tag, HibBranch branch) {
		toGraph(node).removeTag(tag, branch);
	}

	@Override
	public void removeAllTags(HibNode node, HibBranch branch) {
		toGraph(node).removeAllTags(branch);
	}

	@Override
	public Result<HibTag> getTags(HibNode node, HibBranch branch) {
		return toGraph(node).getTags(branch);
	}

	@Override
	public Page<? extends HibTag> getTags(HibNode node, HibUser user, PagingParameters params, HibBranch branch) {
		return toGraph(node).getTags(user, params, branch);
	}

	@Override
	public boolean hasTag(HibNode node, HibTag tag, HibBranch branch) {
		return toGraph(node).hasTag(tag, branch);
	}
}
