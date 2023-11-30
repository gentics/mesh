package com.gentics.mesh.core.endpoint.project;

import com.gentics.mesh.core.action.DAOActions;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;

/**
 * Language CRUD actions handler
 */
public class LanguageCrudHandler extends AbstractCrudHandler<HibLanguage, LanguageResponse> {

	public LanguageCrudHandler(Database db, HandlerUtilities utils, WriteLock writeLock, DAOActions<HibLanguage, LanguageResponse> actions) {
		super(db, utils, writeLock, actions);
	}

}
