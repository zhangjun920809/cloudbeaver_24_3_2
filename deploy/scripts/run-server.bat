@echo off

if "%CARBON_HOME%"=="" set CARBON_HOME=%~sdp0

for /f %%a in ('dir /B /S server\plugins\org.jkiss.dbeaver.launcher*.jar') do SET launcherJar="%%a"

echo %CARBON_HOME%
echo "Starting InDaaSmd Server"

IF NOT EXIST workspace\.metadata (
    IF NOT EXIST workspace\GlobalConfiguration\.dbeaver (
        mkdir workspace\GlobalConfiguration\.dbeaver\
        copy conf\initial-data-sources.conf workspace\GlobalConfiguration\.dbeaver\data-sources.json
    )
)

SET VMARGS_OPTS = ""
If Not Defined JAVA_OPTS (
    SET VMARGS_OPTS = "-Xmx2048M"
)

java %JAVA_OPTS% ^
    -Dfile.encoding=UTF-8 ^
    --add-modules=ALL-SYSTEM ^
    --add-opens=java.base/java.io=ALL-UNNAMED ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
    --add-opens=java.base/java.net=ALL-UNNAMED ^
    --add-opens=java.base/java.nio=ALL-UNNAMED ^
    --add-opens=java.base/java.nio.charset=ALL-UNNAMED ^
    --add-opens=java.base/java.text=ALL-UNNAMED ^
    --add-opens=java.base/java.time=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED ^
    --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED ^
    --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED ^
    --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED  ^
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED ^
    --add-opens=java.base/sun.security.ssl=ALL-UNNAMED ^
    --add-opens=java.base/sun.security.action=ALL-UNNAMED ^
    --add-opens=java.base/sun.security.util=ALL-UNNAMED ^
    --add-opens=java.security.jgss/sun.security.jgss=ALL-UNNAMED ^
    --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED ^
    --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED ^
    --add-opens=java.sql/java.sql=ALL-UNNAMED ^
    -jar  %launcherJar% ^
    -product io.cloudbeaver.product.ce.product ^
    -data %workspacePath% ^
    -web-config conf/indaasmdc.conf ^
    -nl en ^
    -registryMultiLanguage
