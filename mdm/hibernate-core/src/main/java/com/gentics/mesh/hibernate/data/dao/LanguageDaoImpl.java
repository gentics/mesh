package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.jpa.HibernateHints;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.dao.PersistingLanguageDao;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.data.domain.HibLanguageImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Language DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class LanguageDaoImpl extends AbstractHibDaoGlobal<HibLanguage, LanguageResponse, HibLanguageImpl> implements PersistingLanguageDao {

	public static final String[] SORT_FIELDS = new String[] { "name", "nativeName", "languageTag" };

	@Inject
	public LanguageDaoImpl(DaoHelper<HibLanguage, HibLanguageImpl> daoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public HibLanguage findByLanguageTag(String tag) {
		return firstOrNull(em().createQuery("select l from language l where l.languageTag = :tag", HibLanguageImpl.class)
				.setParameter("tag", tag)
				.setHint(HibernateHints.HINT_CACHEABLE, true));
	}

	@Override
	public void delete(HibLanguage element) {
		throw new NotImplementedException("Languages cannot be deleted");
	}

	@Override
	public boolean update(HibLanguage element, InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Languages cannot be updated");
	}

	@Override
	public HibLanguage findByLanguageTag(HibProject project, String tag) {
		return firstOrNull(em().createQuery("select l from project p join p.languages l where p = :project and l.languageTag = :tag", HibLanguageImpl.class)
				.setParameter("tag", tag)
				.setParameter("project", project)
				.setHint(HibernateHints.HINT_CACHEABLE, true));
	}

	@Override
	public Page<? extends HibLanguage> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return PersistingRootDao.shouldSort(pagingInfo) 
				? daoHelper.findAll(ac, Optional.empty(), pagingInfo, Optional.empty()) 
				: daoHelper.findAll(ac, pagingInfo, null, false);
	}

	@Override
	public String[] getGraphQlSortingFieldNames(boolean noDependencies) {
		return Stream.of(
				Arrays.stream(super.getGraphQlSortingFieldNames(noDependencies))
					.filter(item -> !item.startsWith("created") && !item.startsWith("edited") && !item.startsWith("creator.") && !item.startsWith("editor.")),
				Arrays.stream(SORT_FIELDS)					
			).flatMap(Function.identity()).toArray(String[]::new);
	}
}
