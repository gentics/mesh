package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.DeletedMeshEventModel;
import com.gentics.mesh.core.rest.event.UpdatedMeshEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.core.rest.event.CreatedMeshEventModel;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;
import com.gentics.mesh.search.verticle.request.CreateDocumentRequest;
import com.gentics.mesh.search.verticle.request.DeleteDocumentRequest;
import com.gentics.mesh.search.verticle.request.ElasticSearchRequest;
import com.gentics.mesh.search.verticle.request.UpdateDocumentRequest;
import com.gentics.mesh.util.Tuple;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

/**
 * Maps events from mesh to elastic search requests.
 * Call {@link #getHandledEvents()} to get a collection of all supported events
 * Call {@link #handle(MessageEvent)} to map an event to a list of elastic search requests.
 */
@Singleton
public class Eventhandler {
	private static final Logger log = LoggerFactory.getLogger(Eventhandler.class);

	private final BootstrapInitializer boot;
	private final Database db;
	private final UserTransformer userTransformer;
	private final GroupTransformer groupTransformer;
	private final RoleTransformer roleTransformer;
	private final MeshOptions options;

	private final Map<MeshEvent, Function<MeshEventModel, List<ElasticSearchRequest>>> handlers = new HashMap<>();
	// TODO Replace tuple with more semantic class
	private final List<VertexTransformer> transformers = new ArrayList<>();

	@Inject
	public Eventhandler(BootstrapInitializer boot, Database db, UserTransformer userTransformer, GroupTransformer groupTransformer, RoleTransformer roleTransformer, MeshOptions options) {
		this.boot = boot;
		this.db = db;
		this.userTransformer = userTransformer;
		this.groupTransformer = groupTransformer;
		this.roleTransformer = roleTransformer;
		this.options = options;

		fillTransformers();
		addHandlers();
	}

	private void fillTransformers() {
		transformers.add(new VertexTransformer(User.class, vertex -> new CreateDocumentRequest(prefixIndexName(User.composeIndexName()), vertex.getUuid(), userTransformer.toDocument((User) vertex))));
		transformers.add(new VertexTransformer(Group.class, vertex -> new CreateDocumentRequest(prefixIndexName(Group.composeIndexName()), vertex.getUuid(), groupTransformer.toDocument((Group) vertex))));
		transformers.add(new VertexTransformer(Role.class, vertex -> new CreateDocumentRequest(prefixIndexName(Role.composeIndexName()), vertex.getUuid(), roleTransformer.toDocument((Role) vertex))));
	}

	private void addHandlers() {
		addCreateHandler(USER_CREATED, userTransformer, boot.userRoot(), User.composeIndexName());
		addUpdateHandler(USER_UPDATED, userTransformer, boot.userRoot(), User.composeIndexName());
		addDeleteHandler(USER_DELETED, User.composeIndexName());

		addCreateHandler(GROUP_CREATED, groupTransformer, boot.groupRoot(), Group.composeIndexName());
		addHandler(GROUP_UPDATED, UpdatedMeshEventModel.class, event -> db.tx(tx -> {
			Group group = boot.groupRoot().findByUuid(event.getUuid());
			return Stream.concat(
				Stream.of(createRequest(group)),
				group.getUsers().stream().map(this::createRequest)
			).collect(Collectors.toList());
		}));
		addDeleteHandler(GROUP_DELETED, Group.composeIndexName());

		addCreateHandler(ROLE_CREATED, roleTransformer, boot.roleRoot(), Role.composeIndexName());
		addUpdateHandler(ROLE_UPDATED, roleTransformer, boot.roleRoot(), Role.composeIndexName());
		addDeleteHandler(ROLE_DELETED, Role.composeIndexName());

	}

	private <T> void addHandler(MeshEvent event, Class<T> clazz, Function<T, List<ElasticSearchRequest>> generator) {
		handlers.put(event, msg -> {
			if (msg.getClass().isAssignableFrom(clazz)) {
				return generator.apply((T) msg);
			} else {
				throw new InvalidParameterException("Could not apply event message to handler");
			}
		});
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> void addCreateHandler(MeshEvent event, Transformer<T> transformer, RootVertex<T> root, String indexName) {
		addHandler(event, CreatedMeshEventModel.class, eventModel ->
			getDocument(transformer, root, eventModel.getUuid())
				.map(document -> Collections.singletonList((ElasticSearchRequest) new CreateDocumentRequest(prefixIndexName(indexName), eventModel.getUuid(), document)))
				.orElse(Collections.emptyList())
		);
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> void addUpdateHandler(MeshEvent event, Transformer<T> transformer, RootVertex<T> root, String indexName) {
		addHandler(event, UpdatedMeshEventModel.class, eventModel ->
			getDocument(transformer, root, eventModel.getUuid())
				// TODO Maybe use UpdateDocumentRequest? Create is more resilient because it basically is an upsert
				.map(document -> Collections.singletonList((ElasticSearchRequest) new CreateDocumentRequest(prefixIndexName(indexName), eventModel.getUuid(), document)))
				.orElse(Collections.emptyList())
		);
	}

	private void addDeleteHandler(MeshEvent event, String indexName) {
		addHandler(event, DeletedMeshEventModel.class, eventModel ->
			Collections.singletonList(new DeleteDocumentRequest(prefixIndexName(indexName), eventModel.getUuid()))
		);
	}

	private CreateDocumentRequest createRequest(MeshCoreVertex vertex) {
		// TODO Use map
		for (VertexTransformer transformer : transformers) {
			if (transformer.v1().isAssignableFrom(vertex.getClass())) {
				return transformer.v2().apply(vertex);
			}
		}
		throw new RuntimeException(String.format("Could not find transformer for class {%s}", vertex.getClass().getSimpleName()));
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> Optional<JsonObject> getDocument(Transformer<T> transformer, RootVertex<T> root, String uuid) {
		return db.tx(tx -> {
			T element = root.findByUuid(uuid);

			if (element != null) {
				log.trace(String.format("Transforming document with uuid {%s} and transformer {%s}", uuid, transformer.getClass().getSimpleName()));
				return Optional.of(transformer.toDocument(element));
			} else {
				log.warn(String.format("Could not find element with uuid {%s} in class {%s}", uuid, root.getClass().getSimpleName()));
				return Optional.empty();
			}
		});
	}

	private String prefixIndexName(String index) {
		String prefix = options.getSearchOptions().getPrefix();
		return prefix == null
			? index
			: prefix + index;
	}

	/**
	 * Handles an event from mesh. Creates elastic search document from the data of the graph and creates a list
	 * of requests that represent the data that was changed during the event.
	 *
	 * @param messageEvent
	 * @return
	 * @throws Exception
	 */
	public List<ElasticSearchRequest> handle(MessageEvent messageEvent) throws Exception {
		return handlers.get(messageEvent.event).apply(messageEvent.message);
	}

	/**
	 * Gets collection of all events that can be handled by this class.
	 * @return
	 */
	public Collection<MeshEvent> getHandledEvents() {
		return handlers.keySet();
	}

	private static class VertexTransformer extends Tuple<Class, java.util.function.Function<MeshCoreVertex, CreateDocumentRequest>> {
		public VertexTransformer(Class aClass, java.util.function.Function<MeshCoreVertex, CreateDocumentRequest> meshCoreVertexCreateDocumentRequestFunction) {
			super(aClass, meshCoreVertexCreateDocumentRequestFunction);
		}
	}
}
