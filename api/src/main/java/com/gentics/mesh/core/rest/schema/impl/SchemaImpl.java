package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.json.MeshJsonException;

public class SchemaImpl implements RestModel, Schema {

	private String name;
	private String description;
	private String displayField;
	private String segmentField;
	private boolean folder = false;
	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	@Override
	public String getSegmentField() {
		return segmentField;
	}

	@Override
	public void setSegmentField(String segmentField) {
		this.segmentField = segmentField;
	}

	@Override
	public boolean isFolder() {
		return folder;
	}

	@Override
	public void setFolder(boolean flag) {
		this.folder = flag;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public void removeField(String name) {
		int elementToBeRemoved = -1;
		for (int i = 0; i < fields.size(); i++) {
			FieldSchema schema = fields.get(i);
			if (schema.getName().equals(name)) {
				elementToBeRemoved = i;
				break;
			}
		}
		if (elementToBeRemoved != -1) {
			fields.remove(elementToBeRemoved);
		}
	}

	@Override
	public void addField(FieldSchema fieldSchema) {
		this.fields.add(fieldSchema);
	}

	@Override
	public void validate() throws MeshJsonException {
		// TODO make sure that the display name field only maps to string fields since NodeImpl can currently only deal with string field values for
		// displayNames

		Set<String> fieldNames = new HashSet<>();
		Set<String> fieldLabels = new HashSet<>();
		for (FieldSchema fieldSchema : fields) {
			String name = fieldSchema.getName();
			String label = fieldSchema.getLabel();
			if (fieldNames.contains(name)) {
				throw new MeshJsonException("The schema contains duplicate names. The name for a schema field must be unique.");
			} else {
				fieldNames.add(name);
			}
			if (fieldLabels.contains(label)) {
				throw new MeshJsonException("The schema contains duplicate labels. The label for a schema field must be unique.");
			} else {
				fieldLabels.add(label);
			}
		}

	}

	@Override
	public Optional<FieldSchema> getFieldSchema(String fieldName) {
		return fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst();
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
