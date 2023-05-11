package com.gentics.mesh.test;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Change Mesh options before starting the test class.
 * 
 * @author plyhun
 *
 */
public interface MeshOptionChanger {

	void change(MeshOptions options);

	static final class NoOptionChanger implements MeshOptionChanger {

		@Override
		public void change(MeshOptions options) {
			MeshCoreOptionChanger.NO_CHANGE.change(options);
		}		
	}
}
