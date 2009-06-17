@echo off

if "%OS%"=="Windows_NT" goto init

echo.
echo ERROR: this script not supported for %OS%.
echo You will need to modify this script for it to work with
echo your operating system.
echo.
goto error

:init

@setlocal enableextensions enabledelayedexpansion

@REM set SPEECHCLOUD_VERSION=${project.version}

:startValidation

if not "%1" == "" goto chkSpeechCloudHome

echo.
echo ERROR: improper call to launch script
echo launch.bat should not be executed directly, please see README for
echo proper application launching instructions.
echo.
goto error

:chkSpeechCloudHome

if not "%SPEECHCLOUD_HOME%"=="" goto valSpeechCloudHome

if "%OS%"=="Windows_NT" set SPEECHCLOUD_HOME=%~dp0..
if not "%SPEECHCLOUD_HOME%"=="" goto valSpeechCloudHome

echo.
echo ERROR: SPEECHCLOUD_HOME not found in your environment.
echo Please set the SPEECHCLOUD_HOME variable in your environment to match the
echo location of the SpeechCloud installation
echo.
goto error

:valSpeechCloudHome
set SPEECHCLOUD_JAR=%SPEECHCLOUD_HOME%\lib\speechcloud-cli.jar
if exist "%SPEECHCLOUD_JAR%" goto chkJavaHome

echo.
echo ERROR: SPEECHCLOUD_HOME is set to an invalid directory.
echo SPEECHCLOUD_HOME = %SPEECHCLOUD_HOME%
echo %SPEECHCLOUD_JAR% not found!
echo Please set the SPEECHCLOUD_HOME variable in your environment to match the
echo location of the SpeechCloud installation
echo.
goto error

:chkJavaHome

if not "%JAVA_HOME%" == "" goto valJavaHome

echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:valJavaHome

@REM if exist "%JAVA_HOME%\bin\java.exe" goto chkJMF
@REM removed the jmf and jsapi check (jumped right to classpath)

if exist "%JAVA_HOME%\bin\java.exe" goto setClassPath


echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:chkJMF

if exist "%JAVA_HOME%\jre\lib\ext\jmf.jar" goto chkJSAPI
if exist "%JAVA_HOME%\lib\ext\jmf.jar" goto chkJSAPI

echo.
echo ERROR: Java Media Framework (JMF) is not installed.
echo Please download and install JMF from Sun Java web site:
echo http://java.sun.com/products/java-media/jmf/
echo.
goto error

:chkJSAPI

if exist "%JAVA_HOME%\jre\lib\ext\jsapi.jar" goto setClasspath
if exist "%JAVA_HOME%\lib\ext\jsapi.jar" goto setClasspath
if exist "%SPEECHCLOUD_HOME%\lib\jsapi.jar" goto setClasspath

echo.
echo ERROR: Java Speech API (JSAPI) is not installed.
echo Please run jsapi.exe or jsapi.sh and place the extracted
echo jsapi.jar in %JAVA_HOME%\jre\lib\ext
echo The install file can be downloaded from here:
echo http://www.speechforge.org/downloads/jsapi
echo.
goto error

:setClasspath

set CLASSPATH=%SPEECHCLOUD_JAR%
for %%b in (%SPEECHCLOUD_HOME%\lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%b

set CLASSPATH=!CLASSPATH!;%SPEECHCLOUD_HOME%\etc
@REM echo CLASSPATH=%CLASSPATH%

:run
@rem -XX:+UseParallelGC  -XX:+UseConcMarkSweepGC -Xincgc 
@rem -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 
@rem -Xms100m -Xmx200m 
@rem -verbose:gc
@rem "%JAVA_HOME%\bin\java" -Xmx200m  -XX:+UseConcMarkSweepGC -Dlog4j.configuration=log4j.xml -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 %*

"%JAVA_HOME%\bin\java" -Dlog4j.configuration=log4j.xml  %*
goto exit

:error

if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1
pause
goto eof

:exit

if "%OS%"=="Windows_NT" @endlocal

:eof

@REM === EOF ===
