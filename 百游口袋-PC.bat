@echo off
rem 百游口袋 PC 端一键启动（实时画面）
setlocal
set JAR=%~dp0dist\baiyou-pocket-desktop-v1.0.jar
if not exist "%JAR%" (
  echo 未找到 %JAR%，请先运行: python desktop\build_desktop.py
  pause
  exit /b 1
)
start "" javaw -jar "%JAR%"
endlocal
