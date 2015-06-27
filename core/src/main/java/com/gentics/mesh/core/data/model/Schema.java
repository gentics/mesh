package com.gentics.mesh.core.data.model;

import java.util.List;

import com.gentics.mesh.core.data.model.impl.SchemaImpl;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.rest.schema.response.SchemaResponse;

public interface Schema extends GenericNode {

	public static final String CONTENT_KEYWORD = "content";
	public static final String DISPLAY_NAME_KEYWORD = "displayName";
	public static final String NAME_KEYWORD = "name";
	public static final String TEASER_KEYWORD = "teaser";
	public static final String TITLE_KEYWORD = "title";

	String getName();

	void setName(String name);

	String getDisplayName();

	void setDisplayName(String displayName);

	String getDescription();

	void setDescription(String description);

	void setNestingAllowed(boolean b);

	boolean isNestingAllowed();

	BasicPropertyType create(String displayNameKeyword, PropertyType type);

	void addPropertyTypeSchema(BasicPropertyType propertyType);

	List<? extends Project> getProjects();

	void addProject(Project project);


	BasicPropertyType createBasicPropertyTypeSchema(String displayNameKeyword, PropertyType type);

	SchemaImpl getImpl();

	List<? extends BasicPropertyType> getPropertyTypes();

	SchemaResponse transformToRest(MeshAuthUser requestUser);

}
