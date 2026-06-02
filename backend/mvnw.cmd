@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ============================================================
@REM 强制使用 JDK 1.8 编译（项目要求 Java 8）
@REM ============================================================
@IF EXIST "C:\Program Files\Java\jdk1.8.0_152\bin\java.exe" SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_152

@IF "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%
@SETLOCAL

set ERROR_CODE=0

@REM set title of command prompt window
title %0

@REM Determine base dir
set MAVEN_PROJECTBASEDIR=%~dp0
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

@REM Execute Maven wrapper
set MAVEN_WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper
set MAVEN_WRAPPER_JAR=%MAVEN_WRAPPER_DIR%\maven-wrapper.jar
set MAVEN_WRAPPER_PROPERTIES=%MAVEN_WRAPPER_DIR%\maven-wrapper.properties

@REM Download wrapper jar if missing
IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
  echo Maven wrapper jar not found, downloading...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = Get-Content '%MAVEN_WRAPPER_PROPERTIES%' | Where-Object { $_ -like 'wrapperUrl=*' }; $url = ($p -replace 'wrapperUrl=',''); New-Item -Force -ItemType Directory -Path '%MAVEN_WRAPPER_DIR%' | Out-Null; Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile '%MAVEN_WRAPPER_JAR%'" || goto error
)

set JAVA_EXE=java.exe
IF NOT "%JAVA_HOME%"=="" (
  IF EXIST "%JAVA_HOME%\bin\java.exe" set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)
set JAVAC_EXE=
for /f "delims=" %%i in ('where javac 2^>nul') do (
  set JAVAC_EXE=%%i
  goto have_javac
)
:have_javac
IF NOT "%JAVAC_EXE%"=="" for %%i in ("%JAVAC_EXE%") do set JAVAC_DIR=%%~dpi
IF NOT "%JAVAC_DIR%"=="" (
  IF EXIST "%JAVAC_DIR%java.exe" (
    set JAVA_EXE=%JAVAC_DIR%java.exe
    goto java_ok
  )
)

IF EXIST "%JAVA_EXE%" for %%i in ("%JAVA_EXE%") do set JAVA_BIN_DIR=%%~dpi
IF NOT "%JAVA_BIN_DIR%"=="" (
  if EXIST "%JAVA_BIN_DIR%javac.exe" (
    goto java_ok
  )
)
echo No compiler detected for current java. Set JAVA_HOME to a JDK path (contains bin\javac.exe).
goto error

:java_ok

"%JAVA_EXE%" -classpath "%MAVEN_WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
IF ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@ENDLOCAL & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%
