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
package org.grails.plugins.dynomite.hibernate.cache


import org.hibernate.cache.CacheProvider
import org.hibernate.cache.Cache

import org.hibernate.cache.CacheException
import org.hibernate.util.ReflectHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.hibernate.cache.impl.bridge.RegionFactoryCacheProviderBridge

/**
 * DynamicDataSources aware CacheProvider implementation.
 * This one can be used for the legacy (and since 3.3 deprecated)
 * Hibernate CacheProvider approach as well as for the new RegionFactory-based approach
 * in conjunction with the RegionFactoryCacheProviderBridge.
 * (http://docs.jboss.org/hibernate/core/3.3/api/org/hibernate/cache/package-summary.html)
 *
 * Since there are no reasonable implementations for the RegionFactory interface we stick
 * with the deprecated CacheProvider approach.
 *
 * @author Armin Weisser
 *
 */
class DynamicDataSourceCacheProvider implements CacheProvider {

    def grailsApplication

    private static final Logger log = LoggerFactory.getLogger( DynamicDataSourceCacheProvider.class );

    private CacheProvider traditionalCacheProvider

    public DynamicDataSourceCacheProvider() {
        def config = grailsApplication
		String traditionalProviderClassName = config.hibernate?.cache?.traditional?.provider_class

        if(!traditionalProviderClassName) {
            traditionalProviderClassName = RegionFactoryCacheProviderBridge.DEF_PROVIDER
        }

        log.info( "Traditional Cache provider: " + traditionalProviderClassName );
		try {
			traditionalCacheProvider = ( CacheProvider ) ReflectHelper.classForName( traditionalProviderClassName ).newInstance();
		}
		catch ( Exception cnfe ) {
			throw new CacheException( "could not instantiate CacheProvider [" + traditionalProviderClassName + "]", cnfe );
		}
    }

    Cache buildCache(String regionName, Properties properties) {
        Cache dynamicDataSourceCache =
            new DynamicDataSourceCache(traditionalCacheProvider, regionName, properties)
        return dynamicDataSourceCache;
    }

    long nextTimestamp() {
        return traditionalCacheProvider.nextTimestamp();
    }

    void start(Properties properties) {
        traditionalCacheProvider.start(properties)
    }

    void stop() {
        traditionalCacheProvider.stop()
    }

    boolean isMinimalPutsEnabledByDefault() {
        return traditionalCacheProvider.isMinimalPutsEnabledByDefault();
    }
}
