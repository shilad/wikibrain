package org.wikapidia.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Binds together providers for a collection of components. A component is uniquely
 * identified by two elements:
 *
 * 1. A superclass or interface (e.g. LocalPageDao).
 * 2. A name (e.g. 'foo').
 *
 * So there can be multiple instances of the same component as long as it is uniquely named.
 *
 * The configurator Requires a top-level configuration element called "providers" that is a
 * dictionary. The key of a dictionary will be a generic "path" for a component, and the value
 * will be a list of fully qualified class names for providers for components along that path.
 * This configurator will look for its configuration along the path that is the dictionary
 * key of the 'providers' element. Each provider must be a concrete implementation of the
 * abstract Provider class.
 *
 * For example, some section of the config file may look like:
 *
 *      ... some top-level elements...
 *
 *      'providers' : {
 *          'dao.localPage' : [
 *              'org.wikapidia.dao.SqlLocalPageDao',
 *              'org.wikapidia.dao.MemoryLocalPageDao'
 *          ]
 *          ... other providers...
 *      }
 *
 * Note that all providers in a list must return the exact same class, which must be
 * the same class requested by clients.
 *
 * The actual configuration specification for the components follows at some point:
 *
 *      ... some more top-level elements...
 *
 *      'dao' :
 *          'localPage' : {
 *              'foo' : { ... config params for foo impl ... },
 *              'bar' : { ... config params for bar impl ... },
 *          }
 *
 * If a client requests the local page dao named 'foo', the configurator iterates
 * through all dao.localPage providers pass '{ ... config params for foo .. }' until
 * a provider accepts and generates the requested dao.
 *
 * All generated components are considered singletons. Once a named component is
 * generated once, it is cached and reused for future requests.
 */
public class Configurator {
    private static final Logger LOG = Logger.getLogger(Configurator.class.getName());

    public static String PROVIDER_PATH = "providers";

    private final Configuration conf;

    /**
     * A collection of providers for a particular type of component (e.g. LocalPageDao).
     */
    private class ProviderSet {
        Class klass = null;         // component's superclass or interface
        String path;                // path for config elements of the components
        List<Provider> providers;   // list of providers for the component.

        ProviderSet(String path) {
            this.path = path;
            this.providers = new ArrayList<Provider>();
        }
    }

    /**
     * Providers for each component.
     */
    private final Map<Class, ProviderSet> providers = new HashMap<Class, ProviderSet>();

    /**
     * Named instances of each component.
     */
    private final Map<Class, Map<String, Object>> components = new HashMap<Class, Map<String, Object>>();

    /**
     * Constructs a new configuration object with the specified configuration.
     * @param conf
     */
    public Configurator(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        searchForProviders(null);
    }

    /**
     * Performs a depth-first search for providers under a particular path.
     * @param path
     * @throws ConfigurationException
     */
    private void searchForProviders(String path) throws ConfigurationException {
        String fullPath = (path == null)
                ? (PROVIDER_PATH)
                : (PROVIDER_PATH + "." + path);
        ConfigValue value = conf.get().getValue(fullPath);

        if (value instanceof ConfigList) {              // a list of providers
            registerProvidersForComponent(path);
        } else if (value instanceof ConfigObject) {     // a nested dictionary, so recurse
            for (String key : ((ConfigObject)value).keySet()) {
                searchForProviders(path == null ? key : (path + "." + key));
            }
        } else {
            throw new ConfigurationException("Encountered unexpected type while walking providers at " + path + ": " + value);
        }
    }

    /**
     * Instantiates providers for the component.
     * @param componentPath
     * @return
     * @throws ConfigurationException
     */
    private void registerProvidersForComponent(String componentPath) throws ConfigurationException {
        ProviderSet pset = new ProviderSet(componentPath);
        for (String providerClass : conf.get().getStringList(PROVIDER_PATH + "." + componentPath)) {
            try {
                Class<Provider> klass = (Class<Provider>) Class.forName(providerClass);
                Provider provider = ConstructorUtils.invokeConstructor(klass, this, conf);
                if (pset.klass == null) pset.klass = provider.getType();
                if (!pset.klass.equals(provider.getType())) {
                    throw new ConfigurationException(
                            "inconsistent component types declared for " + componentPath +
                                    " for provider " + klass +
                                    " expected component type " + pset.klass +
                                    ", found component type " + provider.getType());
                }
                pset.providers.add(provider);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("error when loading provider for " + componentPath, e);
            } catch (InvocationTargetException e) {
                throw new ConfigurationException("error when loading provider for " + componentPath, e);
            } catch (NoSuchMethodException e) {
                throw new ConfigurationException("error when loading provider for " + componentPath, e);
            } catch (InstantiationException e) {
                throw new ConfigurationException("error when loading provider for " + componentPath, e);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("error when loading provider for " + componentPath, e);
            }
        }
        providers.put(pset.klass, pset);
        components.put(pset.klass, new HashMap<String, Object>());
        LOG.info("installed " + pset.providers.size() + " configurators for " + pset.klass);
    }

    /**
     * Get a specific named instance of the component with the specified class.
     * @param klass The generic interface or superclass, not the specific implementation.
     * @param name The name of the class as it appears in the config file.
     * @return The requested component.
     */
    public Object get(Class klass, String name) throws ConfigurationException {
        if (!providers.containsKey(klass)) {
            throw new ConfigurationException("No registered providers for components with class " + klass);
        }
        ProviderSet pset = providers.get(klass);
        String path = pset.path + "." + name;
        if (!conf.get().hasPath(path)) {
            throw new ConfigurationException("Configuration path " + path + " does not exist");
        }
        Config config = conf.get().getConfig(path);
        Map<String, Object> cache = components.get(klass);
        synchronized (cache) {
            if (cache.containsKey(name)) {
                return cache.get(name);
            }
            for (Provider p : pset.providers) {
                Object o = p.get(name, klass, config);
                if (o != null) {
                    cache.put(name, o);
                    return o;
                }
            }
        }
        throw new ConfigurationException(
                "None of the " + pset.providers.size() + " providers claimed ownership of component " +
                "with class " + klass +
                ", name '" + name +
                "' and configuration path '" + path + "'"
        );
    }

    /**
     * Get a specific named instance of the component with the specified class.
     * This method can only be used when there is exactly one provider, and one
     * instance of the component.
     *
     * @param klass The generic interface or superclass, not the specific implementation.
     * @return The requested component.
     */
    public Object get(Class klass) throws ConfigurationException {
        if (!providers.containsKey(klass)) {
            throw new ConfigurationException("No registered providers for components with class " + klass);
        }
        ProviderSet pset = providers.get(klass);
        if (!conf.get().hasPath(pset.path)) {
            throw new ConfigurationException("Configuration path " + pset.path + " does not exist");
        }
        Config config = conf.get().getConfig(pset.path);
        Map<String, Object> cache = components.get(klass);
        synchronized (cache) {
            if (cache.containsKey("")) {
                return cache.get("");
            }
            for (Provider p : pset.providers) {
                Object o = p.get("", klass, config);
                if (o != null) {
                    cache.put("", o);
                    return o;
                }
            }
        }
        throw new ConfigurationException(
                "None of the " + pset.providers.size() + " providers claimed ownership of component " +
                        "with class " + klass + " no name, " +
                        "' and configuration path '" + pset.path + "'"
        );
    }
}
