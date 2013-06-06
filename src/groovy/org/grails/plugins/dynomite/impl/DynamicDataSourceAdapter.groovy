/* Copyright 2013 Yamir Encarnacion <yencarnacion@webninjapr.com> */
/******************************************************************************
 * Copyright 2010 the original author or authors.                             *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *       http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package org.grails.plugins.dynomite.impl

import dynomite.Dummy
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA

import javax.sql.DataSource
import org.apache.commons.dbcp.BasicDataSource
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.util.Assert
import org.grails.plugins.dynomite.IDataSourceFactory
import org.grails.plugins.dynomite.IDatabaseSchemaImporter

/**
 * The class adapts the DelegatingDataSource and acts as a
 * registry for DataSources.
 *
 * DataSources can be registered with an id.
 * To add a new DataSource to the registry just pass the
 * necessary configuration or a new DataSource instance to the add methods.
 *
 * You can use a registered dataSource by calling 'withDataSource(String id, Closure block)'.
 *
 * The default DataSource (as configured in DataSource.groovy) is automatically
 * registered and accessible under the id "default".
 * (Better use the constant DynamicDataSourceAdapter.DEFAULT_DATASOURCE_ID.)
 *
 * The default dataSource is still used when no withDataSource block is used.
 * so am simple <br>
 * Element.list()<br>
 * and<br>
 * withDataSource('default') {<br>
 *  Element.list()<br>
 * }<br>
 * lists all 'Elements' in the default dataSource configured in DataSource.groovy.
 *
 * @author Cédric Champeau, Armin Weisser
 *
 */
class DynamicDataSourceAdapter extends DelegatingDataSource {

    def myTestProperty //= new Dummy().domainClass.grailsApplication

    public final static String DEFAULT_DATASOURCE_ID = 'default'

    private final static ThreadLocal<String> sourceName = new ThreadLocal<String>();

    public static String getSourceName() {
        return sourceName.get();
    }

    public static void setSourceName(final String name) {
        sourceName.set(name);
    }

    public static String getSourceNameNotNull() {
        getSourceName() ?: DEFAULT_DATASOURCE_ID
    }

    /**
     * Sets up the DataSource identified by 'id' and calls the 'block' closure.
     *
     * Note that a direct call to this is not recommanded.
     * If you do so you must ensure that a new transaction
     * is setup within the 'block' closure.
     * Furthermore you'll have to deal with the hibernate caches
     * yourself.
     *
     * Better use the dynamic method with the same signature accessible in
     * all domain-, controller- and service-classes.
     *
     * @param id
     * @param block
     * @return the result of the 'block' execution
     */
    public static def withDataSource(String id, Closure block) {
        def current = getSourceName()
        try {
            setSourceName id
            return block()
        } finally {
            setSourceName current
        }
    }

    /**
     * these will be injected by Spring
     */
    IDataSourceFactory dataSourceFactory

    IDatabaseSchemaImporter schemaImporter


    /**
     * The registered DataSources to delegate to
     */
    private Map<String, DataSource> delegates;


    /**
     * Ctor
     * @return
     */
    public DynamicDataSourceAdapter() {
        delegates = [:]
    }


    /**
     * Initializes the default Grails DataSource from DataSource.groovy
     * as the current target DataSource
     */
    public def void afterPropertiesSet() {
        setTargetDataSource(defaultGrailsDataSource)
        addDataSource(DEFAULT_DATASOURCE_ID, getTargetDataSource())
    }

    private BasicDataSource getDefaultGrailsDataSource() {
        def config = grailsApplication.config.get("dataSource");
        final BasicDataSource dataSource = getDataSourceInstance(config)
        return dataSource
    }


    @Override
    public DataSource getTargetDataSource() {
        String name = getSourceName();
        DataSource source
        if(!name) {
            // If no name was set fall back to the default DataSource
            source = super.getTargetDataSource();
        }
        else {
            source = delegates.get(name);
            if (!source) {
                // If a name is set, and still no dataSource can be found
                // than the system is not properly configured.
                throw new NoSuchDataSourceException(name);
            }
        }
        return source;
    }


    /**
     * Adds a new DataSource to the registry.
     * A previously registered DataSource with this id is beeing replaced!
     *
     * @param id a unique identifier for the DataSource
     * @param config all properties necessary to create a new DataSource or an instance of DataSource
     * @return
     */
    public def addDataSource(final String id, def config) {
        Assert.notNull(id, "'id' must not be null")
        Assert.notNull(config, "'config' must not be null")
        BasicDataSource dataSource = getDataSourceInstance(config)

        if(id!=DEFAULT_DATASOURCE_ID) {
            schemaImporter.createTables(dataSource)
        }

        delegates.put(id, dataSource);
    }

    private BasicDataSource getDataSourceInstance(def config) {
        dataSourceFactory.createDataSource(config)
    }


    /**
     *
     * @param id
     */
    public void removeDataSource(final String id) {
        delegates.remove(id);
    }

    /**
     *
     * @return IDs of all registered DataSources
     */
    public List<String> getAvailableDataSourceKeys() {
        Collections.unmodifiableList(delegates.keySet().sort())
    }

    /**
     *
     * @return All registered DataSources instances
     */
    public Collection<DataSource> getAvailableDataSources() {
        Collections.unmodifiableCollection(delegates.values())
    }

    /**
     *
     * @param key
     * @return the DataSource instance available for the given key
     */
    public DataSource getDataSource(String key) {
        delegates.get(key)
    }

}
