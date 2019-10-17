package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Flowable;

@Singleton
public class EntitiesProvider implements DebugInfoProvider {
	private final BootstrapInitializer boot;
	private final Database db;

	@Inject
	public EntitiesProvider(BootstrapInitializer boot, Database db) {
		this.boot = boot;
		this.db = db;
	}

	@Override
	public String name() {
		return "entities";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.LIGHT;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.mergeArray(
			rootElements(ac, boot::jobRoot, "entities/jobs.json"),
			rootElements(ac, boot::schemaContainerRoot, "entities/schemas.json"),
			rootElements(ac, boot::microschemaContainerRoot, "entities/microschemas.json"),
			rootElements(ac, boot::projectRoot, "entities/projects.json"),
			branches(ac)
		);
	}

	private Flowable<DebugInfoEntry> branches(InternalActionContext ac) {
		return db.singleTx(() -> boot.projectRoot().findAll().stream()
			.map(project -> DebugInfoEntry.fromString(
				String.format("entities/branches/%s.json", project.getName()),
				rootToString(ac, project.getBranchRoot())
			)).collect(Collectors.toList()))
			.flatMapPublisher(Flowable::fromIterable);
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> Flowable<DebugInfoEntry> rootElements(InternalActionContext ac, Supplier<RootVertex<T>> root, String filename) {
		return db.singleTx(() -> rootToString(ac, root.get()))
			.map(elementList -> DebugInfoEntry.fromString(filename, elementList))
			.toFlowable();
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> String rootToString(InternalActionContext ac, RootVertex<T> root) {
		return JsonUtil.toJson(root.findAll().stream()
			.map(branch -> branch.transformToRestSync(ac, 0))
			.collect(Collectors.toList()));
	}
}
