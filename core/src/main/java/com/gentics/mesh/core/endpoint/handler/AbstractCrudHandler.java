package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Abstract class for CRUD REST handlers. The abstract class provides handler methods for create, read (one), read (multiple) and delete.
 */
public abstract class AbstractCrudHandler<T extends MeshCoreVertex<RM, T>, RM extends RestModel> extends AbstractHandler {

	public static final String TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY = "rootElement";

	protected final Database db;
	protected final HandlerUtilities utils;
	protected final WriteLock globalLock;

	public AbstractCrudHandler(Database db, HandlerUtilities utils, WriteLock globalLock) {
		this.db = db;
		this.utils = utils;
		this.globalLock = globalLock;
	}

	/**
	 * Return the main root vertex that is used to handle CRUD for the elements that are used in combination with this handler.
	 * 
	 * @param ac
	 * @return
	 */
	abstract public RootVertex<T> getRootVertex(InternalActionContext ac);

	/**
	 * Handle create requests.
	 * 
	 * @param ac
	 */
	public void handleCreate(InternalActionContext ac) {
		utils.createElement(ac, () -> getRootVertex(ac));
	}

	/**
	 * Handle delete requests.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 */
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.deleteElement(ac, () -> getRootVertex(ac), uuid);
	}

	/**
	 * Handle read requests that target a single object.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be read
	 */
	public void handleRead(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.readElement(ac, uuid, () -> getRootVertex(ac), READ_PERM);
	}

	/**
	 * Handle update requests.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 */
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.updateElement(ac, uuid, () -> getRootVertex(ac));
	}

	/**
	 * Handle read list requests.
	 * 
	 * @param ac
	 */
	public void handleReadList(InternalActionContext ac) {
		utils.readElementList(ac, () -> getRootVertex(ac));
	}

	/**
	 * Create a route handler which will load the element for the given uuid. The handler will only try to load the root element if a uuid was specified.
	 * Otherwise {@link RoutingContext#next()} will be invoked directly.
	 * 
	 * @param i18nNotFoundMessage
	 *            I18n error message that will be returned when no element could be found
	 */
	public Handler<RoutingContext> getUuidHandler(String i18nNotFoundMessage) {
		Handler<RoutingContext> handler = rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("param0");
			// Only try to load the root element when a uuid string was specified
			if (!isEmpty(uuid)) {
				boolean result = db.tx(() -> {
					T foundElement = getRootVertex(ac).findByUuid(uuid);
					if (foundElement == null) {
						throw error(NOT_FOUND, i18nNotFoundMessage, uuid);
					} else {
						ac.data().put(TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY, foundElement);
					}
					return true;
				});
				if (!result) {
					return;
				}
			}
			rc.next();
		};
		return handler;
	}

}
