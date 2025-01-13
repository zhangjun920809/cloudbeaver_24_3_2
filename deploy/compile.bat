@echo off

echo compaile project
cd ..\server\product\aggregate
call mvn clean verify -Dheadless-platform

echo compaile complate


pause
