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

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir (required)
@REM
@REM Optional ENV vars
@REM   M2_HOME - location of maven2's installed home dir
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@if "%MAVEN_SKIP_RC%"=="" @setlocal enabledelayedexpansion
@if "%MAVEN_SKIP_RC%"=="" @setlocal enableextensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto OkJHome
echo.
echo Error: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init
echo.
echo Error: JAVA_HOME is set to an invalid directory. >&2
echo JAVA_HOME = "%JAVA_HOME%" >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

:init
@REM Decide which Maven wrapper to use
if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\*" goto useLocal
if exist "%DIRNAME%\.mvn\wrapper\maven-wrapper.jar" goto useWrapperJar
goto downloadMaven

:useWrapperJar
set MAVEN_CMD="%JAVA_HOME%\bin\java.exe" -jar "%DIRNAME%\.mvn\wrapper\maven-wrapper.jar"
if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\*" (
    for /d %%i in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\*") do (
        if exist "%%i\apache-maven-3.9.9\bin\mvn.cmd" (
            set "M2_HOME=%%i\apache-maven-3.9.9"
        )
    )
)
if not "%M2_HOME%"=="" (
    if exist "%M2_HOME%\bin\mvn.cmd" (
        "%M2_HOME%\bin\mvn.cmd" %*
        goto end
    )
)
%MAVEN_CMD% %*
goto end

:downloadMaven
echo Downloading Maven 3.9.9...
@REM Try multiple download URLs
set MAVEN_URLS="https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" "https://downloads.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
set DOWNLOAD_DIR=%TEMP%\maven-wrapper-download
if not exist "%DOWNLOAD_DIR%" mkdir "%DOWNLOAD_DIR%"
set ZIP_FILE=%DOWNLOAD_DIR%\apache-maven-3.9.9-bin.zip

for %%u in (%MAVEN_URLS%) do (
    if not exist "%ZIP_FILE%" (
        echo Trying %%u
        powershell -Command "try { [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%%~u', '%ZIP_FILE%') } catch { exit 1 }" 2>nul
    )
)

if not exist "%ZIP_FILE%" (
    echo Failed to download Maven. Please download it manually.
    echo 1. Download https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip
    echo 2. Extract it to %USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\
    echo 3. Re-run this script
    goto error
)

echo Extracting Maven...
powershell -Command "try { Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('%ZIP_FILE%', '%DOWNLOAD_DIR%') } catch { exit 1 }" 2>nul

set EXTRACTED_DIR=%DOWNLOAD_DIR%\apache-maven-3.9.9
if not exist "%EXTRACTED_DIR%" (
    echo Extraction failed. Please extract %ZIP_FILE% manually.
    goto error
)

set M2_HOME=%EXTRACTED_DIR%
echo Maven extracted to %M2_HOME%

"%M2_HOME%\bin\mvn.cmd" %*
goto end

:error
exit /b 1

:end
@endlocal & set MAVEN_SKIP_RC=%MAVEN_SKIP_RC%
