@echo off
setlocal enabledelayedexpansion

REM === Configuration ===
set "JAVAFX_HOME=C:\Program Files\javafx-sdk-21.0.7"
set "OUTPUT_DIR=D:\Workspace\POCs\javaFX\FileUploader"
set "MAIN_CLASS=org.fileupload.fileuploader.FileUploader"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.3"

REM === Verify Paths ===
if not exist "%JAVAFX_HOME%\lib\javafx.controls.jar" (
    echo Error: JavaFX not found at %JAVAFX_HOME%
    pause
    exit /b 1
)

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo Error: JDK not found at %JAVA_HOME%
    pause
    exit /b 1
)

REM === Create Output Directory ===
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM === Run jpackage ===
echo Creating installer...
"%JAVA_HOME%\bin\jpackage" ^
  --runtime-image "%JAVA_HOME%" ^
  --dest "%OUTPUT_DIR%" ^
  --name FileUploader ^
  --module-path "%JAVAFX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web ^
  --input target ^
  --main-jar fileUploader-1.0-SNAPSHOT.jar ^
  --main-class %MAIN_CLASS% ^
  --type exe ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --app-version 1.0 ^
  --description "File Uploader Application" ^
  --vendor "YourCompany"

if %errorlevel% neq 0 (
    echo Error: Installer creation failed
    pause
    exit /b 1
)

echo Installer created successfully in %OUTPUT_DIR%\
pause