
package com.gentics.mesh.assertj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.core.rest.event.MeshElementEventModel;

public abstract class AbstractMeshElementEventModelAssert<S extends AbstractMeshElementEventModelAssert<S, A>, A extends MeshElementEventModel>
	extends AbstractMeshEventModelAssert<S, A> {

	protected AbstractMeshElementEventModelAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public S hasName(String name) {
		assertEquals("Name in the event did not match.", name, actual.getName());
		return (S) this;
	}

	public S hasUuid(String uuid) {
		assertEquals("Uuid in the event did not match.", uuid, actual.getUuid());
		return (S) this;
	}

	public S uuidNotNull() {
		assertNotNull("Uuid in the event should not be null.", actual.getUuid());
		return (S) this;
	}
}
