@ECHO OFF

set "new_java_home=I:\14_JDKs\Microsoft\jdk-25.0.1+8"
echo CURREN JAVA_HOME: %JAVA_HOME%
setx JAVA_HOME %new_java_home% /m
echo NEW JAVA_HOME: %new_java_home%