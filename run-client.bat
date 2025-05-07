
@echo off
set /p COUNT=Please enter the number of chat clients to open: 
echo Launching %COUNT% client windows with 2 seconds delay...

for /L %%i in (1,1,%COUNT%) do (
    start cmd /k "cd src && javac --module-path D:\project\javafx-sdk-24.0.1\lib --add-modules javafx.controls,javafx.fxml ChatClientGUI.java && java --module-path D:\project\javafx-sdk-24.0.1\lib --add-modules javafx.controls,javafx.fxml ChatClientGUI"
    timeout /t 2 >nul
)
