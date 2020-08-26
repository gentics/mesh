package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.user.HibCreatorTracking;

/**
 * Compat interface which must be removed once HibNode has been moved to mesh-mdm-api
 *
 */
public interface HibInNode  extends HibCoreElement, HibCreatorTracking{

	String getElementVersion();

}
