package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.HibernateHints;

import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.graphqlfilter.filter.operation.FieldOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.LiteralOperand;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.APITokenDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.user.HibAPITokenData;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserAPITokenDataModel;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.domain.HibAPITokenDataImpl;
import com.gentics.mesh.parameter.PagingParameters;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Implementation of {@link APITokenDao}
 */
@Singleton
public class APITokenDaoImpl implements APITokenDao {
	protected final CurrentTransaction currentTransaction;

	protected final DaoHelper<HibAPITokenData, HibAPITokenDataImpl> daoHelper;

	@Inject
	public APITokenDaoImpl(CurrentTransaction currentTransaction, DaoHelper<HibAPITokenData, HibAPITokenDataImpl> daoHelper) {
		this.currentTransaction = currentTransaction;
		this.daoHelper = daoHelper;
	}

	@Override
	public UserAPITokenDataModel transformToRestSync(HibAPITokenData element, InternalActionContext ac, int level,
			String... languageTags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HibAPITokenData create(HibUser user, String name, String tokenId, Long expires) {
		return currentTransaction.getTx().create(null, HibAPITokenDataImpl.class, tokenData -> {
			tokenData.setUser(user);
			tokenData.setName(name);
			tokenData.setTokenId(tokenId);
			tokenData.setIssuedTimestamp();
			tokenData.setExpiresTimestamp(expires);
		});
	}

	@Override
	public HibAPITokenData findByUuid(HibUser user, String uuid) {
		return Optional.ofNullable(daoHelper.findByUuid(uuid)).filter(t -> Objects.equals(t.getUser(), user))
				.orElse(null);
	}

	@Override
	public HibAPITokenData findByTokenId(HibUser user, String tokenId) {
		CriteriaQuery<HibAPITokenDataImpl> query = daoHelper.cb().createQuery(daoHelper.getDomainClass());

		Root<HibAPITokenDataImpl> root = query.from(daoHelper.getDomainClass());

		query.where(daoHelper.cb().equal(root.get("tokenId"), tokenId));
		return Optional.ofNullable(firstOrNull(
				daoHelper.em().createQuery(query).setHint(HibernateHints.HINT_CACHEABLE, true).setMaxResults(1)))
				.filter(t -> Objects.equals(t.getUser(), user)).orElse(null);
	}

	@Override
	public Page<? extends HibAPITokenData> findAll(InternalActionContext ac, HibUser user, PagingParameters pagingInfo) {
		FilterOperation<?> userFilter = Comparison.eq(new FieldOperand<>(ElementType.APITOKEN, "user_dbUuid"), new LiteralOperand<>(user.getUuid(), false), StringUtils.EMPTY);
		return daoHelper.findAll(ac, Optional.empty(), pagingInfo, Optional.of(userFilter));
	}
}
