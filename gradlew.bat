@ECHO OFF
SET APP_HOME=%~dp0
SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*