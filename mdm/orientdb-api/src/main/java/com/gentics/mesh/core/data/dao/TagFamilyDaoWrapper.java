package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.google.common.base.Predicate;

public interface TagFamilyDaoWrapper
		extends TagFamilyDao, DaoWrapper<HibTagFamily>, DaoTransformable<HibTagFamily, TagFamilyResponse> {

	boolean update(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch);

	// Find all tag families across all project.
	// TODO rename this method once ready
	Result<? extends HibTagFamily> findAllGlobal();

	Result<? extends HibTagFamily> findAll(HibProject project);

	HibTagFamily create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid);

	HibTagFamily findByName(String name);

	HibTagFamily findByName(HibProject project, String name);

	HibTagFamily findByUuid(String uuid);

	HibTagFamily findByUuid(HibProject project, String uuid);

	void addTag(HibTagFamily tagFamily, HibTag tag);

	void removeTag(HibTagFamily tagFamily, HibTag tag);

	void delete(HibTagFamily tagFamily, BulkActionContext bac);

	String getETag(HibTagFamily tagfamily, InternalActionContext mockActionContext);

	String getAPIPath(HibTagFamily tagFamily, InternalActionContext ac);

	Page<? extends HibTag> getTags(HibTagFamily tagFamily, MeshAuthUser user, PagingParameters pagingInfo);

	HibTagFamily create(HibProject project, String name, HibUser user);

	Page<? extends TagFamily> findAll(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo);

	Page<? extends HibTagFamily> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<HibTagFamily> filter);

	HibTagFamily loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm,
			boolean errorIfNotFound);

	long computeCount(HibProject project);

}
