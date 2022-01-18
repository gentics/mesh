package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.jobs.BranchJobProcessor;
import com.gentics.mesh.core.jobs.JobProcessor;
import com.gentics.mesh.core.jobs.JobProcessorImpl;
import com.gentics.mesh.core.jobs.MicronodeJobProcessor;
import com.gentics.mesh.core.jobs.NodeJobProcessor;
import com.gentics.mesh.core.jobs.SingleJobProcessor;
import com.gentics.mesh.core.jobs.VersionPurgeJobProcessor;
import com.gentics.mesh.core.rest.job.JobType;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class JobProcessingModule {

	@Binds
	abstract JobProcessor jobProcessor(JobProcessorImpl e);

	@Binds
	@IntoMap
	@JobTypeKey(JobType.schema)
	abstract SingleJobProcessor nodeJobProcessor(NodeJobProcessor e);

	@Binds
	@IntoMap
	@JobTypeKey(JobType.microschema)
	abstract SingleJobProcessor microNodeJobProcessor(MicronodeJobProcessor e);

	@Binds
	@IntoMap
	@JobTypeKey(JobType.branch)
	abstract SingleJobProcessor branchJobProcessor(BranchJobProcessor e);

	@Binds
	@IntoMap
	@JobTypeKey(JobType.versionpurge)
	abstract SingleJobProcessor versionPurgeJobProcessor(VersionPurgeJobProcessor e);
}
