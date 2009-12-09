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

Add this tomcat.conf (for linux)

   export MARY_BASE="/usr/share/MARYTTS"
   export SHPROT_BASE="$MARY_BASE/lib/modules/shprot"
   export LD_LIBRARY_PATH="$MARY_BASE/lib/native:$LD_LIBRARY_PATH"

For windows you can move the mary (mp3) dll's to windows/system32
from $maryhome/lib/windows
(lametritonus.dll and lame_enc.dll )

Open Mary
set server=false in $MARY_HOME/conf/marybase.conf