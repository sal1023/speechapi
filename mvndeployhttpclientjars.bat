call mvn deploy:deploy-file -Durl=scp://www.speechforge.org/home/webadmin/speechforge.org/html/repository -DrepositoryId=speechforge-repository -DgroupId="org.apache.httpcomponents" -DartifactId=httpcomponents-client -Dversion="4.0-beta2" -Dpackaging=jar -Dfile="lib\httpclient-4.0-beta2.jar"
call mvn deploy:deploy-file -Durl=scp://www.speechforge.org/home/webadmin/speechforge.org/html/repository -DrepositoryId=speechforge-repository -DgroupId="org.apache.httpcomponents" -DartifactId=httpcomponents-core -Dversion="4.0" -Dpackaging=jar -Dfile="lib\httpcore-4.0.jar"
call mvn deploy:deploy-file -Durl=scp://www.speechforge.org/home/webadmin/speechforge.org/html/repository -DrepositoryId=speechforge-repository -DgroupId="org.apache.httpcomponents" -DartifactId=httpcomponents-mime -Dversion="4.0-beta2" -Dpackaging=jar -Dfile="lib\httpmime-4.0-beta2.jar"
call mvn deploy:deploy-file -Durl=scp://www.speechforge.org/home/webadmin/speechforge.org/html/repository -DrepositoryId=speechforge-repository -DgroupId="org.apache.httpcomponents" -DartifactId=httpcomponents-mime4j -Dversion="0.5" -Dpackaging=jar -Dfile="lib\apache-mime4j-0.5.jar"


