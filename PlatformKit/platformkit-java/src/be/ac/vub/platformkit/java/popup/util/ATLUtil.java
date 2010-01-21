package be.ac.vub.platformkit.java.popup.util;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IExtractor;
import org.eclipse.m2m.atl.core.IInjector;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.ModelFactory;
import org.eclipse.m2m.atl.core.launch.ILauncher;
import org.eclipse.m2m.atl.core.service.CoreService;

/**
 * Utility class for handling the new 3.0 ATL VM.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ATLUtil {
	
	private String atlVMName;
    private ModelFactory factory;
    private IInjector injector;
    private IExtractor extractor;

    /**
     * Creates a new ATLUtil instance for the given ATL VM
     * @param atlVMName The ATL VM name ("Regular VM (with debugger)" or "EMF-specific VM")
     * @throws ATLCoreException
     */
	public ATLUtil(String atlVMName) throws ATLCoreException {
		Assert.assertNotNull(atlVMName);
		this.atlVMName = atlVMName;
		factory = CoreService.getModelFactory(getLauncher().getDefaultModelFactoryName());
		injector = CoreService.getInjector(factory.getDefaultInjectorName());
		extractor = CoreService.getExtractor(factory.getDefaultExtractorName());
	}

	/**
	 * Loads a model from an input stream.
	 * @param refModel The meta-model.
	 * @param source The input stream to the model.
	 * @param name The model name in the transformation module.
	 * @param path The (descriptive) path to the model (file extension matters most here).
	 * @return The loaded model.
	 * @throws ATLCoreException
	 */
	public IModel loadModel(IReferenceModel refModel, InputStream source,
			String name, String path) throws ATLCoreException {
		final Map<String, Object> options = new HashMap<String, Object>();
		options.put("modelName", name);
		options.put("path", path);
		options.put("newModel", "false");
		final IModel model = getFactory().newModel(refModel, options);
		getInjector().inject(model, source, options);
		return model;
	}
    
	/**
	 * Loads a model from an input stream.
	 * @param refModel The meta-model.
	 * @param source The path to the model.
	 * @param name The model name in the transformation module.
	 * @param path The (descriptive) path to the model (file extension matters most here).
	 * @return The loaded model.
	 * @throws ATLCoreException
	 */
	public IModel loadModel(IReferenceModel refModel, String source,
			String name, String path) throws ATLCoreException {
		final Map<String, Object> options = new HashMap<String, Object>();
		options.put("modelName", name);
		options.put("path", path);
		options.put("newModel", "false");
		final IModel model = getFactory().newModel(refModel, options);
		getInjector().inject(model, source, options);
		return model;
	}
    
	/**
	 * Loads a model from an input stream.
	 * @param refModel The meta-model.
	 * @param resource The resource containing the model.
	 * @param name The model name in the transformation module.
	 * @return The loaded model.
	 * @throws ATLCoreException
	 */
	public IModel loadModel(IReferenceModel refModel, Resource resource,
			String name) throws ATLCoreException {
		final Map<String, Object> options = new HashMap<String, Object>();
		options.put("modelName", name);
		options.put("path", resource.getURI().toString());
		options.put("newModel", "false");
		final IModel model = getFactory().newModel(refModel, options);
		final IInjector injector = getInjector();
		try {
			Method inject = injector.getClass().getMethod("inject", IModel.class, Resource.class);
			inject.invoke(injector, model, resource);
		} catch (Exception e) {
			throw new ATLCoreException("Injector does not support loading from EMF Resource", e);
		}
		return model;
	}
    
	/**
	 * Loads a meta-model from an input stream.
	 * @param source The input stream to the meta-model.
	 * @param name The meta-model name in the transformation module.
	 * @param path The (descriptive) path to the meta-model (file extension matters most here).
	 * @param handler The model handler ("MDR", "EMF", or "UML2")
	 * @return The loaded meta-model.
	 * @throws ATLCoreException
	 */
	public IReferenceModel loadRefModel(InputStream source,
			String name, String path, String handler) throws ATLCoreException {
		final Map<String, Object> options = new HashMap<String, Object>();
		options.put("modelHandlerName", handler);
		options.put("modelName", name);
		options.put("path", path);
		options.put("newModel", "false");
		final IReferenceModel model = getFactory().newReferenceModel(options);
		getInjector().inject(model, source, options);
		return model;
	}
    
	/**
	 * Creates a new model.
	 * @param refModel The meta-model.
	 * @param name The model name in the transformation module.
	 * @param path The (descriptive) path to the model (file extension matters most here).
	 * @return The new model.
	 * @throws ATLCoreException
	 */
	public IModel newModel(IReferenceModel refModel,
			String name, String path) throws ATLCoreException {
		final Map<String, Object> options = new HashMap<String, Object>();
		options.put("modelName", name);
		options.put("path", path);
		options.put("newModel", "true");
		final IModel model = getFactory().newModel(refModel, options);
		return model;
	}
	
	/**
	 * @return a new launcher
	 * @throws ATLCoreException 
	 */
	public ILauncher getLauncher() throws ATLCoreException {
		final ILauncher launcher = CoreService.getLauncher(atlVMName);
		if (launcher == null) {
			throw new ATLCoreException("ATL VM \"" + atlVMName + "\" not found");
		}
		final Map<String,Object> options = Collections.emptyMap();
		launcher.initialize(options);
		return launcher;
	}

	/**
	 * @return the factory
	 */
	public ModelFactory getFactory() {
		return factory;
	}

	/**
	 * @return the injector
	 */
	public IInjector getInjector() {
		return injector;
	}

	/**
	 * @return the extractor
	 */
	public IExtractor getExtractor() {
		return extractor;
	}
	
	/**
	 * Converts an ATL VM value to a boolean, if possible
	 * @param value
	 * @return the converted value
	 * @throws ClassCastException if the value was not a boolean
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static boolean getBooleanValue(Object value) throws ClassCastException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Assert.assertNotNull(value);
		if ("org.eclipse.m2m.atl.engine.vm.nativelib.ASMBoolean".equals(value.getClass().getName())) {
			final Method getSymbol = value.getClass().getDeclaredMethod("getSymbol");
			value = (Boolean)getSymbol.invoke(value);
		}
		return ((Boolean)value).booleanValue();
	}
    
}
