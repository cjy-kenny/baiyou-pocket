@echo off
rem 百游口袋 网页版一键启动（浏览器实时画面）
setlocal
set JAR=%~dp0dist\baiyou-pocket-desktop-v1.0.jar
if not exist "%JAR%" (
  echo 未找到 %JAR%，请先运行: python desktop\build_desktop.py
  pause
  exit /b 1
)
start "" javaw -jar "%JAR%" --web 8080
timeout /t 3 >nul
start "" http://127.0.0.1:8080
endlocal
