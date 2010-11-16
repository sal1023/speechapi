SpeechServer Installation 
-------------------------

1.  install jdk  (This release  was tested with the jdk1.6.0.22 )
2.  install xuggler  (This release  was tested with version 3.4.1012)
3.  install tomcat (This release was tested with version 6.0.29)
4.  Unzip the speechapi.zip (or tar.gz) 
5.  You will need the following environment var 

Example for unix install (note your paths may be different depending on where
you placed the various components on the file system.)

    #  tomcat will require JAVA_HOME set to home dir for jdk (not jre)
       export JAVA_HOME=/usr/share/jdk1.6.0_22
       export PATH=$JAVA_HOME/bin:$PATH

    #  xuggler installer may already set this for you.
       export XUGGLE_HOME=/usr/local/xuggler
       export LD_LIBRARY_PATH=$XUGGLE_HOME/lib:$LD_LIBRARY_PATH
       export PATH=$XUGGLE_HOME/bin:$PATH

    #  *** NOTE that speechapi system property must point to location that you unziped speechapi.zip/tar
       export CATALINA_OPTS="-Xms512m -Xmx1024m -Dspeechapi=/usr/share/speechapi"

    #  For mp3 support on linux
       export MARY_BASE="$SPEECHAPI_HOME/MARY TTS"
       export LD_LIBRARY_PATH="$MARY_BASE/lib/linux:$LD_LIBRARY_PATH"

    #  for mp3 support on windows, you can move the mary (mp3) dll's to windows/system32 from $maryhome/lib/windows
    #     lametritonus.dll and lame_enc.dll

6.  Copy the speechcloud.war file to the tomcat webapps directory

7.  Now you can run tomcat the normal way.  
     $TOMCAT_HOME/bin/startup.sh (or .bat)
     $TOMCAT_HOME/bin/shutdown.sh (or .bat)

    Optionally you can use the init.d script to audtomatically startup tomcat.


LIMITATIONS
-----------
- 64bit windows not supported

