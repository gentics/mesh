package com.gentics.mesh.core.field.micronode;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.TestDataProvider;

public interface MicronodeFieldHelper {

	FieldFetcher FETCH = GraphFieldContainer::getMicronode;
	
	DataProvider FILL = (container, name) -> {
		MicronodeGraphField field = container.createMicronode(name, TestDataProvider.getInstance().getMicroschemaContainers().get("vcard").getLatestVersion());

		Micronode micronode = field.getMicronode();
		micronode.createString("firstName").setString("Donald");
		micronode.createString("lastName").setString("Duck");
	};

	
	DataProvider CREATE_EMPTY = (container, name) ->  {
		MicronodeGraphField field = container.createMicronode(name, TestDataProvider.getInstance().getMicroschemaContainers().get("vcard").getLatestVersion());
		field.getMicronode();
		// Create no fields
	};

}
