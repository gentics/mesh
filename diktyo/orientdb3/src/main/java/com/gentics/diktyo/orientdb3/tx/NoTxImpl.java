package com.gentics.diktyo.orientdb3.tx;

import javax.inject.Inject;

import com.gentics.diktyo.tx.AbstractNoTx;
import com.gentics.diktyo.type.TypeManager;

public class NoTxImpl extends AbstractNoTx {


	@Inject
	public NoTxImpl(TypeManager typeManager) {
		super(typeManager);
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

}
