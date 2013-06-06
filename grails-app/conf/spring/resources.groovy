beans = {

    dynamicDataSourceAdapter(org.grails.plugins.dynomite.impl.DynamicDataSourceAdapter){
        myTestProperty = ref('grailsApplication')
    }
}