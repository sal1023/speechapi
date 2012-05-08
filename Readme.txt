SpeechServer Installation 
-------------------------

1.  install jdk  (This release  was tested with the jdk1.6.0.22 )
2.  install tomcat (This release was tested with version 6.0.29)
3.  Unzip the speechapi.zip (or tar.gz) 
4.  You will need the following environment var 

Example for unix install (note your paths may be different depending on where
you placed the various components on the file system.)

    #  tomcat will require JAVA_HOME set to home dir for jdk (not jre)
       export JAVA_HOME=/usr/share/jdk1.6.0_22
       export PATH=$JAVA_HOME/bin:$PATH

    #  *** NOTE that speechapi system property must point to location that you unziped speechapi.zip/tar
       export CATALINA_OPTS="-Xms512m -Xmx1024m -Dspeechapi=/usr/share/speechapi"

    #  For mp3 synthesis support on linux
       export MARY_BASE="$SPEECHAPI_HOME/MARY TTS"
       export LD_LIBRARY_PATH="$MARY_BASE/lib/linux:$LD_LIBRARY_PATH"

    #  for mp3 synthesis support on windows, you can move the mary (mp3) dll's to windows/system32 from $maryhome/lib/windows
    #     lametritonus.dll and lame_enc.dll

5.  Copy the speechcloud.war file to the tomcat webapps directory

6.  Now you can run tomcat the normal way.  
     $TOMCAT_HOME/bin/startup.sh (or .bat)
     $TOMCAT_HOME/bin/shutdown.sh (or .bat)

    Optionally you can use the init.d script to audtomatically startup tomcat.

    On Windows, if you are running Tomcat as a Windows Service, to set the options for the JVM (like setting CATALINA_OPTS or JAVA_OPTS) 
    use the Tomcat Service Manager. This is either a tray application, or you can run the manager directly via $CATALINA_HOME\bin\tomcat6w.exe, 
    set the options under the "Java" tab and hit "Apply" to apply them. Each options goes on it's own line. 


LIMITATIONS
-----------
- synthesis on 64bit windows not supported

- Only wav files support for recognition  To support other formats 
Install xuggler. (This release  was tested with version 3.4.1012)
And then check the following env variable
   #  xuggler installer may already set this for you.
       export XUGGLE_HOME=/usr/local/xuggler
       export LD_LIBRARY_PATH=$XUGGLE_HOME/lib:$LD_LIBRARY_PATH
       export PATH=$XUGGLE_HOME/bin:$PATH


