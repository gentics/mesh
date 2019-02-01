package com.gentics.mesh.search.verticle;

import com.gentics.mesh.MeshEvent;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.CreatedMeshEventModel;
import com.gentics.mesh.event.MeshEventModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gentics.mesh.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.MeshEvent.USER_CREATED;

public class Eventhandler {
	private final BootstrapInitializer boot;
	private final Database db;
	private final UserTransformer userTransformer;
	private final GroupTransformer groupTransformer;
	private final RoleTransformer roleTransformer;
	private final MeshOptions options;

	private final Map<MeshEvent, Function<MeshEventModel, List<ElasticSearchRequest>>> handlers = new HashMap<>();

	@Inject
	public Eventhandler(BootstrapInitializer boot, Database db, UserTransformer userTransformer, GroupTransformer groupTransformer, RoleTransformer roleTransformer, MeshOptions options) {
		this.boot = boot;
		this.db = db;
		this.userTransformer = userTransformer;
		this.groupTransformer = groupTransformer;
		this.roleTransformer = roleTransformer;
		this.options = options;

		addHandlers();
	}

	private void addHandlers() {
		addCreateHandler(USER_CREATED, userTransformer, boot.userRoot(), User.composeIndexName());
		addCreateHandler(GROUP_CREATED, groupTransformer, boot.groupRoot(), Group.composeIndexName());
		addCreateHandler(ROLE_CREATED, roleTransformer, boot.roleRoot(), Role.composeIndexName());

//		addCreateHandler(GROUP_CREATED, groupTransformer, boot.groupRoot(), Group.composeIndexName());
//		addCreateHandler(GROUP_CREATED, groupTransformer, boot.groupRoot(), Group.composeIndexName());
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
		addHandler(event, CreatedMeshEventModel.class, createGenerator(transformer, root, indexName));
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> Function<CreatedMeshEventModel, List<ElasticSearchRequest>> createGenerator(Transformer<T> transformer, RootVertex<T> root, String indexName) {
		return event -> {
			JsonObject document = transformer.toDocument(root.findByUuid(event.getUuid()));
			return Collections.singletonList(new CreateDocumentRequest(prefixIndexName(indexName), event.getUuid(), document));
		};
	}

	private String prefixIndexName(String index) {
		String prefix = options.getSearchOptions().getPrefix();
		return prefix == null
			? index
			: prefix + index;
	}

	public List<ElasticSearchRequest> handle(MessageEvent messageEvent) throws Exception {
		return handlers.get(messageEvent.event).apply(messageEvent.message);
	}

	public Collection<MeshEvent> getHandledEvents() {
		return handlers.keySet();
	}
}
