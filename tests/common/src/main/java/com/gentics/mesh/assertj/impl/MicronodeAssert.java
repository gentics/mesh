package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;

public class MicronodeAssert extends AbstractObjectAssert<MicronodeAssert, Micronode> {
	public MicronodeAssert(Micronode actual) {
		super(actual, MicronodeAssert.class);
	}

	/**
	 * Assert that the micronode contains the string field with the given value.
	 * 
	 * @param name
	 *            Name of the string field
	 * @param value
	 *            Value of the string
	 * @return Fluent API
	 */
	public MicronodeAssert containsStringField(String name, String value) {
		assertThat(actual).as(descriptionText()).isNotNull();
		assertThat(actual.getString(name)).as(descriptionText() + " string field '" + name + "'").isNotNull();
		assertThat(actual.getString(name).getString()).as(descriptionText() + " string field '" + name + "' value").isEqualTo(value);
		return this;
	}

	/**
	 * Assert that the micronode uses the given microschema.
	 * 
	 * @param microschemaVersion
	 *            microschema container
	 * @return Fluent API
	 */
	public MicronodeAssert isOf(HibMicroschemaVersion microschemaVersion) {
		assertTrue(actual.getSchemaContainerVersion().getUuid().equals(microschemaVersion.getUuid()));
		return this;
	}

}
