package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * DAO for {@link HibLanguage}
 * 
 * TODO MDM The method should be moved to {@link LanguageDao}
 */
@Singleton
public class LanguageDaoWrapperImpl extends AbstractDaoWrapper<HibLanguage> implements LanguageDaoWrapper {

	@Inject
	public LanguageDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language create(String languageName, String languageTag) {
		return boot.get().meshRoot().getLanguageRoot().create(languageName, languageTag);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language create(String languageName, String languageTag, String uuid) {
		return boot.get().meshRoot().getLanguageRoot().create(languageName, languageTag, uuid);
	}

	/**
	 * @see LanguageRoot
	 */
	public void addLanguage(Language language) {
		boot.get().meshRoot().getLanguageRoot().addLanguage(language);
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

	/**
	 * @see LanguageRoot
	 */
	public Language findByName(InternalActionContext ac, String name, InternalPermission perm) {
		return boot.get().meshRoot().getLanguageRoot().findByName(ac, name, perm);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		return boot.get().meshRoot().getLanguageRoot().loadObjectByUuid(ac, uuid, perm);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return boot.get().meshRoot().getLanguageRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().meshRoot().getLanguageRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().meshRoot().getLanguageRoot().create(ac, batch);
	}

	/**
	 * @see LanguageRoot
	 */
	public Language create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().meshRoot().getLanguageRoot().create(ac, batch, uuid);
	}

	/**
	 * @see LanguageRoot
	 */
	public void delete(Language element, BulkActionContext bac) {
		boot.get().meshRoot().getLanguageRoot().delete(element, bac);
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
	public boolean update(HibLanguage element, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().meshRoot().getLanguageRoot().update(toGraph(element), ac, batch);
	}

	@Override
	public String getAPIPath(HibLanguage element, InternalActionContext ac) {
		return toGraph(element).getAPIPath(ac);
	}

}
