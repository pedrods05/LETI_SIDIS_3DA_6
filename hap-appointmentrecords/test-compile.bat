@echo off
echo Testando compilacao...
call mvnw.cmd compile test-compile
echo.
echo Executando teste...
call mvnw.cmd test -Dtest=AppointmentQueryControllerTest
pause

