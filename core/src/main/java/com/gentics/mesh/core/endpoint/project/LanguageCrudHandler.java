package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.LanguageDAOActions;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;

/**
 * Language CRUD actions handler
 */
public class LanguageCrudHandler extends AbstractCrudHandler<HibLanguage, LanguageResponse> {

	@Inject
	public LanguageCrudHandler(Database db, HandlerUtilities utils, WriteLock writeLock, LanguageDAOActions actions) {
		super(db, utils, writeLock, actions);
	}

	/**
	 * Locate language object by its tag: en, de, fr etc...
	 * 
	 * @param ac
	 * @param tag
	 */
	public void handleReadByTag(InternalActionContext ac, String tag) {
		utils.syncTx(ac, tx -> {
			HibLanguage element = ((LanguageDAOActions) actions).loadByTag(context(tx, ac, null), tag);

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
}
