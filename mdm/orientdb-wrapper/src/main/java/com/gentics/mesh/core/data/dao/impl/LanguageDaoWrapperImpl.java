package com.gentics.mesh.core.data.dao.impl;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * DAO for {@link HibLanguage}
 * 
 * TODO MDM The method should be moved to {@link LanguageDao}
 */
@Singleton
public class LanguageDaoWrapperImpl extends AbstractCoreDaoWrapper<LanguageResponse, HibLanguage, Language> implements LanguageDaoWrapper {

	@Inject
	public LanguageDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language findByLanguageTag(String languageTag) {
		return boot.get().meshRoot().getLanguageRoot().findByLanguageTag(languageTag);
	}

	/**
	 * @see LanguageRoot
	 */
	public Result<? extends Language> findAll() {
		return boot.get().meshRoot().getLanguageRoot().findAll();
	}

	/**
	 * @see LanguageRoot
	 */
	public Stream<? extends Language> findAllStream(InternalActionContext ac, InternalPermission permission) {
		return boot.get().meshRoot().getLanguageRoot().findAllStream(ac, permission);
	}

	/**
	 * @see LanguageRoot
	 */
	public Page<? extends HibLanguage> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getLanguageRoot().findAll(ac, pagingInfo);
	}

	/**
	 * @see LanguageRoot
	 */
	public Page<? extends HibLanguage> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibLanguage> extraFilter) {
		return boot.get().meshRoot().getLanguageRoot().findAll(ac, pagingInfo, e -> extraFilter.test(e));
	}

	/**
	 * @see LanguageRoot
	 */
	public Page<? extends HibLanguage> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getLanguageRoot().findAllNoPerm(ac, pagingInfo);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language findByName(String name) {
		return boot.get().meshRoot().getLanguageRoot().findByName(name);
	}

	@Override
	public HibLanguage findByUuid(String uuid) {
		return boot.get().meshRoot().getLanguageRoot().findByUuid(uuid);
	}

	@Override
	public long count() {
		return boot.get().meshRoot().getLanguageRoot().globalCount();
	}

	@Override
	public void delete(HibLanguage element, BulkActionContext bac) {
		boot.get().meshRoot().getLanguageRoot().delete(bac);
	}

	@Override
	protected RootVertex<Language> getRoot() {
		return boot.get().meshRoot().getLanguageRoot();
	}
}
