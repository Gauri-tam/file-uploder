@echo off
setlocal enabledelayedexpansion

REM === Configuration ===
set "JAVAFX_HOME=C:\Program Files\javafx-jmods-21.0.7"
set "OUTPUT_DIR=D:\Workspace\POCs\javaFX\FileUploader"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.3"

REM === Verify Paths ===
if not exist "%JAVAFX_HOME%\javafx.controls.jmod" (
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

REM === Create Custom Runtime ===
echo Creating custom runtime...
"%JAVA_HOME%\bin\jlink" ^
  --module-path "%JAVA_HOME%\jmods;%JAVAFX_HOME%" ^
  --add-modules java.base,java.logging,java.xml,javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,java.sql ^
  --output "%OUTPUT_DIR%\runtime"

REM === Run jpackage ===
echo Creating installer...
"%JAVA_HOME%\bin\jpackage" ^
  --runtime-image "%OUTPUT_DIR%\runtime" ^
  --dest "%OUTPUT_DIR%" ^
  --install-dir "FileUploader" ^
  --name FileUploader ^
  --input "target" ^
  --main-jar "FileUploader-1.0-SNAPSHOT.jar" ^
  --main-class "org.fileupload.fileuploader.FileUploader" ^
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