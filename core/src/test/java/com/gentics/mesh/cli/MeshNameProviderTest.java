package com.gentics.mesh.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Test;

public class MeshNameProviderTest {

	@Test
	public void testGetRandomName() {
		assertNotNull(MeshNameProvider.getInstance().getRandomName());
		for (int i = 0; i < 30000; i++) {
			assertNotNull(MeshNameProvider.getInstance().getRandomName());
		}
	}

	@Test
	public void testGetName() throws Exception {
		String name = MeshNameProvider.getInstance().getName();
		System.out.println("Got name: {" + name + "}");
		assertFalse(StringUtils.isEmpty(name));
		assertEquals(name, MeshNameProvider.getInstance().getName());
		MeshNameProvider.getInstance().reset();
		String newName = MeshNameProvider.getInstance().getName();
		assertFalse(StringUtils.isEmpty(newName));
		assertEquals(newName, MeshNameProvider.getInstance().getName());
		assertNotEquals(newName, name);
	}

	@Test
	public void testFirstApril() throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
		Date firstApril = sdf.parse("01-01");
		// always return the same time when querying current time
		DateTimeUtils.setCurrentMillisFixed(firstApril.getTime());
		String name = MeshNameProvider.getInstance().getRandomName();
		assertTrue("We did expect a skynet name but we got none {" + name + "}", name.indexOf("Skynet") > 0);
	}

	@Test
	public void testNonApril() throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
		Date secondApril = sdf.parse("02-01");
		// always return the same time when querying current time
		DateTimeUtils.setCurrentMillisFixed(secondApril.getTime());
		String name = MeshNameProvider.getInstance().getRandomName();
		assertFalse("We did not expect a skynet name but we got one {" + name + "}", name.indexOf("Skynet") > 0);
	}

	@After
	public void cleanUp() {
		DateTimeUtils.setCurrentMillisSystem();
	}
}
