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

	private String schema = null;

	public Tag() {
		this.schema = "tag";
	}

	public static Label getLabel() {

		/**
		 * TODO check whether the CallerSensitive annotation could be used to move this method into an abstract class?
		 * 
		 * @CallerSensitive public static Package getPackage(String name) { ClassLoader l = ClassLoader.getClassLoader(Reflection.getCallerClass());
		 */
		return label;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}
}
