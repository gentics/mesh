package com.gentics.mesh.plugin.pf4j;

import org.pf4j.DefaultExtensionFactory;

import com.gentics.mesh.plugin.ext.RestExtension;

public class MeshExtensionFactory extends DefaultExtensionFactory {

	@Override
	public <T> T create(Class<T> extensionClass) {
		T ext = super.create(extensionClass);
		if (ext instanceof RestExtension) {
			((RestExtension) ext).initialize();
		}
		return ext;
	}

}
