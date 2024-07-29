package com.gentics.mesh.core.rest.test;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.json.JsonUtil;

public class UnwrapTest {

	@Test
	public void testUnwrap() {
		BinaryFieldImpl model = new BinaryFieldImpl();
		BinaryMetadata metadata = new BinaryMetadata();
		metadata.setLocation(42.0, 41.0);
		metadata.add("ene", "muh");
		metadata.add("name", "muhue2");
		model.setMetadata(metadata);

		String json = model.toJson();
		System.out.println(json);
		BinaryFieldImpl model2 = JsonUtil.readValue(json,	BinaryFieldImpl.class);
		System.out.println(model2.getMetadata().getLocation());
		System.out.println(model2.getMetadata().getMap().toString());
		System.out.println(model2.toJson());

	}
}
