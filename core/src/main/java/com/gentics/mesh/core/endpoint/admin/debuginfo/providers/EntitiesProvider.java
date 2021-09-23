package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.DaoGlobal;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Flowable;

/**
 * Debug info provider for entity information (jobs, schemas, microschemas, projects, branches)
 */
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
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.mergeArray(
			rootElements(ac, boot::jobDao, "entities/jobs.json"),
			rootElements(ac, boot::schemaDao, "entities/schemas.json"),
			rootElements(ac, boot::microschemaDao, "entities/microschemas.json"),
			rootElements(ac, boot::projectDao, "entities/projects.json"),
			branches(ac)
		);
	}

	private Flowable<DebugInfoEntry> branches(InternalActionContext ac) {
		return db.singleTx(tx -> tx.projectDao().findAll().stream()
			.map(project -> DebugInfoBufferEntry.fromString(
				String.format("entities/branches/%s.json", project.getName()),
				rootToString(ac, tx.branchDao().findAll(project).stream(), tx.branchDao())
			)).collect(Collectors.toList()))
			.flatMapPublisher(Flowable::fromIterable);
	}

	private <
			T extends HibCoreElement<? extends RestModel>, 
			D extends DaoGlobal<T> & DaoTransformable<T, ? extends RestModel>
		> Flowable<DebugInfoEntry> rootElements(
				InternalActionContext ac, Supplier<D> root, String filename
		) {
		return db.singleTx(() -> rootToString(ac, root.get().findAll().stream(), root.get()))
			.map(elementList -> DebugInfoBufferEntry.fromString(filename, elementList))
			.toFlowable();
	}
	
	private <T extends HibCoreElement<? extends RestModel>> String rootToString(InternalActionContext ac, Stream<? extends T> stream, DaoTransformable<T, ? extends RestModel> dao) {
		return JsonUtil.toJson(
			stream.map(element -> dao.transformToRestSync(element, ac, 0))
				.collect(Collectors.toList()));
	}
}
