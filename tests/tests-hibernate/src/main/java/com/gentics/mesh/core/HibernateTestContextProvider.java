package com.gentics.mesh.core;

import java.util.List;

import org.testcontainers.utility.ThrowingFunction;

import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshComponent.Builder;
import com.gentics.mesh.check.HibernateBranchCheck;
import com.gentics.mesh.dagger.DaggerHibernateMeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshInstanceProvider;
import com.gentics.mesh.test.MeshTestActions;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.MeshTestSetting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HibernateTestContextProvider extends AbstractHibernateOptionsProvider implements MeshTestContextProvider, MeshInstanceProvider<HibernateMeshOptions> {

	protected MeshComponent.Builder componentBuilder;
	protected final HibernateMeshTestActions actions = new HibernateMeshTestActions();
	
	protected static final Logger log = LoggerFactory.getLogger(HibernateTestContextProvider.class);
	
	public HibernateTestContextProvider() {
		this(null);
	}

	public HibernateTestContextProvider(MeshComponent.Builder componentBuilder) {
		if (componentBuilder != null) {
			this.componentBuilder = componentBuilder;
		} else {
			this.componentBuilder = DaggerHibernateMeshComponent.builder();
		}
	}

	@Override
	public MeshInstanceProvider<? extends MeshOptions> getInstanceProvider() {
		return this;
	}

	@Override
	public void initPhysicalStorage(MeshTestSetting settings) throws Exception {

	}

	@Override
	public void initMeshData(MeshTestSetting settings, MeshComponent meshDagger) {
		// since some tests setup branches without migrating the root of the project to it, the HibernateBranchCheck will fail on them.
		// therefore we exclude this consistency check for now
		List<ConsistencyCheck> checks = meshDagger.consistencyChecks();
		checks.removeIf(check -> check instanceof HibernateBranchCheck);

		// remove "async only" tests, if env is not set
		if (!Boolean.valueOf(System.getenv("MESH_CONSISTENCY_CHECKS"))) {
			checks.removeIf(check -> check.asyncOnly());
		}
	}

	@Override
	public void initFolders(ThrowingFunction<String, String> pathProvider) throws Exception {
				
	}

	@Override
	public void cleanupPhysicalStorage() throws Exception {
				
	}

	@Override
	public Builder getComponentBuilder() {
		return componentBuilder;
	}

	@Override
	public void teardownStorage() {
		
	}

	@Override
	public MeshTestActions actions() {
		return actions;
	}

	@Override
	public HibernateMeshOptions getOptions() {
		fillMeshOptions(meshOptions);
		return meshOptions;
	}

	@Override
	public HibernateMeshOptions getClone() throws Exception {
		return HibernateMeshOptions.class.cast(super.getClone());
	}

	@Override
	public void setSyncWrites(boolean syncWrites) {
		meshOptions.getStorageOptions().setSynchronizeWrites(syncWrites);
	}
}
