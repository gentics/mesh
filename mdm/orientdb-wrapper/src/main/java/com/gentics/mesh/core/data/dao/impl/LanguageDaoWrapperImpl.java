package com.gentics.mesh.core.data.dao.impl;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

@Singleton
public class LanguageDaoWrapperImpl extends AbstractDaoWrapper<HibLanguage> implements LanguageDaoWrapper {

	@Inject
	public LanguageDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	public Language create(String languageName, String languageTag) {
		return boot.get().languageRoot().create(languageName, languageTag);
	}

	public Language create(String languageName, String languageTag, String uuid) {
		return boot.get().languageRoot().create(languageName, languageTag, uuid);
	}

	public void addLanguage(Language language) {
		boot.get().languageRoot().addLanguage(language);
	}

	public Language findByLanguageTag(String languageTag) {
		return boot.get().languageRoot().findByLanguageTag(languageTag);
	}

	public Result<? extends Language> findAll() {
		return boot.get().languageRoot().findAll();
	}

	public Stream<? extends Language> findAllStream(InternalActionContext ac, InternalPermission permission) {
		return boot.get().languageRoot().findAllStream(ac, permission);
	}

	public Page<? extends Language> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().languageRoot().findAll(ac, pagingInfo);
	}

	public Page<? extends Language> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Language> extraFilter) {
		return boot.get().languageRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public Page<? extends Language> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().languageRoot().findAllNoPerm(ac, pagingInfo);
	}

	public Language findByName(String name) {
		return boot.get().languageRoot().findByName(name);
	}

	public Language findByName(InternalActionContext ac, String name, InternalPermission perm) {
		return boot.get().languageRoot().findByName(ac, name, perm);
	}

	public Language findByUuid(String uuid) {
		return boot.get().languageRoot().findByUuid(uuid);
	}

	public Language loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		return boot.get().languageRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Language loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return boot.get().languageRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Language loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().languageRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public Language create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().languageRoot().create(ac, batch);
	}

	public Language create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().languageRoot().create(ac, batch, uuid);
	}

	public void delete(Language element, BulkActionContext bac) {
		boot.get().languageRoot().delete(element, bac);
	}

	@Override
	public HibLanguage findByUuidGlobal(String uuid) {
		return boot.get().languageRoot().findByUuid(uuid);
	}

	@Override
	public long computeGlobalCount() {
		return boot.get().languageRoot().computeCount();
	}

}
