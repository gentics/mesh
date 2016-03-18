package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;

public class MicronodeAssert extends AbstractObjectAssert<MicronodeAssert, Micronode> {
	public MicronodeAssert(Micronode actual) {
		super(actual, MicronodeAssert.class);
	}

	public MicronodeAssert containsStringField(String name, String value) {
		assertThat(actual).as(descriptionText()).isNotNull();
		assertThat(actual.getString(name)).as(descriptionText() + " string field '" + name + "'").isNotNull();
		assertThat(actual.getString(name).getString()).as(descriptionText() + " string field '" + name + "' value").isEqualTo(value);
		return this;
	}

	/**
	 * Assert that the micronode uses the given microschema
	 * @param microschemaContainer microschema container
	 * @return fluent API
	 */
	public MicronodeAssert isOf(MicroschemaContainerVersion microschemaContainer) {
		assertThat(actual.getMicroschemaContainerVersion()).as(descriptionText() + " Microschema").equals(microschemaContainer);
		return this;
	}

}
