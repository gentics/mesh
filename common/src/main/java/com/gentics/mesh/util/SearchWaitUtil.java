package com.gentics.mesh.util;

import com.gentics.mesh.parameter.ParameterProviderContext;
import io.reactivex.Completable;

public interface SearchWaitUtil {

	boolean delayRequested(ParameterProviderContext ppc);

	Completable awaitSync(ParameterProviderContext ppc);

	Completable waitForIdle();
}
