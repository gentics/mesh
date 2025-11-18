package com.gentics.mesh.hibernate.data.dao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.hibernate.data.dao.helpers.RootJoin;
import com.gentics.mesh.hibernate.util.JpaUtil;
import com.gentics.mesh.hibernate.util.Specification;
import com.gentics.mesh.parameter.PagingParameters;
import com.google.auto.factory.AutoFactory;

/**
 * Contains helper methods to search for entity which are bound to a root
 * @param <CHILD>
 * @param <CHILDIMPL>
 * @param <ROOT>
 * @param <ROOTIMPL>
 */
@AutoFactory
public class RootDaoHelper<CHILD extends HibBaseElement, CHILDIMPL extends CHILD,
		                   ROOT extends HibBaseElement, ROOTIMPL extends ROOT> {

	private final DaoHelper<CHILD, CHILDIMPL> daoHelper;
	private final RootJoin<CHILDIMPL, ROOTIMPL> rootJoin;

	/**
	 * Build a RootDaoHelper. Consumers do not need to create a new instance manually, since it can be injected through
	 * dagger dependency injection mechanism.
	 *
	 * @param daoHelper daoHelper
	 * @param rootClass the persistence class of the root entity
	 * @param rootJoin join definition
	 * @param rootFieldName the name of the field in the child entity referencing the root
	 */
	public RootDaoHelper(DaoHelper<CHILD, CHILDIMPL> daoHelper, RootJoin<CHILDIMPL, ROOTIMPL> rootJoin) {
		this.daoHelper = daoHelper;
		this.rootJoin = rootJoin;
	}

	/**
	 * Return the daoHelper instance
	 * @return
	 */
	public DaoHelper<CHILD, CHILDIMPL> getDaoHelper() {
		return daoHelper;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Page<? extends CHILD> findAllInRoot(
			ROOT root, InternalActionContext ac,
			PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeNativeFilter, java.util.function.Predicate<CHILD> extraFilter,
			List<InternalPermission> permissions) {
		if (maybeNativeFilter.isPresent() || PersistingRootDao.shouldSort(pagingInfo)) {
			return daoHelper.findAll(ac, Optional.ofNullable(permissions).flatMap(perms -> perms.stream().findAny()), pagingInfo, maybeNativeFilter, Optional.of(Pair.of(rootJoin, (ROOTIMPL) root)));
		} else {
			return (Page) daoHelper.getResultPage(ac, makeFindAllInRoot(root, ac, permissions), pagingInfo, daoHelper.getEntityGraph("rest", true), extraFilter);
		}
	}

	/**
	 * Count items natively.
	 * 
	 * @param root
	 * @param ac
	 * @param pagingInfo
	 * @param maybeFilter
	 * @param checkPermissions
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public long countAllInRoot(ROOT root, InternalActionContext ac, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeFilter, InternalPermission permission) {
		return daoHelper.countAll(ac, Optional.ofNullable(permission), pagingInfo, maybeFilter, Optional.of(Pair.of(rootJoin, (ROOTIMPL) root)));
	}

	private CriteriaQuery<? extends CHILD> makeFindAllInRoot(
			ROOT root, InternalActionContext ac,
			List<InternalPermission> permissions) {
		return makeQuery(distinct()
					.and(havingRootEqual(root))
					.and(addingPermissionRestriction(ac, permissions)));
	}

	/**
	 * Return a filtered page of children entities having root equal to provided root, checking for permission depending on the checkPermission flag.
	 * @param root the root of the child
	 * @param ac action context
	 * @param pagingInfo paging information
	 * @param extraFilter extra filter to apply to the page. Might be null
	 * @param checkPermissions whether we should check for permission
	 * @return
	 */
	public Page<? extends CHILD> findAllInRoot(
			ROOT root, InternalActionContext ac,
			PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeNativeFilter, java.util.function.Predicate<CHILD> extraFilter,
			boolean checkPermissions) {
		List<InternalPermission> permissionToCheck = checkPermissions
				? Collections.singletonList(InternalPermission.READ_PERM)
				: Collections.emptyList();

		return findAllInRoot(root, ac, pagingInfo, maybeNativeFilter, extraFilter, permissionToCheck);
	}

	/**
	 * Return a filtered page of children entities having root equal to provided root, checking for the provided permission.
	 * @param root the root of the child
	 * @param ac action context
	 * @param pagingInfo paging information
	 * @param extraFilter extra filter to apply to the page. Might be null
	 * @param permission the permission to check
	 * @return
	 */
	public Page<? extends CHILD> findAllInRoot(
			ROOT root, InternalActionContext ac,
			PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeNativeFilter, java.util.function.Predicate<CHILD> extraFilter,
			InternalPermission permission) {
		return findAllInRoot(root, ac, pagingInfo, maybeNativeFilter, extraFilter, Collections.singletonList(permission));
	}

	/**
	 * Return a stream of children entities having root equal to provided root, checking for the provided permission.
	 * @param root the root of the child
	 * @param ac action context
	 * @param permission the permission to check
	 * @return
	 */
	public Stream<? extends CHILD> findAllInRoot(
			ROOT root, InternalActionContext ac,
			InternalPermission permission) {
		CriteriaQuery<? extends CHILD> query = makeFindAllInRoot(root, ac, Collections.singletonList(permission));
		return daoHelper.getResultStream(ac, query, daoHelper.getEntityGraph("rest", true));
	}

	/**
	 * Return a stream of children entities having root equal to provided root, having elementFieldName equal to the elementFieldValue.
	 * @param root the root of the child
	 * @param elementFieldName the elementFieldName to check
	 * @param elementFieldValue the value which the elementFieldName should be equal to
	 * @return
	 */
	public Stream<CHILDIMPL> findByElementInRoot(
			ROOT root,
			String elementFieldName, Object elementFieldValue) {
		return findByElementInRoot(root, null, elementFieldName, elementFieldValue, null);
	}

	/**
	 * Return a stream of children entities having root equal to provided root, having elementFieldName equal to the elementFieldValue,
	 * checking the provided permission.
	 * @param root the root of the child
	 * @param elementFieldName the elementFieldName to check
	 * @param elementFieldValue the value which the elementFieldName should be equal to
	 * @param permission the permission to check
	 * @return
	 */
	public Stream<CHILDIMPL> findByElementInRoot(
			ROOT root,
			InternalActionContext ac,
			String elementFieldName, Object elementFieldValue,
			InternalPermission permission) {
		CriteriaQuery<CHILDIMPL> query = makeQuery(
						havingElementFieldEqual(elementFieldName, elementFieldValue)
						.and(havingRootEqual(root))
						.and(addingPermissionRestriction(ac, permission)));

		return em().createQuery(query).getResultStream();
	}

	/**
	 * Return the count of children having the provided root
	 * @param root
	 * @return
	 */
	public long countInRoot(ROOT root) {
		CriteriaQuery<CHILDIMPL> query = makeQuery(distinct().and(havingRootEqual(root)));
		return JpaUtil.count(em(), query);
	}

	private Join<CHILDIMPL, ROOTIMPL> getJoin(Root<CHILDIMPL> dRoot, EntityType<CHILDIMPL> rootMetamodel) {
		return rootJoin.getJoin(dRoot, rootMetamodel);
	}

	private CriteriaQuery<CHILDIMPL> makeQuery(Specification<CHILDIMPL, ROOTIMPL> specification) {
		CriteriaQuery<CHILDIMPL> query = cb().createQuery(rootJoin.getDomainClass());
		Metamodel metamodel = em().getMetamodel();
		EntityType<CHILDIMPL> rootMetamodel = metamodel.entity(rootJoin.getDomainClass());
		Root<CHILDIMPL> root = query.from(rootJoin.getDomainClass());
		Join<CHILDIMPL, ROOTIMPL> resultJoin = getJoin(root, rootMetamodel);
		query.select(resultJoin.getParent());

		Predicate predicate = specification.toPredicate(root, query, resultJoin, cb());
		daoHelper.addRestrictions(query, predicate);

		return query;
	}

	private Specification<CHILDIMPL, ROOTIMPL> distinct() {
		return (root, query, join, cb) -> {
			query.distinct(true);
			return null;
		};
	}

	private Specification<CHILDIMPL, ROOTIMPL> havingRootEqual(ROOT rootEntity) {
		return (root, query, join, cb) -> cb.equal(join.get("dbUuid"), rootEntity.getId());
	}

	private Specification<CHILDIMPL, ROOTIMPL> addingPermissionRestriction(InternalActionContext ac, InternalPermission perm) {
		return (root, query, join, cb) -> {
			if (perm != null && ac != null) {
				daoHelper.addPermissionRestriction(query, root, ac.getUser(), perm);
				daoHelper.addPermissionRestriction(query, join, ac.getUser(), perm);
			}
			return null;
		};
	}

	private Specification<CHILDIMPL, ROOTIMPL> addingPermissionRestriction(InternalActionContext ac, List<InternalPermission> permissions) {
		return (root, query, join, cb) -> {
			if (!permissions.isEmpty()) {
				daoHelper.addPermissionRestriction(query, root, ac.getUser(), permissions);
				daoHelper.addPermissionRestriction(query, join, ac.getUser(), permissions);
			}
			return null;
		};
	}


	private Specification<CHILDIMPL, ROOTIMPL> havingElementFieldEqual(String elementFieldName,  Object elementFieldValue) {
		return (root, query, join, cb) -> cb().equal(root.get(elementFieldName), elementFieldValue);
	}

	private EntityManager em() {
		return daoHelper.em();
	}

	private CriteriaBuilder cb() {
		return daoHelper.cb();
	}
}
