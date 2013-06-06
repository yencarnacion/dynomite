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

import javax.sql.DataSource
import org.springframework.util.Assert
import org.grails.plugins.dynomite.IDataSourceFactory

/**
 * Given a Map of named properties, the factory creates new BasicDataSource instances.
 * This is the default implementation of the IDataSourceFactory interface.
 *
 * @author Armin Weisser
 *
 */
class BasicDataSourceFactory implements IDataSourceFactory {


    public DataSource createDataSource(DataSource dataSource) {
        return dataSource
    }

    public DataSource createDataSource(def config) {
        Assert.notEmpty(config, "'config must not be empty")
        org.apache.commons.dbcp.BasicDataSourceFactory.createDataSource(config as Properties)
    }


}
