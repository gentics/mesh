package com.gentics.diktyo.tx;

import com.gentics.diktyo.type.TypeManager;

public abstract class AbstractNoTx implements NoTx {

	private final TypeManager typeManager;

	public AbstractNoTx(TypeManager typeManager) {
		this.typeManager = typeManager;
	}

	@Override
	public TypeManager type() {
		return typeManager;
	}

}
