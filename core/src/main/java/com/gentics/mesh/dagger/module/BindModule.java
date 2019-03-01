package com.gentics.mesh.dagger.module;

import com.gentics.mesh.auth.MeshOAuth2ServiceImpl;
import com.gentics.mesh.auth.MeshOAuthService;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.impl.SearchQueueBatchImpl;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.DropwizardMetricsService;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.common.DropIndexHandlerImpl;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class BindModule {

	@Binds
	abstract DropIndexHandler bindCommonHandler(DropIndexHandlerImpl e);

	@Binds
	abstract SearchQueueBatch bindSQB(SearchQueueBatchImpl e);	

	@Binds
	abstract BootstrapInitializer bindBoot(BootstrapInitializerImpl e);

	@Binds
	abstract WebRootService bindWebrootService(WebRootServiceImpl e);

	@Binds
	abstract MeshOAuthService bindOAuthHandler(MeshOAuth2ServiceImpl e);

	@Binds
	abstract MetricsService bindMetricsService(DropwizardMetricsService e);
	
	@Binds
	abstract Database bindDatabase(OrientDBDatabase e);
}
