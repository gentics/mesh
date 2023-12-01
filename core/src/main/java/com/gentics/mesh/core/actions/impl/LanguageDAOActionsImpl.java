package com.gentics.mesh.core.actions.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.LanguageDAOActions;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class LanguageDAOActionsImpl implements LanguageDAOActions {

	@Inject
	public LanguageDAOActionsImpl() {
	}

	@Override
	public HibLanguage create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw error(BAD_REQUEST, "error_language_creation_forbidden");
	}

	@Override
	public HibLanguage loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return ctx.tx().languageDao().findByUuid(uuid);
	}

	@Override
	public HibLanguage loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		return ctx.tx().languageDao().findByName(name);
	}

	@Override
	public Page<? extends HibLanguage> loadAll(DAOActionContext ctx, PagingParameters pagingInfo,
			Predicate<HibLanguage> extraFilter) {
		return ctx.tx().languageDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, HibLanguage element, InternalActionContext ac, EventQueueBatch batch) {
		return tx.languageDao().update(element, ac, batch);
	}

	@Override
	public void delete(Tx tx, HibLanguage element, BulkActionContext bac) {
		// Unassign languages should cause a batch process that removes the FieldContainers for the given language.
		tx.languageDao().delete(element, bac);
	}

	@Override
	public LanguageResponse transformToRestSync(Tx tx, HibLanguage element, InternalActionContext ac, int level,
			String... languageTags) {
		return tx.languageDao().transformToRestSync(element, ac, level, languageTags);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibLanguage element) {
		return tx.languageDao().getETag(element, ac);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibLanguage element) {
		return tx.languageDao().getAPIPath(element, ac);
	}

	@Override
	public Page<? extends HibLanguage> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().languageDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibLanguage> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().languageDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public HibLanguage loadByTag(DAOActionContext ctx, String languageTag) {
		return ctx.tx().languageDao().findByLanguageTag(languageTag);
	}
}
