package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import dagger.Lazy;

/**
 * DAO for {@link HibTag} elements.
 * 
 * TODO MDM Migrate to {@link TagDao}
 */
public class TagDaoWrapperImpl extends AbstractCoreDaoWrapper<TagResponse, HibTag, Tag> implements TagDaoWrapper {

	@Inject
	public TagDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	// New Methods

	@Override
	public Result<? extends HibTag> findAll(HibTagFamily tagFamily) {
		TagFamily graphTagFamily = toGraph(tagFamily);
		return graphTagFamily.findAll();
	}

	@Override
	public Result<? extends Tag> findAll() {
		TagRoot tagRoot = boot.get().meshRoot().getTagRoot();
		return tagRoot.findAll();
	}

// TODO check if still required 
//	@Override
//	public HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String tagUuid, InternalPermission perm) {
//		// TODO this is an old bug in mesh. The global tag root is used to load tags. Instead the project specific tags should be checked.
//		// This code is used for branch tagging. The case makes incorrect usage of the root.
//		return loadObjectByUuid(ac, tagUuid, perm);
//	}

	@Override
	public HibTag findByName(String name) {
		return boot.get().meshRoot().getTagRoot().findByName(name);
	}

	@Override
	public HibTag findByName(HibTagFamily tagFamily, String name) {
		return HibClassConverter.toGraph(tagFamily).findByName(name);
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
	public Tag findByUuid(String uuid) {
		TagRoot globalTagRoot = boot.get().meshRoot().getTagRoot();
		return globalTagRoot.findByUuid(uuid);
	}

	@Override
	public Page<? extends Node> findTaggedNodes(HibTag tag, HibUser requestUser, HibBranch branch, List<String> languageTags,
		ContainerType type, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getTagRoot().findTaggedNodes(toGraph(tag), requestUser, toGraph(branch), languageTags, type, pagingInfo);
	}

	@Override
	public Result<? extends HibNode> findTaggedNodes(HibTag tag, InternalActionContext ac) {
		return boot.get().meshRoot().getTagRoot().findTaggedNodes(tag, ac);
	}

	@Override
	public Result<? extends HibNode> getNodes(HibTag tag, HibBranch branch) {
		return boot.get().meshRoot().getTagRoot().getNodes(toGraph(tag), branch);
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
	public long count() {
		return boot.get().meshRoot().getTagRoot().globalCount();
	}

	@Override
	public long count(HibTagFamily tagFamily) {
		TagFamily graphTagFamily = toGraph(tagFamily);
		return graphTagFamily.computeCount();
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

	@Override
	public Stream<? extends HibTag> findAllStream(HibTagFamily root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).findAllStream(ac, permission);
	}

	@Override
	public Page<? extends HibTag> findAllNoPerm(HibTagFamily root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public void addItem(HibTagFamily root, HibTag item) {
		toGraph(root).addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibTagFamily root, HibTag item) {
		toGraph(root).removeItem(toGraph(item));
	}

	@Override
	public String getRootLabel(HibTagFamily root) {
		return toGraph(root).getRootLabel();
	}

	@Override
	public long globalCount(HibTagFamily root) {
		return toGraph(root).globalCount();
	}

	@Override
	public Page<? extends HibTag> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getTagRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibTag> findAll(InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<HibTag> extraFilter) {
		return boot.get().meshRoot().getTagRoot().findAll(ac, pagingInfo, e -> extraFilter.test(e));
	}

	@Override
	protected RootVertex<Tag> getRoot() {
		return boot.get().meshRoot().getTagRoot();
	}
}
