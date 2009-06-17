#!/bin/bash

error() {
   echo "Error occurred"
   exit 1
}


#startValidation
if [ -z "$1" ]
then
   echo "ERROR: improper call to launch script"
   echo "launch.bat should not be executed directly, please see README for"
   echo "proper application launching instructions."
   error
fi


#chkSpeechCloudHome
if [ -z "$SPEECHCLOUD_HOME" ]
then
   echo "SPEECHCLOUD_HOME not found in your environment."
   echo "Please set the SPEECHCLOUD_HOME variable in your environment to match the"
   echo "location of the SpeechCloud installation"
   echo "using pwd"
   SPEECHCLOUD_HOME=$(pwd)/..
fi

#validate SpeechCloudHome
SPEECHCLOUD_JAR="$SPEECHCLOUD_HOME/lib/speechcloud-cli.jar"
if [ ! -e "$SPEECHCLOUD_JAR" ] ; then
   echo
   echo "ERROR: SPEECHCLOUD_HOME is set to an invalid directory."
   echo "$SPEECHCLOUD_HOME \n"
   echo "SPEECHCLOUD_JAR not found!"
   echo "Please set the SPEECHCLOUD_HOME variable in your environment to match the"
   echo "location of the SpeechCloud installation"
   echo
   error
fi

#chkJavaHome
if [ -z "$JAVA_HOME" ] ; then 
   echo
   echo "ERROR: JAVA_HOME not found in your environment."
   echo "Please set the JAVA_HOME variable in your environment to match the"
   echo "location of your Java installation"
   echo
   error
fi
   
#valJavaHome
if [ ! -e "$JAVA_HOME/bin/java" ] ; then
   echo "ERROR: JAVA_HOME is set to an invalid directory.
   echo "JAVA_HOME = $JAVA_HOME"
   echo "Please set the JAVA_HOME variable in your environment to match the
   echo "location of your Java installation"
   error
fi


#setClasspath
#NOTE: OS and OSTYPE do not seem to work on my installation so the
#check below is commented out.  Will add a item in issue tracker
#In any case, it is unlikey that shell scripts will be needed on windows anyway.
#scripts work on Ubuntu linux.
CPATH=$SPEECHCLOUD_JAR
for file in $( find $SPEECHCLOUD_HOME/lib -name '*.jar' |sort)
do
   # classpath delimiter different in windows
   #if [ $OS = "Windows_NT" ]; then
       #CPATH="$CPATH;$file"
   #else 
      CPATH="$CPATH:$file"
   #fi
   #echo $file
done

# classpath delimiter different in windows
#if [ $OS = "Windows_NT" ]; then
    #CPATH="$CPATH;$SPEECHCLOUD_HOME/etc"
#else 
   CPATH="$CPATH:$SPEECHCLOUD_HOME/etc"
#fi

echo CPATH=$CPATH
#export CLASSPATH=$CPATH

#run
#Some command parmas that may be useful
#-XX:+UseParallelGC  -XX:+UseConcMarkSweepGC -Xincgc 
#-Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000
#-Xms100m -Xmx200m 
#-verbose:gc
#-Dawt.toolkit=sun.awt.HeadlessToolkit (workaround for headless JMF on linux)

echo $@

#"$JAVA_HOME/bin/java" -cp $CPATH  -Xmx200m  -XX:+UseConcMarkSweepGC -Dawt.toolkit=sun.awt.HeadlessToolkit -Dlog4j.configuration=log4j.xml -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 "$@"
"$JAVA_HOME/bin/java" -cp $CPATH  -Dlog4j.configuration=log4j.xml "$@"
exit 0

