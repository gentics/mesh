package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.dao.PersistingTagFamilyDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagFamilyImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Tag family DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class TagFamilyDaoImpl extends AbstractHibRootDao<TagFamily, TagFamilyResponse, HibTagFamilyImpl, Project, HibProjectImpl> implements PersistingTagFamilyDao {

	private final TagDaoImpl tagDao;
	
	@Inject
	public TagFamilyDaoImpl(RootDaoHelper<TagFamily, HibTagFamilyImpl, Project, HibProjectImpl> rootDaoHelper,
			HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, 
			EventFactory eventFactory, TagDaoImpl tagDao, Lazy<Vertx> vertx) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.tagDao = tagDao;
	}

	@Override
	public Page<? extends Tag> getTags(TagFamily tagFamily, User user, PagingParameters pagingInfo) {
		CriteriaQuery<HibTagImpl> query = cb().createQuery(HibTagImpl.class);
		Root<HibTagImpl> root = query.from(HibTagImpl.class);
		query.select(root);
		query.where(cb().equal(root.get("tagFamily"), tagFamily));

		daoHelper.addPermissionRestriction(query, root, user, READ_PERM);

		return daoHelper.getResultPage(query, pagingInfo);
	}

	@Override
	public Result<? extends TagFamily> findAll(Project project) {
		return new TraversalResult<>(((HibProjectImpl) project).getTagFamilies());
	}

	@Override
	public Stream<? extends TagFamily> findAllStream(Project project, InternalActionContext ac,
			InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeNativeFilter) {
		// TODO FIXME this fix belongs to PersistingTagDao
		if (paging == null) {
			paging = ac.getPagingParameters();
		}
		return StreamSupport.stream(rootDaoHelper.findAllInRoot(project, ac, paging, maybeNativeFilter, null, permission).spliterator(), false);
	}

	@Override
	public long countAll(Project root, InternalActionContext ac, InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		if (paging == null) {
			paging = ac.getPagingParameters();
		}
		return rootDaoHelper.countAllInRoot(root, ac, paging, maybeFilter, permission);
	}

	@Override
	public Page<? extends TagFamily> findAll(Project project, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), null, true);
	}

	@Override
	public Page<? extends TagFamily> findAll(Project project, InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<TagFamily> extraFilter) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), extraFilter, true);
	}

	@Override
	public Page<? extends TagFamily> findAllNoPerm(Project project, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), null, false);
	}

	@Override
	public TagFamily findByName(Project project, String name) {
		return HibernateTx.get().data().mesh().tagFamilyNameCache().get(getCacheKey(project, name), key -> {
			return firstOrNull(rootDaoHelper.findByElementInRoot(project, null, "name", name, null));
		});
	}

	@Override
	public void addItem(Project root, TagFamily item) {
		HibProjectImpl project = (HibProjectImpl) root;
		project.addTagFamily(item);
	}

	@Override
	public void removeItem(Project root, TagFamily item) {
		HibProjectImpl project = (HibProjectImpl) root;
		project.removeTagFamily(item);
	}

	@Override
	public Class<? extends TagFamily> getPersistenceClass(Project root) {
		return HibTagFamilyImpl.class;
	}

	@Override
	public TagFamily createPersisted(Project root, String uuid, Consumer<TagFamily> inflater) {
		HibProjectImpl project = (HibProjectImpl) root;
		HibTagFamilyImpl hibTagFamily = daoHelper.create(uuid, f -> {
			f.setProject(project);
			inflater.accept(f);	
		});
		project.addTagFamily(hibTagFamily);
		return hibTagFamily;
	}

	@Override
	public TagFamily mergeIntoPersisted(Project root, TagFamily entity) {
		return em().merge(entity);
	}

	@Override
	public Function<TagFamily, Project> rootGetter() {
		return TagFamily::getProject;
	}

	@Override
	public void deletePersisted(Project root, TagFamily entity) {
		HibProjectImpl project = (HibProjectImpl) root;
		project.removeTagFamily(entity);
		em().remove(entity);
	}

	@Override
	public BaseElement resolveToElement(BaseElement permissionRoot, Project root, Stack<String> stack) {
		if (stack.isEmpty()) {
			return permissionRoot;
		} else {
			String uuidSegment = stack.pop();
			TagFamily tagFamily = findByUuid(uuidSegment);
			if (stack.isEmpty()) {
				return tagFamily;
			} else {
				String nestedRootNode = stack.pop();
				if (PermissionRoots.TAGS.contentEquals(nestedRootNode)) {
					return tagDao.resolveToElement(tagFamily, tagFamily, stack);
				} else {
					// TODO i18n
					throw error(NOT_FOUND, "Unknown tagFamily element {" + nestedRootNode + "}");
				}
			}
		}
	}

	@Override
	public long count() {
		return daoHelper.count();
	}

	@Override
	public Result<? extends TagFamily> findAll() {
		return daoHelper.findAll();
	}

	@Override
	public TagFamily loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return daoHelper.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TagFamily findByUuid(String uuid) {
		return daoHelper.findByUuid(uuid);
	}

	@Override
	public Page<? extends TagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return PersistingRootDao.shouldSort(pagingInfo) ? daoHelper.findAll(ac, READ_PERM, pagingInfo, Optional.empty()) : daoHelper.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends TagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<TagFamily> extraFilter) {
		return daoHelper.findAll(ac, pagingInfo, extraFilter, true);
	}

	@Override
	public Page<? extends TagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return daoHelper.findAll(ac, InternalPermission.READ_PERM, pagingInfo, Optional.ofNullable(extraFilter));
	}

	@Override
	public TagFamily findByName(String name) {
		return HibernateTx.get().data().mesh().tagFamilyNameCache().get(name, familyName -> {
			return daoHelper.findByName(familyName);
		});
	}

	@Override
	public TagFamily loadObjectByUuid(InternalActionContext ac, String userUuid, InternalPermission perm) {
		return daoHelper.loadObjectByUuid(ac, userUuid, perm);
	}
}
