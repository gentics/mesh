package com.gentics.mesh.dagger;

import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.impl.SearchQueueBatchImpl;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.common.DropIndexHandlerImpl;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class ExtraModule {

	@Binds
	abstract DropIndexHandler bindCommonHandler(DropIndexHandlerImpl e);
	
	@Binds
	abstract SearchQueueBatch bindSQB(SearchQueueBatchImpl e);
}
