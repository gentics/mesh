package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.apache.commons.collections4.IteratorUtils;
import org.hibernate.Hibernate;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingTagDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeTag;
import com.gentics.mesh.hibernate.data.domain.HibPermissionImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagFamilyImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Tag DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class TagDaoImpl extends AbstractHibDaoGlobal<HibTag, TagResponse, HibTagImpl> implements PersistingTagDao {

	private final RootDaoHelper<HibTag, HibTagImpl, HibTagFamily, HibTagFamilyImpl> rootDaoHelper;

	@Inject
	public TagDaoImpl(RootDaoHelper<HibTag, HibTagImpl, HibTagFamily, HibTagFamilyImpl> rootDaoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(rootDaoHelper.getDaoHelper(), permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.rootDaoHelper = rootDaoHelper;
	}

	@Override
	public Page<? extends HibNode> findTaggedNodes(HibTag tag, HibUser requestUser, HibBranch branch,
			List<String> languageTags, ContainerType type, PagingParameters pagingInfo) {
		String branchUuid =branch.getUuid();

		List<? extends HibNode> nodes = null;
		if (requestUser.isAdmin()) {
			nodes = em().createNamedQuery("node.findByTagsLanguageTagsAndContainerTypeForAdmin", HibNodeImpl.class)
					.setParameter("tagUuid", tag.getId())
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.getResultList();
		} else {
			List<HibRole> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(requestUser).iterator());
			nodes = SplittingUtils.splitAndMergeInList(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createNamedQuery("node.findByTagsLanguageTagsAndContainerType", HibNodeImpl.class)
					.setParameter("tagUuid", tag.getId())
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.setParameter("roles", slice)
					.getResultList());
		}

		return new DynamicTransformableStreamPageImpl<>(nodes.stream(), pagingInfo);
	}

	@Override
	public Result<? extends HibNode> findTaggedNodes(HibTag tag, InternalActionContext ac, InternalPermission perm) {
		HibUser user = ac.getUser();
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac);
		String branchUuid = branch.getUuid();

		Stream<HibNodeImpl> stream = null;
		if (user.isAdmin()) {
			stream = em().createNamedQuery("node.findByTagsLanguageTags.admin", HibNodeImpl.class)
					.setParameter("tagUuid", tag.getId())
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.getResultStream();
		} else if (perm == null || perm == InternalPermission.READ_PUBLISHED_PERM) {
			List<HibRole> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			stream = em().createNamedQuery("node.findByTagsLanguageTags", HibNodeImpl.class)
					.setParameter("tagUuid", tag.getId())
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("roles", roles)
					.getResultStream();
		} else {
			List<HibRole> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			String query = HibNodeImpl.QUERY_FIND_BY_TAGS_LANG_TAGS + "and perm." + HibPermissionImpl.getColumnName(perm) + " = true";
			stream = em().createQuery(query, HibNodeImpl.class)
					.setParameter("tagUuid", tag.getId())
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("roles", roles)
					.getResultStream();
		}
		return new TraversalResult<>(stream);
	}

	@Override
	public void removeNode(HibTag tag, HibNode node) {
		em().createQuery("delete from node_tag t where t.id.nodeUUID = :nodeuuid and t.id.tagUUID = :taguuid")
				.setParameter("nodeuuid", node.getId())
				.setParameter("taguuid", tag.getId())
				.executeUpdate();
	}

	@Override
	public Result<? extends HibNode> getNodes(HibTag tag, HibBranch branch) {
		Stream<HibNodeImpl> nodes = em().createQuery("select t.node from node_tag t where t.id.tagUUID = :taguuid and t.id.branchUUID = :branchuuid", HibNodeImpl.class)
				.setParameter("taguuid", tag.getId())
				.setParameter("branchuuid", branch.getId())
				.getResultStream();

		return new TraversalResult<>(nodes.iterator());
	}

	@Override
	public void removeTag(HibNode node, HibTag tag, HibBranch branch) {
		em().createQuery("delete from node_tag t where t.id.nodeUUID = :nodeuuid and t.id.tagUUID = :taguuid and t.id.branchUUID = :branchuuid")
				.setParameter("nodeuuid", node.getId())
				.setParameter("taguuid", tag.getId())
				.setParameter("branchuuid", branch.getId())
				.executeUpdate();
	}

	@Override
	public void removeAllTags(HibNode node, HibBranch branch) {
		em().createQuery("delete from node_tag t where t.id.nodeUUID = :nodeuuid and t.id.branchUUID = :branchuuid")
				.setParameter("nodeuuid", node.getId())
				.setParameter("branchuuid", branch.getId())
				.executeUpdate();
	}

	@Override
	public Result<HibTag> getTags(HibNode node, HibBranch branch) {
		HibNodeImpl impl = (HibNodeImpl) node;
		if (em().contains(node) && Hibernate.isInitialized(impl.getTags())) {
			// creating a new traversal result to avoid concurrent exceptions
			return new TraversalResult<>(impl.getTags(branch).list());
		}

		Stream<HibTagImpl> tags = em().createQuery("select t.tag from node_tag t where t.id.nodeUUID = :nodeuuid and t.id.branchUUID = :branchuuid order by t.tag.name asc", HibTagImpl.class)
				.setParameter("nodeuuid", node.getId())
				.setParameter("branchuuid", branch.getId())
				.getResultStream();

		return new TraversalResult<>(tags.iterator());
	}

	@Override
	public Page<? extends HibTag> getTags(HibNode node, HibUser user, PagingParameters params, HibBranch branch) {
		CriteriaQuery<HibTagImpl> query = cb().createQuery(HibTagImpl.class);
		Root<HibNodeTag> root = query.from(HibNodeTag.class);
		query.select(root.get("tag"))
				.where(cb().and(
						cb().equal(root.get("id").get("nodeUUID"), node.getId()),
						cb().equal(root.get("id").get("branchUUID"), branch.getId())));
				//.orderBy(cb().asc(root.get("tag").get("name"))); // Breaks the expected results in tests

		daoHelper.addPermissionRestriction(query, root.get("tag"), user, READ_PERM);

		return daoHelper.getResultPage(query, params);
	}

	@Override
	public boolean hasTag(HibNode node, HibTag tag, HibBranch branch) {
		return em().createQuery("select t from node_tag t where t.id.nodeUUID = :nodeuuid and t.id.tagUUID = :taguuid and t.id.branchUUID = :branchuuid")
				.setParameter("nodeuuid", node.getId())
				.setParameter("taguuid", tag.getId())
				.setParameter("branchuuid", branch.getId())
				.getResultStream()
				.findFirst()
				.isPresent();
	}

	@Override
	public Stream<? extends HibTag> findAllStream(HibTagFamily root, InternalActionContext ac,
			InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeNativeFilter) {
		return StreamSupport.stream(rootDaoHelper.findAllInRoot(root, ac, paging, maybeNativeFilter, null, permission).spliterator(), false);
	}

	@Override
	public long countAll(HibTagFamily root, InternalActionContext ac, InternalPermission permission,
			PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeFilter) {
		return rootDaoHelper.countAllInRoot(root, ac, pagingInfo, maybeFilter, permission);
	}

	@Override
	public Page<? extends HibTag> findAll(HibTagFamily root, InternalActionContext ac, PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), null, true);
	}

	@Override
	public Page<? extends HibTag> findAll(HibTagFamily root, InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<HibTag> extraFilter) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), extraFilter, true);
	}

	@Override
	public Page<? extends HibTag> findAllNoPerm(HibTagFamily root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), null, false);
	}

	@Override
	public HibTag findByName(HibTagFamily root, String name) {
		return HibernateTx.get().data().mesh().tagNameCache().get(getCacheKey(root, name), key -> {
			return firstOrNull(
					em().createQuery("select t from tag t where t.name = :name and t.tagFamily = :tagFamily", HibTagImpl.class)
							.setParameter("name", name)
							.setParameter("tagFamily", root));
		});
	}

	@Override
	public HibTag findByName(String name) {
		return HibernateTx.get().data().mesh().tagNameCache().get(name, tagName -> {
			return super.findByName(tagName);
		});
	}

	@Override
	public HibTag findByUuid(HibTagFamily root, String uuid) {
		if (!UUIDUtil.isUUID(uuid)) {
			return null;
		}

		HibTag tag = daoHelper.findByUuid(uuid);
		// double check that the tag actually belongs to the tag family
		if (tag != null && Objects.equals(tag.getTagFamily(), root)) {
			return tag;
		}
		return null;
	}

	@Override
	public void addItem(HibTagFamily root, HibTag item) {
		root.addTag(item);
	}

	@Override
	public void removeItem(HibTagFamily root, HibTag item) {
		root.removeTag(item);
	}

	@Override
	public long globalCount(HibTagFamily root) {
		return daoHelper.count();
	}

	@Override
	public Result<? extends HibTag> findAll(HibTagFamily root) {
		return new TraversalResult<>(((HibTagFamilyImpl) root).getTags());
	}

	@Override
	public HibTag beforeDeletedFromDatabase(HibTag element) {
		removeItem(element.getTagFamily(), element);
		em().createQuery("delete from node_tag t where t.id.tagUUID = :taguuid")
				.setParameter("taguuid", element.getId())
				.executeUpdate();
		// remove tags from branches
		List<HibBranchImpl> branchesWithTag = em().createNamedQuery("branch.findForTag", HibBranchImpl.class)
				.setParameter("tag", element)
				.getResultList();
		for (HibBranchImpl branch : branchesWithTag) {
			branch.removeTag(element);
		}
		return element;
	}
}
