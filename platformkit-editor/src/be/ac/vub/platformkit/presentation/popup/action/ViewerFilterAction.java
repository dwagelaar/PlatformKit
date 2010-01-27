package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.jface.viewers.ViewerFilter;

/**
 * General superclass for popup actions with a viewer filter.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class ViewerFilterAction extends ObjectSelectionAction {

	private ViewerFilter filter = null;

	/**
	 * Creates a new {@link ViewerFilterAction}.
	 */
	public ViewerFilterAction() {
		super();
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(ViewerFilter filter) {
		this.filter = filter;
	}

	/**
	 * @return the filter
	 */
	public ViewerFilter getFilter() {
		return filter;
	}

}
