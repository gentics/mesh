package com.gentics.mesh.test.context;

import com.gentics.mesh.etc.config.MeshOptions;

import java.util.function.Consumer;

public enum MeshOptionChanger {
	NO_CHANGE(ignore -> {}),
	SMALL_EVENT_BUFFER(options -> {
		options.getSearchOptions().setEventBufferSize(100);
	});

	final Consumer<MeshOptions> changer;

	MeshOptionChanger(Consumer<MeshOptions> changer) {
		this.changer = changer;
	}
}
