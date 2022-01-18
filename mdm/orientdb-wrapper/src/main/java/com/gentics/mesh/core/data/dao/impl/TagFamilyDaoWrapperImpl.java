package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.AbstractRootDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;
import dagger.Lazy;

/**
 * @see TagFamilyDaoWrapper
 */
public class TagFamilyDaoWrapperImpl extends AbstractRootDaoWrapper<TagFamilyResponse, HibTagFamily, TagFamily, HibProject> implements TagFamilyDaoWrapper {

	@Inject
	public TagFamilyDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public Result<? extends TagFamily> findAll() {
		return boot.get().meshRoot().getTagFamilyRoot().findAll();
	}

	@Override
	public HibTagFamily create(HibProject project, String name, HibUser user, String uuid) {
		HibTagFamily hibTagFamily = TagFamilyDaoWrapper.super.create(project, name, user, uuid);
		// add to global root
		TagFamilyRoot root = boot.get().meshRoot().getTagFamilyRoot();
		if (root != null && !root.equals(getRoot(project))) {
			root.addTagFamily(toGraph(hibTagFamily));
		}

		return hibTagFamily;
	}

	@Override
	public long count() {
		return boot.get().meshRoot().getTagFamilyRoot().globalCount();
	}

	@Override
	public HibTagFamily findByName(HibProject project, String name) {
		TagFamilyRoot root = toGraph(project).getTagFamilyRoot();
		TagFamily tagFamily = root.findByName(name);
		return tagFamily;
	}

	@Override
	public HibTagFamily findByUuid(HibProject project, String uuid) {
		TagFamilyRoot root = toGraph(project).getTagFamilyRoot();
		TagFamily tagFamily = root.findByUuid(uuid);
		return tagFamily;
	}

	@Override
	public HibTagFamily findByUuid(String uuid) {
		TagFamilyRoot globalTagFamilyRoot = boot.get().meshRoot().getTagFamilyRoot();
		return globalTagFamilyRoot.findByUuid(uuid);
	}

	@Override
	public Result<? extends TagFamily> findAll(HibProject project) {
		return toGraph(project).getTagFamilyRoot().findAll();
	}

	@Override
	public Page<? extends HibTag> getTags(HibTagFamily tagFamily, HibUser user, PagingParameters pagingInfo) {
		return toGraph(tagFamily).getTags(user, pagingInfo);
	}

	@Override
	public Page<? extends TagFamily> findAll(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo) {
		Project graphProject = toGraph(project);
		return graphProject.getTagFamilyRoot().findAll(ac, pagingInfo);
	}

	@Override
	public HibTagFamily findByName(String name) {
		return boot.get().meshRoot().getTagFamilyRoot().findByName(name);
	}

	@Override
	public long count(HibProject project) {
		Project graphProject = toGraph(project);
		return graphProject.getTagFamilyRoot().computeCount();
	}

	@Override
	public Stream<? extends HibTagFamily> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).getTagFamilyRoot().findAllStream(ac, permission);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(HibProject root, InternalActionContext ac, PagingParameters pagingInfo,
			java.util.function.Predicate<HibTagFamily> extraFilter) {
		return toGraph(root).getTagFamilyRoot().findAll(ac, pagingInfo, t -> extraFilter.test(t));
	}

	@Override
	public Page<? extends HibTagFamily> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).getTagFamilyRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public void addItem(HibProject root, HibTagFamily item) {
		toGraph(root).getTagFamilyRoot().addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibProject root, HibTagFamily item) {
		toGraph(root).getTagFamilyRoot().removeItem(toGraph(item));
	}

	@Override
	public String getRootLabel(HibProject root) {
		return toGraph(root).getTagFamilyRoot().getRootLabel();
	}

	@Override
	public Class<? extends HibTagFamily> getPersistenceClass(HibProject root) {
		return toGraph(root).getTagFamilyRoot().getPersistanceClass();
	}

	@Override
	public long globalCount(HibProject root) {
		return toGraph(root).getTagFamilyRoot().globalCount();
	}

	@Override
	public Page<? extends HibTagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getTagFamilyRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<HibTagFamily> extraFilter) {
		return boot.get().meshRoot().getTagFamilyRoot().findAll(ac, pagingInfo, e -> extraFilter.test(e));
	}

	@Override
	protected RootVertex<TagFamily> getRoot(HibProject root) {
		return toGraph(root).getTagFamilyRoot();
	}

	@Override
	public void onRootDeleted(HibProject root, BulkActionContext bac) {
		TagFamilyDaoWrapper.super.onRootDeleted(root, bac);
		toGraph(root).getTagFamilyRoot().delete(bac);
	}
}
