package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.util.RxDebugger;

public class FullIndexStressTest extends AbstractSearchVerticleTest {

//	@BeforeClass
//	public static void debug() {
//		new RxDebugger().start();
//	}

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		return new ArrayList<>();
	}

	@Test
	public void testA() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testB() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testC() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testD() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testA3() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testB3() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testC3() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testD3() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testA2() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testB2() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testC2() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testD2() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testA1() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testB1() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testC1() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}

	@Test
	public void testD1() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}
	}
}
