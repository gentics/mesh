package com.gentics.mesh.core.data.dao.impl;

import com.gentics.mesh.cli.ODBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.AbstractODBDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBTagFamilyDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.google.common.base.Predicate;
import dagger.Lazy;
import io.vertx.core.logging.Logger;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.vertx.core.logging.LoggerFactory.getLogger;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ODBTagFamilyDaoWrapperImpl extends AbstractODBDaoWrapper<HibTagFamily> implements OrientDBTagFamilyDao {

	private static final Logger log = getLogger(ODBTagFamilyDaoWrapperImpl.class);

	@Inject
	public ODBTagFamilyDaoWrapperImpl(Lazy<ODBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@Override
	public Result<? extends TagFamily> findAllGlobal() {
		return boot.get().tagFamilyRoot().findAll();
	}

	@Override
	public long globalCount() {
		return boot.get().tagFamilyRoot().globalCount();
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
	public HibTagFamily findByUuidGlobal(String uuid) {
		TagFamilyRoot globalTagFamilyRoot = boot.get().tagFamilyRoot();
		return globalTagFamilyRoot.findByUuid(uuid);
	}

	@Override
	public HibTagFamily findByUuid(String uuid) {
		return findByUuidGlobal(uuid);
	}

	@Override
	public TagFamilyResponse transformToRestSync(HibTagFamily tagFamily, InternalActionContext ac, int level,
			String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		TagFamily graphTagFamily = toGraph(tagFamily);

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

		graphTagFamily.fillCommonRestFields(ac, fields, restTagFamily);

		if (fields.has("perms")) {
			setRolePermissions(graphTagFamily, ac, restTagFamily);
		}

		return restTagFamily;

	}

	@Override
	public boolean update(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Tx tx = Tx.get();
		HibProject project = tx.getProject(ac);
		String newName = requestModel.getName();

		if (isEmpty(newName)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}

		TagFamily tagFamilyWithSameName = toGraph(project).getTagFamilyRoot().findByName(newName);
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
	public TagFamily create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibUser requestUser = ac.getUser();
		UserDao userDao = boot.get().userDao();
		TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

		String name = requestModel.getName();
		if (StringUtils.isEmpty(name)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}
		TagFamilyRoot projectTagFamilyRoot = toGraph(project).getTagFamilyRoot();

		// Check whether the name is already in-use.
		TagFamily conflictingTagFamily = projectTagFamilyRoot.findByName(name);
		if (conflictingTagFamily != null) {
			throw conflict(conflictingTagFamily.getUuid(), name, "tagfamily_conflicting_name", name);
		}

		if (!userDao.hasPermission(requestUser, projectTagFamilyRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectTagFamilyRoot.getUuid(),
					CREATE_PERM.getRestPerm().getName());
		}
		TagFamily tagFamily = projectTagFamilyRoot.create(name, requestUser, uuid);
		projectTagFamilyRoot.addTagFamily(tagFamily);
		userDao.inheritRolePermissions(requestUser, projectTagFamilyRoot, tagFamily);

		batch.add(tagFamily.onCreated());
		return tagFamily;
	}

	@Override
	public HibTagFamily create(HibProject project, String name, HibUser user) {
		return toGraph(project).getTagFamilyRoot().create(name, user);
	}

	@Override
	public Result<? extends TagFamily> findAll(HibProject project) {
		return toGraph(project).getTagFamilyRoot().findAll();
	}

	@Override
	public void delete(HibTagFamily tagFamily, BulkActionContext bac) {
		TagDao tagDao = Tx.get().tagDao();

		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + tagFamily.getName() + "}");
		}

		// Delete all the tags of the tag root
		for (HibTag tag : tagDao.findAll(tagFamily)) {
			tagDao.delete(tag, bac);
		}

		bac.add(tagFamily.onDeleted());

		// Now delete the tag root element
		tagFamily.deleteElement();
		bac.process();
	}

	@Override
	public String getETag(HibTagFamily tagfamily, InternalActionContext ac) {
		return toGraph(tagfamily).getETag(ac);
	}

	@Override
	public String getAPIPath(HibTagFamily tagFamily, InternalActionContext ac) {
		return toGraph(tagFamily).getAPIPath(ac);
	}

	@Override
	public void removeTag(HibTagFamily tagFamily, HibTag tag) {
		TagFamily graphTagFamily = toGraph(tagFamily);
		graphTagFamily.removeTag(toGraph(tag));
	}

	@Override
	public void addTag(HibTagFamily tagFamily, HibTag tag) {
		TagFamily graphTagFamily = toGraph(tagFamily);
		graphTagFamily.addTag(toGraph(tag));
	}

	@Override
	public Page<? extends HibTag> getTags(HibTagFamily tagFamily, HibUser user, PagingParameters pagingInfo) {
		return toGraph(tagFamily).getTags(user, pagingInfo);
	}

	@Override
	public HibTagFamily loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid,
			InternalPermission perm, boolean errorIfNotFound) {
		return toGraph(project).getTagFamilyRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public Page<? extends TagFamily> findAll(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo) {
		Project graphProject = toGraph(project);
		return graphProject.getTagFamilyRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo, Predicate<HibTagFamily> filter) {
		Project graphProject = toGraph(project);
		return graphProject.getTagFamilyRoot().findAll(ac, pagingInfo, tagFamily -> {
			return filter.test(tagFamily);
		});
	}

	@Override
	public HibTagFamily findByName(String name) {
		return boot.get().tagFamilyRoot().findByName(name);
	}

	@Override
	public long computeCount(HibProject project) {
		Project graphProject = toGraph(project);
		return graphProject.getTagFamilyRoot().computeCount();
	}

}
