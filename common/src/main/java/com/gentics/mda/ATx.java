package com.gentics.mda;

import com.gentics.madl.tx.Tx;
import com.gentics.mda.entitycollection.UserDao;

public interface ATx extends Tx {
	UserDao users();
}
