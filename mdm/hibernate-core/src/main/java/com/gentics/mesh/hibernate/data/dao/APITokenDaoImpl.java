package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.conflict;
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

import jakarta.persistence.TypedQuery;

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
		UserAPITokenDataModel model = new UserAPITokenDataModel();
		model.setUuid(element.getUuid());
		model.setName(element.getName());
		model.setIssued(element.getIssuedDate());
		model.setLastUsed(element.getLastUsedDate());
		model.setExpires(element.getExpiresDate());
		model.setValid(element.isValid());
		return model;
	}

	@Override
	public HibAPITokenData create(HibUser user, String name, String tokenId, Long expires) {
		HibAPITokenData conflicting = findByName(user, name);
		if (conflicting != null) {
			throw conflict(conflicting.getUuid(), name, "apitoken_conflicting_name");
		}

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
		TypedQuery<HibAPITokenData> query = daoHelper.em()
				.createQuery("FROM apitoken t WHERE t.user = :user AND t.tokenId = :tokenId", HibAPITokenData.class);
		query.setParameter("user", user);
		query.setParameter("tokenId", tokenId);
		query.setHint(HibernateHints.HINT_CACHEABLE, true);
		return firstOrNull(query);
	}

	@Override
	public HibAPITokenData findByName(HibUser user, String name) {
		TypedQuery<HibAPITokenData> query = daoHelper.em().createQuery("FROM apitoken t WHERE t.user = :user AND t.name = :name", HibAPITokenData.class);
		query.setParameter("user", user);
		query.setParameter("name", name);
		query.setHint(HibernateHints.HINT_CACHEABLE, true);
		return firstOrNull(query);
	}

	@Override
	public Page<? extends HibAPITokenData> findAll(InternalActionContext ac, HibUser user, PagingParameters pagingInfo) {
		FilterOperation<?> userFilter = Comparison.eq(new FieldOperand<>(ElementType.APITOKEN, "user_dbUuid"), new LiteralOperand<>(user.getUuid(), false), StringUtils.EMPTY);
		return daoHelper.findAll(ac, Optional.empty(), pagingInfo, Optional.of(userFilter));
	}
}
