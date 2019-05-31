package com.gentics.mesh.test.context;

import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2Options;

import java.util.function.Consumer;

public enum MeshOptionChanger {
	NO_CHANGE(ignore -> {}),
	SMALL_EVENT_BUFFER(options -> {
		options.getSearchOptions().setEventBufferSize(100);
	}),
	WITH_MAPPER_SCRIPT(options -> {
		AuthenticationOptions auth = options.getAuthenticationOptions();
		OAuth2Options oauth2options = auth.getOauth2();
		oauth2options.setMapperScriptDevMode(true);
		oauth2options.setMapperScriptPath("src/test/resources/oauth2/mapperscript.js");
	});

	final Consumer<MeshOptions> changer;

	MeshOptionChanger(Consumer<MeshOptions> changer) {
		this.changer = changer;
	}
}
