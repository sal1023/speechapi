@echo off

set PACKAGE=com.spokentech.speechdown.server.standalone
set CLASS=SpeechServerMain

start "%CLASS%"  .\launch.bat %PACKAGE%.%CLASS% "../etc/speechserver.xml"
