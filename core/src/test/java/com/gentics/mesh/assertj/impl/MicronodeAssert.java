package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.node.Micronode;

public class MicronodeAssert extends AbstractAssert<MicronodeAssert, Micronode> {
	public MicronodeAssert(Micronode actual) {
		super(actual, MicronodeAssert.class);
	}

	public MicronodeAssert containsStringField(String name, String value) {
		assertThat(actual).as(descriptionText()).isNotNull();
		assertThat(actual.getString(name)).as(descriptionText() + " string field '" + name + "'").isNotNull();
		assertThat(actual.getString(name).getString()).as(descriptionText() + " string field '" + name + "' value").isEqualTo(value);
		return this;
	}
}
