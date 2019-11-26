package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.admin.runtimeconfig.LocalConfigModel;

public class LocalConfigExamples {
	public LocalConfigModel createExample() {
		return new LocalConfigModel().setReadOnly(false);
	}
}
