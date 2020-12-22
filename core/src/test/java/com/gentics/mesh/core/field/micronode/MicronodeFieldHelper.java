package com.gentics.mesh.core.field.micronode;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.TestDataProvider;

/**
 * Test helper for micronode fields.
 */
public interface MicronodeFieldHelper {

	public static final FieldFetcher FETCH = (container, name) -> container.getMicronode(name);
	
	public final DataProvider FILL = (container, name) -> {
		MicronodeGraphField field = container.createMicronode(name, TestDataProvider.getInstance().getMicroschemaContainers().get("vcard").getLatestVersion());

		Micronode micronode = field.getMicronode();
		micronode.createString("firstName").setString("Donald");
		micronode.createString("lastName").setString("Duck");
	};

	
	public static final DataProvider CREATE_EMPTY = (container, name) ->  {
		MicronodeGraphField field = container.createMicronode(name, TestDataProvider.getInstance().getMicroschemaContainers().get("vcard").getLatestVersion());
		field.getMicronode();
		// Create no fields
	};

}
