package be.ac.vub.platformkit.presentation.util.provider;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.CommandParameter;
import org.eclipse.emf.edit.command.CreateChildCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.IChangeNotifier;
import org.eclipse.emf.edit.provider.IDisposable;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.INotifyChangedListener;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;

import be.ac.vub.platformkit.kb.Ontologies;
import be.ac.vub.platformkit.presentation.util.IEObjectValidator;

/**
 * Adapter that can change the available child items and their order
 * in an EMF-based editor.
 * @author dennis
 *
 */
public class PlatformKitItemProviderAdapter implements
		Adapter.Internal, 
		IEditingDomainItemProvider, 
		ITreeItemContentProvider, 
		IItemLabelProvider, 
		IItemPropertySource, 
		IChangeNotifier,
	    IDisposable,
	    CreateChildCommand.Helper,
	    ResourceLocator {

	private ItemProviderAdapter inner;
	private AdapterFactory factory;
	private IEObjectValidator validator = null;
	protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);
	
	/**
	 * Creates a new PlatformKitItemProviderAdapter
	 * @param inner The wrapped ItemProviderAdapter
	 * @param factory The adapter factory to create new wrapped adapters
	 */
	public PlatformKitItemProviderAdapter(ItemProviderAdapter inner, AdapterFactory factory) {
		Assert.isNotNull(inner);
		Assert.isNotNull(factory);
		this.inner = inner;
		this.factory = factory;
	}
	
	/**
	 * Attach wrapper and validator if msg is an EObject.
	 * @param msg
	 */
	public void notifyChanged(Notification msg) {
		inner.notifyChanged(msg);
		Object object = msg.getNewValue();
		if (object instanceof EObject) {
			updateObject((EObject) object);
		}
	}

    /**
     * Updates the given EObject with wrappers and validators.
     * @param object
     */
    protected void updateObject(EObject object) {
    	Assert.isNotNull(object);
    	if (object.eAdapters().size() == 0) {
        	// Bug #26: Profiling the Instant Messenger configuration editor throws ClassCastException
    		// Solved: use AdapterFactory from editingDomain to pro-actively create underlying adapter
			logger.info("Creating new adapters using adapter factory " + factory);
			factory.adaptAllNew(object);
    	}
		for (int i = 0; i < object.eAdapters().size(); i++) {
			Object adapter = object.eAdapters().get(i);
			if (adapter instanceof ItemProviderAdapter) {
				PlatformKitItemProviderAdapter wrapper =
					new PlatformKitItemProviderAdapter((ItemProviderAdapter) adapter, factory);
				wrapper.setValidator(getValidator());
				logger.info("Created wrapper adapter for new child " + object.toString());
				object.eAdapters().set(i, wrapper);
			}
		}
    }
    
	/**
	 * @return The validated child item options in the optimised order.
	 */
	public Collection getNewChildDescriptors(Object object,
			EditingDomain editingDomain, Object sibling) {
		Collection ncds = inner.getNewChildDescriptors(object, editingDomain, sibling);
		if (getValidator() == null) {
			return ncds;
		}
		//create sorted map of NewChildDescriptors
		SortedMap sortedncds = new TreeMap();
		List unsortedncds = new ArrayList();
		for (Iterator it = ncds.iterator(); it.hasNext();) {
			Object ncd = it.next();
			if (ncd instanceof CommandParameter) {
				CommandParameter cp = (CommandParameter) ncd;
				Object value = cp.getValue();
				if (value instanceof EObject) {
					int index = getValidator().indexOf((EObject) value);
					if (index > -1) {
						sortedncds.put(new Integer(index), ncd);
						continue;
					}
				}
			}
			unsortedncds.add(ncd);
		}
		//collect valid NewChildDescriptors
		Collection validncds = new ArrayList();
		while (!sortedncds.isEmpty()) {
			Object key = sortedncds.firstKey();
			Object ncd = sortedncds.get(key);
			if (isValid(ncd)) {
				validncds.add(ncd);
			}
			sortedncds.remove(key);
		}
		for (Iterator it = unsortedncds.iterator(); it.hasNext();) {
			Object ncd = it.next();
			if (isValid(ncd)) {
				validncds.add(ncd);
			}
		}
		return validncds;
	}
	
	/**
	 * @param ncd NewChildDescriptor
	 * @return True if the NewChildDescriptor is valid, false otherwise.
	 */
	private boolean isValid(Object ncd) {
		if (ncd instanceof CommandParameter) {
			CommandParameter cp = (CommandParameter) ncd;
			Object value = cp.getValue();
			if (value instanceof EObject) {
				return getValidator().isValid((EObject) value);
			}
		}
		return true;
	}
	
	public Command createCommand(Object object, EditingDomain editingDomain,
			Class commandClass, CommandParameter commandParameter) {
		return inner.createCommand(object, editingDomain, commandClass, commandParameter);
	}

	public Collection getChildren(Object object) {
		return inner.getChildren(object);
	}

	public Object getParent(Object object) {
		return inner.getParent(object);
	}

	public Collection getElements(Object object) {
		return inner.getElements(object);
	}

	public boolean hasChildren(Object object) {
		return inner.hasChildren(object);
	}

	public Object getImage(Object object) {
		return inner.getImage(object);
	}

	public String getText(Object object) {
		return inner.getText(object);
	}

	public Object getEditableValue(Object object) {
		return inner.getEditableValue(object);
	}

	public IItemPropertyDescriptor getPropertyDescriptor(Object object,
			Object propertyID) {
		return inner.getPropertyDescriptor(object, propertyID);
	}

	public List getPropertyDescriptors(Object object) {
		return inner.getPropertyDescriptors(object);
	}

	public void addListener(INotifyChangedListener listener) {
		inner.addListener(listener);
	}

	public String crop(String text) {
		return inner.crop(text);
	}

	public void dispose() {
		inner.dispose();
	}

	public void fireNotifyChanged(Notification notification) {
		inner.fireNotifyChanged(notification);
	}

	public AdapterFactory getAdapterFactory() {
		return inner.getAdapterFactory();
	}

	public URL getBaseURL() {
		return inner.getBaseURL();
	}

	public String getCreateChildDescription(Object owner, Object feature, Object child, Collection selection) {
		return inner.getCreateChildDescription(owner, feature, child, selection);
	}

	public Object getCreateChildImage(Object owner, Object feature, Object child, Collection selection) {
		return inner.getCreateChildImage(owner, feature, child, selection);
	}

	public Collection getCreateChildResult(Object child) {
		return inner.getCreateChildResult(child);
	}

	public String getCreateChildText(Object owner, Object feature, Object child, Collection selection) {
		return inner.getCreateChildText(owner, feature, child, selection);
	}

	public String getCreateChildToolTipText(Object owner, Object feature, Object child, Collection selection) {
		return inner.getCreateChildToolTipText(owner, feature, child, selection);
	}

	public Object getImage(String key) {
		return inner.getImage(key);
	}

	public Object getPropertyValue(Object object, String property) {
		return inner.getPropertyValue(object, property);
	}

	public String getString(String key, boolean translate) {
		return inner.getString(key, translate);
	}

	public String getString(String key, Object[] substitutions, boolean translate) {
		return inner.getString(key, substitutions, translate);
	}

	public String getString(String key, Object[] substitutions) {
		return inner.getString(key, substitutions);
	}

	public String getString(String key) {
		return inner.getString(key);
	}

	public Notifier getTarget() {
		return inner.getTarget();
	}

	public String getUpdateableText(Object object) {
		return inner.getUpdateableText(object);
	}

	public boolean isAdapterForType(Object type) {
		return inner.isAdapterForType(type);
	}

	public boolean isPropertySet(Object object, String property) {
		return inner.isPropertySet(object, property);
	}

	public void removeListener(INotifyChangedListener listener) {
		inner.removeListener(listener);
	}

	public void resetPropertyValue(Object object, String property) {
		inner.resetPropertyValue(object, property);
	}

	public void setPropertyValue(Object object, String property, Object value) {
		inner.setPropertyValue(object, property, value);
	}

	public void setTarget(Notifier target) {
		inner.setTarget(target);
	}

	public void unsetTarget(Notifier target) {
		inner.unsetTarget(target);
	}

	public IEObjectValidator getValidator() {
		return validator;
	}

	public void setValidator(IEObjectValidator validator) {
		this.validator = validator;
	}

}
