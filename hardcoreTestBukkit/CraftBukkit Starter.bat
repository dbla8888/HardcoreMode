@ECHO OFF
IF /I "%PROCESSOR_ARCHITECTURE:~-2%"=="64" "%ProgramFiles(x86)%\Java\jre6\bin\java.exe" -Xincgc -Xmx1024M -jar "%~dp0craftbukkit-1.2.5-R4.0.jar"
IF /I "%PROCESSOR_ARCHITECTURE:~-2%"=="86"  java -Xincgc -Xmx1024M -jar "%~dp0craftbukkit-1.2.5-R4.0.jar"
PAUSE