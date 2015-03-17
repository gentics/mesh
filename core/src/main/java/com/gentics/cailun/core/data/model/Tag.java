package com.gentics.cailun.core.data.model;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericTag;

@NodeEntity
public class Tag extends GenericTag<Tag, GenericFile> {

	private static final long serialVersionUID = 7645315435657775862L;

	private static Label label = DynamicLabel.label(Tag.class.getSimpleName());

	//TODO this should be a relationship to the schema node
	private String schemaName;

	public Tag() {
		this.schemaName = "tag";
	}

	public static Label getLabel() {

		/**
		 * TODO check whether the CallerSensitive annotation could be used to move this method into an abstract class?
		 * 
		 * @CallerSensitive public static Package getPackage(String name) { ClassLoader l = ClassLoader.getClassLoader(Reflection.getCallerClass());
		 */
		return label;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schema) {
		this.schemaName = schema;
	}
}
