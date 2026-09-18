@echo off
setlocal
set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIR=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip"

if exist "%MAVEN_DIR%\bin\mvn.cmd" goto run

if not exist "%MAVEN_ZIP%" (
  if defined MAVEN_DISTRIBUTION_URL (
    set "DOWNLOAD_URL=%MAVEN_DISTRIBUTION_URL%"
  ) else (
    set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri $env:DOWNLOAD_URL -OutFile $env:MAVEN_ZIP"
  if errorlevel 1 exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force -Path $env:MAVEN_ZIP -DestinationPath ($env:BASE_DIR + '.mvn')"
if errorlevel 1 exit /b 1

:run
call "%MAVEN_DIR%\bin\mvn.cmd" %*
exit /b %errorlevel%
