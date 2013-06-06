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

import java.sql.SQLException
import java.sql.Statement
import java.sql.Connection
import javax.sql.DataSource
import org.springframework.util.Assert
import org.grails.plugins.dynomite.IDatabaseSchemaImporter

/**
 *
 * @author aweisser
 *
 */
class DefaultDatabaseSchemaImporter implements IDatabaseSchemaImporter {

    private List<String> createTableStatements

    public DefaultDatabaseSchemaImporter() {
        createTableStatements = []
    }

    public void init(String importScript) {
        initCreateTableStatements(importScript)
    }

    private void initCreateTableStatements(String importScript) {
        createTableStatements = []
        importScript.eachLine { line ->
            createTableStatements.add(line)
        }
    }

    public void createTables(DataSource ds) {
        Assert.state(createTableStatements?.size() > 0, "Please call init(String) before creating tables.")
        executeStatements(createTableStatements, ds)
    }

    private def executeStatements(List sqlStatements, DataSource ds) {
        Connection con
        try {
            con = ds.getConnection()
            assert con != null
            for (sql in sqlStatements) {
                try {
                    Statement query = con.createStatement()
                    query.execute(sql)
                } catch (SQLException e) {
                    // schlucken
                }
            }
        } catch (SQLException sqle) {
            // schlucken
        } finally {
            con?.close()
        }
    }

}



