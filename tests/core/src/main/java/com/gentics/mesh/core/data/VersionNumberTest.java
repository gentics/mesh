package com.gentics.mesh.core.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.util.VersionNumber;

/**
 * Test cases for VersionNumber
 */
public class VersionNumberTest {

	@Test
	public void testInitialVersion() {
		assertThat(new VersionNumber()).as("Initial Version").hasToString("0.1");
	}

	@Test
	public void testSpecificVersion() {
		assertThat(new VersionNumber(47, 11)).as("Version").hasToString("47.11");
	}

	@Test
	public void testNextDraft() {
		assertThat(new VersionNumber(0, 1).nextDraft()).as("Next Draft after 0.1").hasToString("0.2");
		assertThat(new VersionNumber(3, 0).nextDraft()).as("Next Draft after 3.0").hasToString("3.1");
		assertThat(new VersionNumber(47, 11).nextDraft()).as("Next Draft after 47.11").hasToString("47.12");
	}

	@Test
	public void testNextPublished() {
		assertThat(new VersionNumber(0, 1).nextPublished()).as("Next Published after 0.1").hasToString("1.0");
		assertThat(new VersionNumber(3, 0).nextPublished()).as("Next Published after 3.0").hasToString("4.0");
		assertThat(new VersionNumber(47, 11).nextPublished()).as("Next Published after 47.11").hasToString("48.0");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativMajor() {
		new VersionNumber(-3, 18);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativMinor() {
		new VersionNumber(18, -3);
	}

	@Test
	public void testFromString() {
		assertThat(new VersionNumber("47.11")).as("Version").hasToString("47.11");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalPattern() {
		new VersionNumber("Bla");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOnlyMajor() {
		new VersionNumber("47");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOnlyMinor() {
		new VersionNumber(".11");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullVersion() {
		new VersionNumber(null);
	}

	@Test
	public void testEquals() {
		VersionNumber versionA = new VersionNumber("47.11");
		VersionNumber versionB = new VersionNumber("47.12");
		assertTrue("VersionA should be equal to itself", versionA.equals(versionA));
		assertTrue("VersionA should be equal to same value", versionA.equals(versionA.toString()));
		assertFalse("VersionA should not be equal to versionB", versionA.equals(versionB));
		assertFalse("VersionA should not be equal to versionB's value", versionA.equals(versionB.toString()));
		VersionNumber versionC = new VersionNumber("47.12");
		assertTrue("VersionB should be equal to versionC since both have the same value.", versionB.equals(versionC));
	}

	@Test
	public void testCompareTo() {
		VersionNumber versionA = new VersionNumber("47.11");
		VersionNumber versionB = new VersionNumber("47.12");
		VersionNumber versionC = new VersionNumber("47.12");
		assertEquals("VersionA should be smaller than versionB", versionA.compareTo(versionB), -1);
		assertEquals("VersionB should be greater than versionA", versionB.compareTo(versionA), 1);
		assertEquals("VersionB should be equal to itself", versionB.compareTo(versionB), 0);
		assertEquals("VersionB should be equal to versionC", versionB.compareTo(versionC), 0);
	}
}
