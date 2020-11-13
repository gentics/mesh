package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;

public class LocalConfigExamples {
	public LocalConfigModel createExample() {
		return new LocalConfigModel().setReadOnly(false);
	}
}
