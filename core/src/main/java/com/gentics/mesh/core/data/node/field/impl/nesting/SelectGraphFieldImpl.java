package com.gentics.mesh.core.data.node.field.impl.nesting;

import java.util.List;

import com.gentics.mesh.core.data.node.field.nesting.AbstractComplexGraphField;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.SelectGraphField;

public class SelectGraphFieldImpl<T extends ListableGraphField> extends AbstractComplexGraphField implements SelectGraphField<T> {

	@Override
	public void addOption(T t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<T> getOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeOption(T t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAllOptions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public T getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMultiselect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setMultiselect(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<T> getSelections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}

}
