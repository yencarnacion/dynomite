package dynomite

import static org.junit.Assert.*
import org.junit.*

class TestDataSourceCreationTests {

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testSomething() {
        def nickname = "yamir"

        def new_datasource =
                {
                    dataSourceId = "yamir"
                    driverClassName = "com.mysql.jdbc.Driver"
                    url = "jdbc:mysql://localhost:10000/d7d184d29f686418a9dcb76ebf409c68b"
                    username = "ujOgu7OBCwBDn"
                    password = "pQVTmxkI88kxa"
                    readOnly = Boolean.TRUE
                }

        try {
            dataSource.addDataSource(nickname, new_datasource)
            log.debug("Created ${nickname}!")
        } catch (Exception e) {
            // TODO add errors
            // dataSourceInstance.errors = ...
            //render(view: "create", model: [dataSourceInstance: dataSourceInstance])
            log.debug(e)
            log.debug("Error: "+e.message())
        }
    }
}
