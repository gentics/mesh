package com.gentics.mesh.dagger.module;

import com.gentics.mesh.core.rest.job.JobType;
import dagger.MapKey;

/**
 * Annotation used together with {@link IntoMap} Dagger annotation to create a map that can be injected.
 */
@MapKey
@interface JobTypeKey {

	JobType value();
}
