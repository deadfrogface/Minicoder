@rem Gradle wrapper for Windows
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set GRADLE_HOME=%DIRNAME%
set JAVA_EXE=java.exe
"%JAVA_EXE%" -version >nul 2>&1
if %ERRORLEVEL% neq 0 set JAVA_EXE=java
set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
