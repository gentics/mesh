package com.gentics.mesh.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for {@link CompareUtils#equals(Object, Object, boolean, boolean)}
 */
@RunWith(Parameterized.class)
public class CompareUtilsTest {
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] {
			{
				"null is null",
				null,
				null,
				false,
				false,
				true
			},
			{
				"null is not empty",
				null,
				"",
				false,
				false,
				false
			},
			{
				"null can be empty",
				null,
				"",
				true,
				false,
				true
			},
			{
				"empty is not null",
				"",
				null,
				false,
				false,
				false
			},
			{
				"null means unchanged",
				"a",
				null,
				false,
				true,
				true
			},
			{
				"equal arrays",
				new String[] {"a", "b", "c"},
				new String[] {"a", "b", "c"},
				false,
				false,
				true
			},
			{
				"unequal arrays",
				new String[] {"a", "b", "c"},
				new String[] {"a", "b"},
				false,
				false,
				false
			},
			{
				"arrays with different order",
				new String[] {"a", "b", "c"},
				new String[] {"c", "b", "a"},
				false,
				false,
				false
			},
			{
				"empty array is not null",
				null,
				new String[0],
				false,
				false,
				false
			},
			{
				"empty array can be null",
				null,
				new String[0],
				true,
				false,
				true
			},
			{
				"equal lists",
				Arrays.asList("a", "b", "c"),
				Arrays.asList("a", "b", "c"),
				false,
				false,
				true
			},
			{
				"unequal lists",
				Arrays.asList("a", "b", "c"),
				Arrays.asList("b", "c"),
				false,
				false,
				false
			},
			{
				"lists with different order",
				Arrays.asList("a", "b", "c"),
				Arrays.asList("c", "b", "a"),
				false,
				false,
				false
			},
			{
				"empty list is not null",
				null,
				Collections.emptyList(),
				false,
				false,
				false
			},
			{
				"empty list can be null",
				null,
				Collections.emptyList(),
				true,
				false,
				true
			},
			{
				"equal sets",
				Set.of("a", "b", "c"),
				Set.of("a", "b", "c"),
				false,
				false,
				true
			},
			{
				"unequal sets",
				Set.of("a", "b", "c"),
				Set.of("a", "c"),
				false,
				false,
				false
			},
			{
				"sets with different order",
				Set.of("a", "b", "c"),
				Set.of("c", "b", "a"),
				false,
				false,
				true
			},
			{
				"empty set is not null",
				null,
				Collections.emptySet(),
				false,
				false,
				false
			},
			{
				"empty set can be null",
				null,
				Collections.emptySet(),
				true,
				false,
				true
			},

			{
				"equal maps",
				Map.of("a", "1", "b", "2", "c", "3"),
				Map.of("a", "1", "b", "2", "c", "3"),
				false,
				false,
				true
			},
			{
				"unequal maps",
				Map.of("a", "1", "b", "2", "c", "3"),
				Map.of("a", "1", "b", "4", "c", "3"),
				false,
				false,
				false
			},
			{
				"maps with different order",
				Map.of("a", "1", "b", "2", "c", "3"),
				Map.of("c", "3", "b", "2", "a", "1"),
				false,
				false,
				true
			},
			{
				"empty map is not null",
				null,
				Collections.emptyMap(),
				false,
				false,
				false
			},
			{
				"empty map can be null",
				null,
				Collections.emptyMap(),
				true,
				false,
				true
			},
		});
	}

	/**
	 * Test case description
	 */
	@Parameter(0)
	public String description;

	/**
	 * First value to compare
	 */
	@Parameter(1)
	public Object firstValue;

	/**
	 * Second value to compare
	 */
	@Parameter(2)
	public Object secondValue;

	/**
	 * Flag whether null shall be treated equal to empty
	 */
	@Parameter(3)
	public boolean nullIsEmpty;

	/**
	 * Flag whether null as object b always means "unchanged" ("equal")
	 */
	@Parameter(4)
	public boolean nullIsUnchanged;

	/**
	 * True, when the values are expected to be "equal"
	 */
	@Parameter(5)
	public boolean expectEqual;

	@Test
	public void testCompare() {
		assertThat(CompareUtils.equals(firstValue, secondValue, nullIsEmpty, nullIsUnchanged)).as(description)
				.isEqualTo(expectEqual);
	}
}
