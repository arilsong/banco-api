@echo off
echo Iniciando Ambiente de Teste Mojaloop (Node.js)...
start "HUB - Switch Mock" cmd /c "cd d:\Projetos\mojaloop\bancos\api\mock-pisp && node index.js"
timeout /t 2
start "DFSP - BCA Mock" cmd /c "cd d:\Projetos\mojaloop\bancos\api\mock-pisp && node dfsp.js"
echo.
echo Ambiente rodando!
echo Dashboard: http://localhost:4015
echo DFSP Mock (BCA): http://localhost:8081
echo.
pause
