package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Arrays;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.dao.PersistingTagFamilyDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
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
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Tag family DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class TagFamilyDaoImpl extends AbstractHibRootDao<HibTagFamily, TagFamilyResponse, HibTagFamilyImpl, HibProject, HibProjectImpl> implements PersistingTagFamilyDao {
	public static final String[] SORT_FIELDS = new String[] { "name" };

	private final TagDaoImpl tagDao;
	
	@Inject
	public TagFamilyDaoImpl(RootDaoHelper<HibTagFamily, HibTagFamilyImpl, HibProject, HibProjectImpl> rootDaoHelper,
			HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, 
			EventFactory eventFactory, TagDaoImpl tagDao, Lazy<Vertx> vertx) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.tagDao = tagDao;
	}

	@Override
	public Page<? extends HibTag> getTags(HibTagFamily tagFamily, HibUser user, PagingParameters pagingInfo) {
		CriteriaQuery<HibTagImpl> query = cb().createQuery(HibTagImpl.class);
		Root<HibTagImpl> root = query.from(HibTagImpl.class);
		query.select(root);
		query.where(cb().equal(root.get("tagFamily"), tagFamily));

		daoHelper.addPermissionRestriction(query, root, user, READ_PERM);

		return daoHelper.getResultPage(query, pagingInfo);
	}

	@Override
	public Result<? extends HibTagFamily> findAll(HibProject project) {
		return new TraversalResult<>(((HibProjectImpl) project).getTagFamilies());
	}

	@Override
	public Stream<? extends HibTagFamily> findAllStream(HibProject project, InternalActionContext ac,
			InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeNativeFilter) {
		// TODO FIXME this fix belongs to PersistingTagDao
		if (paging == null) {
			paging = ac.getPagingParameters();
		}
		return StreamSupport.stream(rootDaoHelper.findAllInRoot(project, ac, paging, maybeNativeFilter, null, permission).spliterator(), false);
	}

	@Override
	public long countAll(HibProject root, InternalActionContext ac, InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		if (paging == null) {
			paging = ac.getPagingParameters();
		}
		return rootDaoHelper.countAllInRoot(root, ac, paging, maybeFilter, permission);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), null, true);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<HibTagFamily> extraFilter) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), extraFilter, true);
	}

	@Override
	public Page<? extends HibTagFamily> findAllNoPerm(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), null, false);
	}

	@Override
	public HibTagFamily findByName(HibProject project, String name) {
		return HibernateTx.get().data().mesh().tagFamilyNameCache().get(getCacheKey(project, name), key -> {
			return firstOrNull(rootDaoHelper.findByElementInRoot(project, null, "name", name, null));
		});
	}

	@Override
	public void addItem(HibProject root, HibTagFamily item) {
		HibProjectImpl project = (HibProjectImpl) root;
		project.addTagFamily(item);
	}

	@Override
	public void removeItem(HibProject root, HibTagFamily item) {
		HibProjectImpl project = (HibProjectImpl) root;
		project.removeTagFamily(item);
	}

	@Override
	public Class<? extends HibTagFamily> getPersistenceClass(HibProject root) {
		return HibTagFamilyImpl.class;
	}

	@Override
	public HibTagFamily createPersisted(HibProject root, String uuid, Consumer<HibTagFamily> inflater) {
		HibProjectImpl project = (HibProjectImpl) root;
		HibTagFamilyImpl hibTagFamily = daoHelper.create(uuid, f -> {
			f.setProject(project);
			inflater.accept(f);	
		});
		project.addTagFamily(hibTagFamily);
		return hibTagFamily;
	}

	@Override
	public HibTagFamily mergeIntoPersisted(HibProject root, HibTagFamily entity) {
		return em().merge(entity);
	}

	@Override
	public Function<HibTagFamily, HibProject> rootGetter() {
		return HibTagFamily::getProject;
	}

	@Override
	public void deletePersisted(HibProject root, HibTagFamily entity) {
		HibProjectImpl project = (HibProjectImpl) root;
		project.removeTagFamily(entity);
		em().remove(entity);
	}

	@Override
	public HibBaseElement resolveToElement(HibBaseElement permissionRoot, HibProject root, Stack<String> stack) {
		if (stack.isEmpty()) {
			return permissionRoot;
		} else {
			String uuidSegment = stack.pop();
			HibTagFamily tagFamily = findByUuid(uuidSegment);
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
	public Result<? extends HibTagFamily> findAll() {
		return daoHelper.findAll();
	}

	@Override
	public HibTagFamily loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return daoHelper.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public HibTagFamily findByUuid(String uuid) {
		return daoHelper.findByUuid(uuid);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return PersistingRootDao.shouldSort(pagingInfo) ? daoHelper.findAll(ac, READ_PERM, pagingInfo, Optional.empty()) : daoHelper.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibTagFamily> extraFilter) {
		return PersistingRootDao.shouldSort(pagingInfo) 
				? new DynamicStreamPageImpl<>(
						// Since we do not know yet what the extra filter gives to us, we dare at this moment no paging - it will be applied at the PageImpl
						daoHelper.findAll(ac, Optional.empty(), ((PagingParameters) new PagingParametersImpl().putSort(pagingInfo.getSort())), Optional.empty()).stream(), 
						pagingInfo, extraFilter, false)
				: daoHelper.findAll(ac, pagingInfo, extraFilter, true);
	}

	@Override
	public Page<? extends HibTagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return daoHelper.findAll(ac, InternalPermission.READ_PERM, pagingInfo, Optional.ofNullable(extraFilter));
	}

	@Override
	public HibTagFamily findByName(String name) {
		return HibernateTx.get().data().mesh().tagFamilyNameCache().get(name, familyName -> {
			return daoHelper.findByName(familyName);
		});
	}

	@Override
	public HibTagFamily loadObjectByUuid(InternalActionContext ac, String userUuid, InternalPermission perm) {
		return daoHelper.loadObjectByUuid(ac, userUuid, perm);
	}

	@Override
	public String[] getGraphQlSortingFieldNames(boolean noDependencies) {
		return Stream.of(
				Arrays.stream(super.getGraphQlSortingFieldNames(noDependencies)),
				Arrays.stream(SORT_FIELDS)					
			).flatMap(Function.identity()).toArray(String[]::new);
	}
}
