@echo off

rem command line arguments

echo Clone and build Indaasmdc

IF EXIST drivers rmdir /S /Q drivers
IF EXIST indaasmdc rmdir /S /Q indaasmdc
mkdir indaasmdc
mkdir indaasmdc\server
mkdir indaasmdc\conf
mkdir indaasmdc\workspace
mkdir indaasmdc\web

echo already download

@REM cd ..\..

@REM echo Pull dbeaver platform

@REM IF NOT EXIST dbeaver git clone https://github.com/dbeaver/dbeaver.git
@REM IF NOT EXIST dbeaver-common git clone https://github.com/dbeaver/dbeaver-common.git
@REM IF NOT EXIST dbeaver-jdbc-libsql git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git

@REM cd cloudbeaver\deploy

echo Build indaasmdc server

cd ..\server\product\aggregate
call mvn clean verify -Dheadless-platform

cd ..\..\..\deploy

echo Copy server packages

xcopy /E /Q ..\server\product\web-server\target\products\io.cloudbeaver.product\all\all\all\* indaasmdc\server >NUL
copy scripts\* indaasmdc >NUL
mkdir indaasmdc\samples


copy ..\config\core\* indaasmdc\conf >NUL
copy ..\config\DefaultConfiguration\GlobalConfiguration\.dbeaver\data-sources.json indaasmdc\conf\initial-data-sources.conf >NUL

move drivers indaasmdc >NUL

echo "Build static content"

mkdir .\indaasmdc\web

cd ..\webapp

call yarn
cd .\packages\product-default
call yarn run bundle

if %ERRORLEVEL% neq 0 (
    echo 'Application build failed'
    exit /b %ERRORLEVEL%
)

cd ..\..\
@REM  call yarn test

@REM  if %ERRORLEVEL% neq 0 (
@REM     echo 'Frontend tests failed'
@REM     exit /b %ERRORLEVEL%
@REM )

cd ..\deploy

echo "Copy static content"

xcopy /E /Q ..\webapp\packages\product-default\lib indaasmdc\web >NUL

echo "indaasmdc is ready. Run run-server.bat in indaasmdc folder to start the server."

pause
