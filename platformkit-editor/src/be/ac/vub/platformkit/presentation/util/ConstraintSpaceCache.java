package be.ac.vub.platformkit.presentation.util;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;

import be.ac.vub.platformkit.ConstraintSpace;

/**
 * Provides a modification date aware cache for {@link ConstraintSpace}s.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ConstraintSpaceCache {

	protected Map<Object, Reference<CacheEntry>> cache = new HashMap<Object, Reference<CacheEntry>>();

	/**
	 * @param key the key under which the cache entry is stored.
	 * @param timestamp the required timestamp of the cache entry.
	 * @return the {@link ConstraintSpace} from the cache.
	 */
	public ConstraintSpace get(Object key, long timestamp) {
		Reference<CacheEntry> ref = cache.get(key);
		if (ref != null) {
			CacheEntry e = cache.get(key).get();
			if ((e != null) && (e.getModificationTimestamp() == timestamp)) {
				return e.getSpace();
			}
		}
		return null;
	}

	/**
	 * Stores a {@link ConstraintSpace} in the cache.
	 * @param key the key under which the cache entry is stored.
	 * @param space the {@link ConstraintSpace} to store.
	 * @param timestamp the required timestamp of the cache entry.
	 */
	public void put(Object key, ConstraintSpace space, long timestamp) {
		CacheEntry entry = new CacheEntry();
		entry.setSpace(space);
		entry.setModificationTimestamp(timestamp);
		cache.put(key, new SoftReference<CacheEntry>(entry));
	}

    /**
     * @param resource The resource that is used as a cache entry key.
     * @return The cached {@link ConstraintSpace}, if available, null otherwise.
     */
    public ConstraintSpace get(IResource resource) {
        return get(resource, resource.getModificationStamp());
    }

    /**
     * @param resource The resource that is used as a cache entry key.
     * @param space The {@link ConstraintSpace} to add to the cache.
     */
    public void put(IResource resource, ConstraintSpace space) {
        put(resource, space, resource.getModificationStamp());
    }
    
    /**
     * @param resource The resource that is used as a cache entry key.
     * @return The cached constraint space, if available, null otherwise.
     */
    public ConstraintSpace get(URI resource) {
    	if (resource.isPlatformResource()) {
    		IPath resourcePath = new Path(resource.toPlatformString(true));
            IFile resourceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);
            Assert.isNotNull(resourceFile);
            return get(resourceFile);
    	} else {
    		return get(resource, IResource.NULL_STAMP);
    	}
    }

    /**
     * @param resource The resource that is used as a cache entry key.
     * @param space The {@link ConstraintSpace} to add to the cache.
     */
    public void put(URI resource, ConstraintSpace space) {
    	if (resource.isPlatformResource()) {
    		IPath resourcePath = new Path(resource.toPlatformString(true));
            IFile resourceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(resourcePath);
            Assert.isNotNull(resourceFile);
            put(resourceFile, space);
    	} else {
    		put(resource, space, IResource.NULL_STAMP);
    	}
    }
    
	/**
	 * Class for cached objects. 
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	public class CacheEntry {
		private ConstraintSpace space;
		private long modificationTimestamp;
		/**
		 * @return the space
		 */
		public ConstraintSpace getSpace() {
			return space;
		}
		/**
		 * @param space the space to set
		 */
		public void setSpace(ConstraintSpace space) {
			this.space = space;
		}
		/**
		 * @return the modificationTimestamp
		 */
		public long getModificationTimestamp() {
			return modificationTimestamp;
		}
		/**
		 * @param modificationTimestamp the modificationDate to set
		 */
		public void setModificationTimestamp(long modificationTimestamp) {
			this.modificationTimestamp = modificationTimestamp;
		}
	}

}
