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

import org.hibernate.cache.Cache
import org.hibernate.cache.CacheProvider
import org.grails.plugins.dynomite.impl.DynamicDataSourceAdapter

/**
 * The DynamicDataSourceCache holds a map of DataSourceIds to Cache instances.
 * The Cache instances are build on demand, dependant on the currents Threads dataSourceName.
 *
 * @author Armin Weisser
 *
 */
class DynamicDataSourceCache implements Cache {

    // CacheProvider to build a traditional Cache on demand
    CacheProvider traditionalCacheProvider

    // The regionName to build caches with
    String regionName

    // The properties to build caches with
    Properties properties

    // Maps DynamicDataSource Ids to Cache instances.
    Map<String, Cache> dataSourceCacheMap

    DynamicDataSourceCache(CacheProvider traditionalCacheProvider, String regionName, Properties properties) {
        this.traditionalCacheProvider = traditionalCacheProvider
        this.regionName = regionName
        this.properties = properties
        dataSourceCacheMap = [:]
    }

    private Cache getCache() {
        String dataSourceId = getCurrentDataSourceId()
        Cache cache = dataSourceCacheMap.get(dataSourceId)
        if(!cache) {
            cache = buildCache()
            dataSourceCacheMap.put(dataSourceId, cache)
        }
        return cache
    }

    private String getCurrentDataSourceId() {
        DynamicDataSourceAdapter.getSourceNameNotNull()
    }

    private Cache buildCache() {
        traditionalCacheProvider.buildCache(regionName, properties)
    }

    Object read(Object key) {
        return cache.read(key)
    }

    Object get(Object key) {
        return cache.get(key)
    }

    void put(Object key, Object value) {
        cache.put(key, value)
    }

    void update(Object key, Object value) {
        cache.update(key, value)
    }

    void remove(Object key) {
        cache.remove(key)
    }

    void clear() {
        cache.clear()
    }

    void destroy() {
        // TODO Also remove cache from map?
        cache.destroy()
    }

    void lock(Object key) {
        cache.lock(key)
    }

    void unlock(Object key) {
        cache.unlock(key)
    }

    long nextTimestamp() {
        return cache.nextTimestamp();
    }

    int getTimeout() {
        return cache.getTimeout();
    }

    String getRegionName() {
        return cache.getRegionName();
    }

    long getSizeInMemory() {
        return cache.getSizeInMemory();
    }

    long getElementCountInMemory() {
        return cache.getElementCountInMemory();
    }

    long getElementCountOnDisk() {
        return cache.getElementCountOnDisk();
    }

    Map toMap() {
        return cache.toMap();
    }

}
