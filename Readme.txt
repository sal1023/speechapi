* To run in servlet container
    * set JAXWS_HOME to the JAX-WS installation directory
    * ant clean server - runs apt to generate server side artifacts and
      does the deployment
    * ant clean client run - runs wsimport on the published wsdl by the deplyed
      endpoint, compiles the generated artifacts and the client application
      then executes it.

download jaxws ri
https://jax-ws.dev.java.net/

Tomcat
Add this to CATALINA_HOME/conf/catalina.properties
shared.loader=c:/tools/jaxws-ri/lib/*.jar
