package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.event.EventCauseAction.DELETE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.action.DAOActions;
import com.gentics.mesh.core.action.LoadAllAction;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.core.db.TxAction0;
import com.gentics.mesh.core.db.TxAction2;
import com.gentics.mesh.core.db.TxEventAction;
import com.gentics.mesh.core.db.TxEventAction0;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.ValidationUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Common request handler methods which can be used for CRUD operations.
 */
@Singleton
public class HandlerUtilities {

	private static final Logger log = LoggerFactory.getLogger(HandlerUtilities.class);

	private final Database database;

	private final Provider<EventQueueBatch> queueProvider;

	private final Provider<BulkActionContext> bulkProvider;

	private final WriteLock writeLock;

	private final PageTransformer pageTransformer;

	private final MeshOptions meshOptions;

	@Inject
	public HandlerUtilities(Database database, MeshOptions meshOptions, Provider<EventQueueBatch> queueProvider,
		Provider<BulkActionContext> bulkProvider, WriteLock writeLock, PageTransformer pageTransformer) {
		this.database = database;
		this.queueProvider = queueProvider;
		this.bulkProvider = bulkProvider;
		this.writeLock = writeLock;
		this.pageTransformer = pageTransformer;
		this.meshOptions = meshOptions;
	}

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param actions
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void createElement(InternalActionContext ac, DAOActions<T, RM> actions) {
		createOrUpdateElement(ac, null, actions);
	}

