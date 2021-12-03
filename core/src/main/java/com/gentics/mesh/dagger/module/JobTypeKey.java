package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.rest.job.JobType;
import dagger.MapKey;

@MapKey
@interface JobTypeKey {

	JobType value();
}
