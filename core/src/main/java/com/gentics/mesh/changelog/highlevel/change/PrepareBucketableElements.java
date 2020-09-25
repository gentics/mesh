package com.gentics.mesh.changelog.highlevel.change;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.BucketableElement;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PrepareBucketableElements extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(PrepareBucketableElements.class);

	private final Lazy<Database> db;

	@Inject
	public PrepareBucketableElements(Lazy<Database> db) {
		this.db = db;
	}

	@Override
	public String getName() {
		return "PrepareBucketableElements";
	}

	@Override
	public String getUuid() {
		return "BEB88BA9CF494CA59FFF3329F4DE1764";
	}

	@Override
	public String getDescription() {
		return "Sets the bucketIds for all bucketable elements.";
	}

	@Override
	public void apply() {
		log.info("Applying change: " + getName());
		migrate(UserImpl.class);
		migrate(GroupImpl.class);
		migrate(RoleImpl.class);
		migrate(TagImpl.class);
		migrate(TagFamilyImpl.class);
		migrate(ProjectImpl.class);
		migrate(SchemaContainerImpl.class);
		migrate(MicroschemaContainerImpl.class);
		migrate(NodeGraphFieldContainerImpl.class);
	}

	private <T extends BucketableElement> void migrate(Class<T> clazz) {
		Iterator<? extends T> it = db.get().getVerticesForType(clazz);
		long count = 0;
		while (it.hasNext()) {
			T element = it.next();
			element.generateBucketId();
			if (count % 10_000 == 0) {
				log.info("Migrated {" + count + "} of type {" + clazz.getSimpleName() + "}");
				Tx.get().commit();
			}
			count++;
		}
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}
}