	/**
	 * Invoke the delete operation for the element.
	 * 
	 * @param <T>
	 *            Element type
	 * @param <RM>
	 *            Response type
	 * @param ac
	 *            Request context
	 * @param actions
	 *            Actions to be used for loading and deleting
	 * @param uuid
	 *            Element uuid
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void deleteElement(InternalActionContext ac, DAOActions<T, RM> actions,
		String uuid) {
		deleteElement(ac, null, actions, uuid);
	}

	/**
	 * Delete the specified element.
	 * 
	 * @param ac
	 * @param parentLoader
	 * @param actions
	 *            Handler which provides the root vertex which will be used to load the element
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void deleteElement(InternalActionContext ac, Function<Tx, Object> parentLoader,
		DAOActions<T, RM> actions,
		String uuid) {
		ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
		try (WriteLock lock = writeLock.lock(ac)) {
			syncTx(ac, tx -> {
				Object parent = null;
				if (parentLoader != null) {
					parent = parentLoader.apply(tx);
				}
				T element = actions.loadByUuid(context(tx, ac, parent), uuid, DELETE_PERM, true);

				// Load the name and uuid of the element. We need this info after deletion.
				String elementUuid = element.getUuid();
				bulkableAction(bac -> {
					bac.setRootCause(element.getTypeInfo().getType(), elementUuid, DELETE);
					actions.delete(tx, element, bac);
				});
				log.info("Deleted element {" + elementUuid + "} for type {" + element.getClass().getSimpleName() + "}");
			}, () -> ac.send(NO_CONTENT));
		}

	}

	/**
	 * Locate and update or create the element using the action context data.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 * @param actions
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void updateElement(InternalActionContext ac, String uuid,
		DAOActions<T, RM> actions) {
		createOrUpdateElement(ac, uuid, actions);
	}

	/**
	 * Handle a create/update of the given element by uuid
	 * 
	 * @param <T>
	 *            Element type
	 * @param <RM>
	 *            Response model
	 * @param ac
	 *            Context for the request
	 * @param uuid
	 *            Element uuid
	 * @param actions
	 *            Actions to be used for loading the element
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void createOrUpdateElement(InternalActionContext ac, String uuid,
		DAOActions<T, RM> actions) {
		createOrUpdateElement(ac, null, uuid, actions);
	}

	/**
	 * Either create or update an element with the given uuid.
	 * 
	 * @param ac
	 * @param parentLoader
	 *            Parent element to be used for the operation
	 * @param uuid
	 *            Uuid of the element to create or update. If null, an element will be created with random Uuid
	 * @param actions
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void createOrUpdateElement(InternalActionContext ac, Function<Tx, Object> parentLoader,
		String uuid, DAOActions<T, RM> actions) {
		ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
		try (WriteLock lock = writeLock.lock(ac)) {
			AtomicBoolean created = new AtomicBoolean(false);
			syncTx(ac, (batch, tx) -> {
				// 1. Load the element from the root element using the given uuid (if not null)
				T element = null;
				if (uuid != null) {
					if (!UUIDUtil.isUUID(uuid)) {
						throw error(BAD_REQUEST, "error_illegal_uuid", uuid);
					}
					Object parent = null;
					if (parentLoader != null) {
						parent = parentLoader.apply(tx);
					}
					element = actions.loadByUuid(context(tx, ac, parent), uuid, InternalPermission.UPDATE_PERM, false);
				}

				// Check whether we need to update a found element or whether we need to create a new one.
				if (element != null) {
					final T updateElement = element;
					actions.update(tx, updateElement, ac, batch);
					RM model = actions.transformToRestSync(tx, updateElement, ac, 0);
					return model;
				} else {
					created.set(true);
					T createdElement =  actions.create(tx, ac, batch, uuid);
					RM model = actions.transformToRestSync(tx, createdElement, ac, 0);
					String path = actions.getAPIPath(tx, ac, createdElement);
					ac.setLocation(path);
					return model;
				}
			}, model -> ac.send(model, created.get() ? CREATED : OK));
		}
	}

	/**
	 * Read the element with the given element by loading it from the specified dao action.
	 * 
	 * @param <T>
	 * @param <RM>
	 * @param ac
	 * @param uuid
	 * @param actions
	 * @param perm
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void readElement(InternalActionContext ac, String uuid,
		DAOActions<T, RM> actions, InternalPermission perm) {
		readElement(ac, null, uuid, actions, perm);
	}

	/**
	 * Read the element with the given element by loading it from the specified dao action.
	 * 
	 * @param ac
	 * @param parentLoader
	 *            Loader for the parent element
	 * @param uuid
	 *            Uuid of the element which should be loaded
	 * @param actions
	 *            Handler which provides the root vertex which should be used when loading the element
	 * @param perm
	 *            Permission to check against when loading the element
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void readElement(InternalActionContext ac, Function<Tx, Object> parentLoader, String uuid,
		DAOActions<T, RM> actions, InternalPermission perm) {
		ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
		syncTx(ac, tx -> {
			Object parent = null;
			if (parentLoader != null) {
				parent = parentLoader.apply(tx);
			}
			T element = actions.loadByUuid(context(tx, ac, parent), uuid, perm, true);

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

	/**
	 * Read the element list and return it to the client.
	 * 
	 * @param <T>
	 *            Persistence entity type
	 * @param <RM>
	 *            Response model type
	 * @param ac
	 * @param actions
	 *            Actions to be used to load the paged data
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void readElementList(InternalActionContext ac, LoadAllAction<T> actions) {
		readElementList(ac, null, actions);
	}

	/**
	 * Read a list of elements of the given root vertex and respond with a list response.
	 * 
	 * @param ac
	 * @param parentLoader
	 * @param actions
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public <T extends HibCoreElement<RM>, RM extends RestModel> void readElementList(InternalActionContext ac, Function<Tx, Object> parentLoader,
		LoadAllAction<T> actions) {
		PagingParameters pagingInfo = ac.getPagingParameters();
		ValidationUtil.validate(pagingInfo);
		ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
		syncTx(ac, tx -> {
			Object parent = null;
			if (parentLoader != null) {
				parent = parentLoader.apply(tx);
			}
			Page<? extends T> page = actions.loadAll(context(tx, ac, parent), pagingInfo);

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

	/**
	 * Invoke sync handler in a tx and return the result via the provided action.
	 * 
	 * @param <RM>
	 * @param ac
	 * @param handler
	 * @param action
	 */
	public <RM> void syncTx(InternalActionContext ac, TxAction<RM> handler, Consumer<RM> action) {
		try {
			ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
			RM model = database.tx(handler);
			action.accept(model);
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Perform a transaction-demarcated action, dispatch any event actions and return the result via the provided
	 * action
	 * @param ac
	 * @param handler
	 * @param action
	 * @param <RM>
	 */
	public <RM> void syncTx(InternalActionContext ac, TxEventAction<RM> handler, Consumer<RM> action) {
		try {
			ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
			RM rm = database.tx((tx) -> {
				EventQueueBatch eventQueueBatch = queueProvider.get();
				tx.<CommonTx>unwrap().data().setEventQueueBatch(eventQueueBatch);
				return handler.handle(eventQueueBatch, tx);
			});
			action.accept(rm);
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Perform a transaction-demarcated action, dispatch any event actions and finally run the provided action.
	 * @param ac
	 * @param handler
	 * @param action
	 */
	public void syncTx(InternalActionContext ac, TxEventAction0 handler, Runnable action) {
		try {
			ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
			database.tx((tx) -> {
				EventQueueBatch eventQueueBatch = queueProvider.get();
				tx.<CommonTx>unwrap().data().setEventQueueBatch(eventQueueBatch);
				handler.handle(eventQueueBatch, tx);
			});
			action.run();
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Invoke sync action in a tx.
	 * 
	 * @param ac
	 * @param handler
	 * @param action
	 */
	public void syncTx(InternalActionContext ac, TxAction0 handler, Runnable action) {
		try {
			ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
			database.tx(handler);
			action.run();
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Invoke sync action in a tx.
	 *
	 * @param ac
	 * @param handler
	 * @param action
	 */
	public void syncTx(InternalActionContext ac, TxAction2 handler, Runnable action) {
		try {
			database.tx(handler);
			action.run();
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Invoke a bulkable action.
	 * 
	 * @param function
	 */
	public void bulkableAction(Consumer<BulkActionContext> function) {
		BulkActionContext bac = bulkProvider.get();
		function.accept(bac);
		bac.process(true);
	}

	/**
	 * Invoke an event action which returns a result.
	 *
	 * @param function
	 * @return
	 */
	public <T> T eventAction(BiFunction<Tx, EventQueueBatch, T> function) {
		T t = database.tx(tx -> {
			EventQueueBatch batch = queueProvider.get();
			tx.<CommonTx>unwrap().data().setEventQueueBatch(batch);
			T result = function.apply(tx, batch);
			return result;
		});
		return t;
	}

	/**
	 * Check whether the user is an admin. An error will be thrown otherwise.
	 * 
	 * @param context
	 */
	public void requiresAdminRole(RoutingContext context) {
		InternalRoutingActionContextImpl ac = new InternalRoutingActionContextImpl(context);
		ac.setHttpServerConfig(meshOptions.getHttpServerOptions());
		if (database.tx(() -> !ac.getUser().isAdmin())) {
			throw error(FORBIDDEN, "error_admin_permission_required");
		} else {
			context.next();
		}
	}
}
