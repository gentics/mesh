package com.gentics.madl.query;

/**
 * MADL SQL query interface.
 */
public interface MadlGraphQuery extends GraphQuery {

	/**
	 * (Blueprints Extension) Sets the labels to filter. Labels are bound to Class
	 * names by default.
	 *
	 * @param labels String vararg of labels
	 * @return Current Query Object to allow calls in chain.
	 */
	MadlGraphQuery labels(String... labels);

	/**
	 * Skips first iSkip items from the result set.
	 *
	 * @param iSkip Number of items to skip on result set
	 * @return Current Query Object to allow calls in chain.
	 */
	MadlGraphQuery skip(long iSkip);

	/**
	 * (Blueprints Extension) Sets the order of results by a field in ascending
	 * (asc) order. This is translated on ORDER BY in the underlying SQL query.
	 *
	 * @param props Field to order by
	 * @return Current Query Object to allow calls in chain.
	 */
	MadlGraphQuery order(String props);

	/**
	 * (Blueprints Extension) Sets the order of results by a field in ascending
	 * (asc) or descending (desc) order based on dir parameter. This is translated
	 * on ORDER BY in the underlying SQL query.
	 *
	 * @param props Field to order by
	 * @param dir   Direction. Use "asc" for ascending and "desc" for descending
	 * @return Current Query Object to allow calls in chain.
	 */
	MadlGraphQuery order(String props, String dir);

	/** (Blueprints Extension) Returns the fetch plan used. */
	String getFetchPlan();

	/**
	 * (Blueprints Extension) Sets the fetch plan to use on returning result set.
	 */
	void setFetchPlan(String fetchPlan);

}