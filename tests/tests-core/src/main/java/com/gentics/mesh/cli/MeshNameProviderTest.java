package com.gentics.mesh.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;

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
		String name = MeshNameProvider.getInstance().getRandomName();
		System.out.println("Got name: {" + name + "}");
		assertFalse(StringUtils.isEmpty(name));
	}

	@Test
	public void testFirstApril() throws Exception {
		LocalDate inputDate = LocalDate.of(2017, 4, 1);

		MeshNameProvider spy = Mockito.spy(new MeshNameProvider());
		Mockito.when(spy.getDate()).thenReturn(inputDate);
		String name = spy.getRandomName();
		assertTrue("We did expect a skynet name but we got none {" + name + "}", name.indexOf("Skynet") > 0);
	}

	@Test
	public void testNonApril() throws Exception {
		LocalDate inputDate = LocalDate.of(2017, 4, 2);

		MeshNameProvider spy = Mockito.spy(new MeshNameProvider());
		Mockito.when(spy.getDate()).thenReturn(inputDate);
		String name = spy.getRandomName();
		assertFalse("We did not expect a skynet name but we got one {" + name + "}", name.indexOf("Skynet") > 0);
	}
}
