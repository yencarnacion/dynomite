/* Copyright 2013 Yamir Encarnacion <yencarnacion@webninjapr.com> */
/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.plugins.dynomite.impl.BasicDataSourceFactory
import org.grails.plugins.dynomite.impl.DefaultDatabaseSchemaImporter
import org.grails.plugins.dynomite.impl.DynamicDataSourceAdapter
import org.hibernate.Session
import org.hibernate.dialect.Dialect
import org.hibernate.dialect.resolver.DialectResolver
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.support.DatabaseMetaDataCallback
import org.springframework.jdbc.support.JdbcUtils
import org.springframework.jdbc.support.MetaDataAccessException

import java.sql.DatabaseMetaData
import java.sql.SQLException

class DynomiteGrailsPlugin {

    //def grailsApplication = application
    //grailsApplication

    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Dynomite Plugin" // Headline display name of the plugin
    def author = "Yamir Encarnacion"
    def authorEmail = "yencarnacion@webninjapr.com"
    def description = '''\
Update to the DynamicDataSources plugin so that it can be used to dynamically add/remove a datasource to a running grails 2.x app.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/dynomite"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def dependsOn = ["hibernate":"2.2 > *"]

    def loadAfter = ['hibernate']


    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {

        dataSourceFactory(BasicDataSourceFactory)
        schemaImporter(DefaultDatabaseSchemaImporter)
        dataSource(DynamicDataSourceAdapter) {
            dataSourceFactory = dataSourceFactory
            schemaImporter = schemaImporter
        }

        // Alter the hibernate configuration to enable DynamicDataSources aware 2nd level caching.
        // We have to do it in 'doWithSpring' because this is the only hook that
        // is called before the SessionFactory gets initialized.
        def hibernate = application.config.hibernate
        String traditionalCacheProvider = hibernate.cache.provider_class
        hibernate.cache.provider_class = 'org.grails.plugins.dynomite.hibernate.cache.DynamicDataSourceCacheProvider'
        hibernate.cache.traditional.provider_class = traditionalCacheProvider

    }


    // we need a domainClass to call a withSession and withTransaction within the
    // withDataSource-Closure
    def aDomainClass

    /**
     * After a call to withDataSource from a controller, domainClass oder service
     * the currentSession will be cleared!
     */
    def withDataSource = { String id, Closure block ->
        DynamicDataSourceAdapter.withDataSource(id) {
            executeSecondLevelCacheAware {
                executeFirstLevelCacheAware {
                    executeDynamicDataSourceAware {
                        block()
                    }
                }
            }
        }
    }


    private def executeSecondLevelCacheAware(Closure block) {

        // The first approach is to evict all cached data
//        grailsApplication.domainClasses.each {
//            sessionFactory.evict(it.clazz)
//        }
        // when 2nd level cache is enabled, it would only work for this
        // little block. So 2nd level caching would effectively be disabled!
        // That's not that good.

        // Currently the 2nd level cache awareness is provided by replacing the
        // configured cache provider class with a DynamicDataSourceCacheProvider
        // wrapping the configured one.
        // This CacheProvider builds DynamicDataSourceCache instances on demand per dataSource.
        block()
    }


    private def executeFirstLevelCacheAware(Closure block) {

        if (!aDomainClass) {
            return block()
        }

        aDomainClass.withSession { Session session ->
            try {
                // We must empty the first level cache because
                // we do not know if there were persistence operations
                // on other dataSources within the same session.
                // So this avoids wrong first level caching for this 'block'.
                session.clear()

                return block()

            } finally {
                // We must empty the first level cache again because
                // we do not know if there will be further persistence operations
                // on other dataSources within the same session.
                // So this avoids wrong first level caching for upcoming operations.
                session.clear()
            }
        }
    }


    private def executeDynamicDataSourceAware(Closure block) {

        if (!aDomainClass) {
            return block()
        }

        // We need a transaction here because this is the only way to
        // let hibernate call the dataSource.getTargetDataSource()
        // and get the right dataSource instance.
        // Basically we just need to ensure that the transaction is setup
        // AFTER the right dataSource is in place, that is
        // AFTER DynamicDataSourceAdapter.set(dataSourceId) is called.
        aDomainClass.withTransaction {
            block()
        }
    }


    def doWithDynamicMethods = { ctx ->


        final def domainClasses = application.domainClasses
        if (domainClasses) {
            aDomainClass = domainClasses.iterator().next().clazz
            log.debug("**YAMIR**\n>>>"+aDomainClass.dump()+"\n<<<")
        }
        application.controllerClasses.each { controllerClass ->
            controllerClass.clazz.metaClass.withDataSource = withDataSource
            controllerClass.clazz.metaClass.static.withDataSource = withDataSource
        }
        application.serviceClasses.each { serviceClass ->
            serviceClass.clazz.metaClass.withDataSource = withDataSource
            serviceClass.clazz.metaClass.static.withDataSource = withDataSource
        }
        application.domainClasses.each { domainClass ->
            domainClass.clazz.metaClass.withDataSource = withDataSource
            domainClass.clazz.metaClass.static.withDataSource = withDataSource
        }

    }

    def doWithApplicationContext = { applicationContext ->
        // post initialization spring config (optional)
        String ddlSQL = schemaExport()
        def schemaImporter = applicationContext.getBean("schemaImporter")
        schemaImporter.init(ddlSQL)
    }

    def observe = ["domainClass", "services", "controllers"]

    def onChange = { event ->
        // Code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
        println "[Dynomite-Plugin] Source changed"

        def changedSource = event.source

        if (!aDomainClass && "Domain" == event.application.getArtefactType(changedSource)?.type) {
            // now we have a domain class
            aDomainClass = changedSource
        }

        // re-inject the withDataSource closure
        def mc = changedSource.metaClass
        mc.withDataSource = withDataSource

    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }


    // TODO Extract Class
    private String schemaExport() {

        //depends(packageApp)

        //configureFromArgs()

        File schemaFile = new File("ddl.sql")

        if (!schemaFile.createNewFile()) {
            schemaFile.text = ""
        }

//        configClasspath()
//        loadApp()
        Properties props = new Properties()
        populateProperties(props)

        def configuration = new GrailsAnnotationConfiguration()
        configuration.setGrailsApplication(application)
        configuration.setProperties(props)

//        def hibernateCfgXml = eventsClassLoader.getResource('hibernate/hibernate.cfg.xml')
//        if (hibernateCfgXml) {
//            configuration.configure(hibernateCfgXml)
//        }

        def schemaExport = new org.hibernate.tool.hbm2ddl.SchemaExport(configuration).setHaltOnError(true).setOutputFile(schemaFile.path).setDelimiter(';')

//        def action = export ? "Exporting" : "Generating script to ${file.path}"
//        println "${action} in environment '${grailsEnv}' using properties ${props}"

        boolean stdout = false
//        if (export) {
        // 1st drop, warning exceptions
        schemaExport.execute(stdout, true, true, false)
        schemaExport.exceptions.clear()
        // then create
        schemaExport.execute(stdout, true, false, true)
//        }
//        else {
//            // generate
//            schemaExport.execute(stdout, false, false, false)
//        }

        if (!schemaExport.exceptions.empty) {
            schemaExport.exceptions[0].printStackTrace()
        }

        return schemaFile.text
    }

    /*private Dialect resolveDialect(DataSource dataSource) {
       try {

           return (Dialect)JdbcUtils.extractDatabaseMetaData(dataSource, new DatabaseMetaDataCallback() {
               @Override
               public Object processMetaData(DatabaseMetaData dbmd) throws SQLException,
                       MetaDataAccessException {
                   //return dialectResolver.resolveDialect(dbmd);
                   return "";
               }
           });
       } catch (MetaDataAccessException e) {
           log.warn("Failed to resolve Dialect for DataSource " + dataSource + " no validation query will be resolved", e);
           return null;
       }
   }*/


    private void populateProperties(Properties props) {

        def dsConfig = application.config

        props.'hibernate.connection.username' = dsConfig?.dataSource?.username ?: 'sa'
        props.'hibernate.connection.password' = dsConfig?.dataSource?.password ?: ''
        props.'hibernate.connection.url' = dsConfig?.dataSource?.url ?: 'jdbc:hsqldb:mem:testDB'
        props.'hibernate.connection.driver_class' =
                dsConfig?.dataSource?.driverClassName ?: 'org.hsqldb.jdbcDriver'

        if (dsConfig?.dataSource?.configClass) {
            if (dsConfig.dataSource.configClass instanceof Class) {
                configClassName = dsConfig.dataSource.configClass.name
            }
            else {
                configClassName = dsConfig.dataSource.configClass
            }
        }

        def namingStrategy = dsConfig?.hibernate?.naming_strategy
        if (namingStrategy) {
            try {
                GrailsDomainBinder.configureNamingStrategy namingStrategy
            }
            catch (Throwable t) {
                println """WARNING: You've configured a custom Hibernate naming strategy '$namingStrategy' in DataSource.groovy, however the class cannot be found.
    Using Grails' default naming strategy: '${GrailsDomainBinder.namingStrategy.getClass().name}'"""
            }
        }

        if (dsConfig?.dataSource?.dialect) {
            def dialect = dsConfig.dataSource.dialect
            if (dialect instanceof Class) {
                dialect = dialect.name
            }
            props.'hibernate.dialect' = dialect
        }
        else {
            println('WARNING: Autodetecting the Hibernate Dialect; consider specifying the class name in DataSource.groovy')
            try {
                def ds = new DriverManagerDataSource(
                        props.'hibernate.connection.driver_class',
                        props.'hibernate.connection.url',
                        props.'hibernate.connection.username',
                        props.'hibernate.connection.password')
                //def dbName = JdbcUtils.extractDatabaseMetaData(ds, 'getDatabaseProductName')
                // def majorVersion = JdbcUtils.extractDatabaseMetaData(ds, 'getDatabaseMajorVersion')


                props.'hibernate.dialect' =  DynomiteUtils.resolveDialect(ds).class.name;
                /*
                //DialectResolver.resolveDialect determineDialect(dbName, majorVersion).class.name
                */
            }
            catch (Exception e) {
                println "ERROR: Problem autodetecting the Hibernate Dialect: ${e.message}"
                throw e
            }
        }

    }
    /*







        else {
            println('WARNING: Autodetecting the Hibernate Dialect; consider specifying the class name in DataSource.groovy')
            try {
                def ds = new DriverManagerDataSource(
                        props.'hibernate.connection.driver_class',
                        props.'hibernate.connection.url',
                        props.'hibernate.connection.username',
                        props.'hibernate.connection.password')
                //def dbName = JdbcUtils.extractDatabaseMetaData(ds, 'getDatabaseProductName')
                // def majorVersion = JdbcUtils.extractDatabaseMetaData(ds, 'getDatabaseMajorVersion')
                props.'hibernate.dialect' =
                        (Dialect)JdbcUtils.extractDatabaseMetaData(ds, new DatabaseMetaDataCallback() {
                            @Override
                            public Object processMetaData(DatabaseMetaData dbmd) throws SQLException,
                                    MetaDataAccessException {
                                return DialectResolver.resolveDialect(dbmd);
                            }
                        }).class.name;
                //DialectResolver.resolveDialect determineDialect(dbName, majorVersion).class.name
            }
            catch (Exception e) {
                println "ERROR: Problem autodetecting the Hibernate Dialect: ${e.message}"
                throw e
            }
        }
    }
    */

    /*

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }*/
}
