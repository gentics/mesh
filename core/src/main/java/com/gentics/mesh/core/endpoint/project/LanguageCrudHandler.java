package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.LanguageDAOActions;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.dao.PersistingLanguageDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.ValidationUtil;

/**
 * Language CRUD actions handler
 */
public class LanguageCrudHandler extends AbstractCrudHandler<HibLanguage, LanguageResponse> {

	private final PageTransformer pageTransformer;

	@Inject
	public LanguageCrudHandler(Database db, HandlerUtilities utils, WriteLock writeLock, LanguageDAOActions actions, PageTransformer pageTransformer) {
		super(db, utils, writeLock, actions);
		this.pageTransformer = pageTransformer;
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		PagingParameters pagingInfo = ac.getPagingParameters();
		ValidationUtil.validate(pagingInfo);

		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);

			Page<? extends HibLanguage> page;
			if (project != null) {
				page = new DynamicTransformableStreamPageImpl<>(tx.<CommonTx>unwrap().projectDao().findLanguages(project).stream(), pagingInfo);
			} else {
				page = ((LanguageDAOActions) actions).loadAll(context(tx, ac, null), pagingInfo);
			}

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = pageTransformer.getETag(page, ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return pageTransformer.transformToRestSync(page, ac, 0);
		}, m -> ac.send(m, OK));
	}

	@Override
	public void handleRead(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "languageUuid");

		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);

			HibLanguage element;
			if (project != null) {
				element = tx.<CommonTx>unwrap().projectDao().findLanguages(project).stream().filter(l -> l.getUuid().equals(uuid)).findAny().orElse(null);
			} else {
				element = ((LanguageDAOActions) actions).loadByUuid(context(tx, ac, null), uuid, InternalPermission.READ_PERM, false);
			}
			if (element == null) {
				throw error(NOT_FOUND, "error_language_not_found", uuid);
			}

			// Handle etag
			if (element != null && ac.getGenericParameters().getETag()) {
				String etag = actions.getETag(tx, ac, element);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return actions.transformToRestSync(tx, element, ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Locate language object by its tag: en, de, fr etc...
	 * 
	 * @param ac
	 * @param tag
	 */
	public void handleReadByTag(InternalActionContext ac, String tag) {
		validateParameter(tag, "languageTag");

		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);

			HibLanguage element;
			if (project != null) {
				element = tx.<CommonTx>unwrap().projectDao().findLanguages(project).stream().filter(l -> l.getLanguageTag().equals(tag)).findAny().orElse(null);
			} else {
				element = ((LanguageDAOActions) actions).loadByTag(context(tx, ac, null), tag);
			}
			if (element == null) {
				throw error(NOT_FOUND, "error_language_not_found", tag);
			}

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = actions.getETag(tx, ac, element);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return actions.transformToRestSync(tx, element, ac, 0);
		}, model -> ac.send(model, OK));
	}

	public void handleAssignLanguageToProject(InternalActionContext ac, String languageUuidOrTag, String paramName) {
		validateParameter(languageUuidOrTag, paramName);

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, (batch, tx) -> {
				PersistingLanguageDao languageDao = tx.<CommonTx>unwrap().languageDao();

				HibProject project = tx.getProject(ac);
				String projectUuid = project.getUuid();
				if (!tx.userDao().hasPermission(ac.getUser(), project, InternalPermission.UPDATE_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
				}
				HibLanguage language;
				if (UUIDUtil.isUUID(languageUuidOrTag)) {
					language = ((LanguageDAOActions) actions).loadByUuid(context(tx, ac, null), languageUuidOrTag, READ_PERM, true);
				} else {
					language = ((LanguageDAOActions) actions).loadByTag(context(tx, ac, null), languageUuidOrTag);
				}
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageUuidOrTag);
				}
				languageDao.assign(language, project, batch, false);

				return tx.projectDao().transformToRestSync(project, ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	public void handleUnassignLanguageFromProject(InternalActionContext ac, String languageUuidOrTag, String paramName) {
		validateParameter(languageUuidOrTag, paramName);

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, (batch, tx) -> {
				PersistingLanguageDao languageDao = tx.<CommonTx>unwrap().languageDao();

				HibProject project = tx.getProject(ac);
				String projectUuid = project.getUuid();
				if (!tx.userDao().hasPermission(ac.getUser(), project, InternalPermission.UPDATE_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
				}
				HibLanguage language;
				if (UUIDUtil.isUUID(languageUuidOrTag)) {
					language = ((LanguageDAOActions) actions).loadByUuid(context(tx, ac, null), languageUuidOrTag, READ_PERM, true);
				} else {
					language = ((LanguageDAOActions) actions).loadByTag(context(tx, ac, null), languageUuidOrTag);
				}
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageUuidOrTag);
				}
				languageDao.unassign(language, project, batch, false);

				return tx.projectDao().transformToRestSync(project, ac, 0);
			}, model -> ac.send(model, OK));
		}
	}
}
