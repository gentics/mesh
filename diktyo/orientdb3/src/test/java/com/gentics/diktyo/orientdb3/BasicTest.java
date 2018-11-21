package com.gentics.diktyo.orientdb3;

import static com.gentics.diktyo.db.DatabaseType.MEMORY;

import org.junit.Test;

import com.gentics.diktyo.Diktyo;
import com.gentics.diktyo.db.Database;
import com.gentics.diktyo.db.DatabaseType;
import com.gentics.diktyo.index.Index;

/*
 * Test which covers the basic function of the OrientDB vendor implementation.
 */
public class BasicTest {

	@Test
	public void testBasics() {
		Diktyo diktyo = Diktyo.diktyo();
		diktyo.db().create("test", MEMORY);
		try (Database db = diktyo.db().open("test", MEMORY)) {
			for (Index index: db.index().list()) {
				System.out.println(index.name());
			}
		}
		Database db = diktyo.db().open("test", MEMORY);
		db.close();
		db = diktyo.db().open("test", MEMORY);
	}

}
