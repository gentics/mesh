package com.gentics.mesh.core.field.micronode;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface MicronodeListFieldHelper {

	FieldFetcher FETCH = GraphFieldContainer::getMicronodeList;

    //		field.addItem(field.createMicronode(field));
// TestDataProvider.getInstance().getMicroschemaContainers().get("vcard").getLatestVersion());
//Micronode micronode = field.getMicronode();
//		micronode.createString("firstName").setString("Donald");
//		micronode.createString("lastName").setString("Duck");
    DataProvider FILL = GraphFieldContainer::createMicronodeFieldList;

	DataProvider CREATE_EMPTY = GraphFieldContainer::createMicronodeFieldList;
}
