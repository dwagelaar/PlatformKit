package be.ac.vub.platformkit.presentation.popup.action;

import be.ac.vub.platformkit.presentation.jobs.SortPlatformkitModelJob;

/**
 * Sorts the options in a PlatformKit model least-specific first.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class SortLeastSpecificFirst extends SortPlatformkitModel {

	/**
	 * Creates a new {@link SortLeastSpecificFirst}.
	 */
	public SortLeastSpecificFirst() {
		super(SortPlatformkitModelJob.LEAST_SPECIFIC);
	}

}
