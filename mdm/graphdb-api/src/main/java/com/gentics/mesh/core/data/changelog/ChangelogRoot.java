package com.gentics.mesh.core.data.changelog;

import java.util.Iterator;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.dao.ChangelogDao;

/**
 * Root element for @link {@link ChangeMarkerVertex} vertices.
 */
public interface ChangelogRoot extends MeshVertex, ChangelogDao {

	@Override
	Iterator<? extends ChangeMarkerVertex> findAll();
}
