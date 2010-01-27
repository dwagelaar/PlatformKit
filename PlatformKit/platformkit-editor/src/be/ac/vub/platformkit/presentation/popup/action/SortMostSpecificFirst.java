package be.ac.vub.platformkit.presentation.popup.action;

import be.ac.vub.platformkit.presentation.jobs.SortPlatformkitModelJob;

/**
 * Sorts the options in a PlatformKit model most-specific first.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class SortMostSpecificFirst extends SortPlatformkitModel {

	/**
	 * Creates a new {@link SortMostSpecificFirst}.
	 */
	public SortMostSpecificFirst() {
		super(SortPlatformkitModelJob.MOST_SPECIFIC);
	}

}
