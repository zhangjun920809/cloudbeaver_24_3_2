#!/bin/bash
set -Eeo pipefail
set +u

echo "Clone and build Cloudbeaver"

rm -rf ./drivers
rm -rf ./indaasmdc
mkdir ./indaasmdc
mkdir ./indaasmdc/server
mkdir ./indaasmdc/conf
mkdir ./indaasmdc/workspace
mkdir ./indaasmdc/license
mkdir ./indaasmdc/security

echo "Pull indaasmdc platform"

cd ../..

echo "已手动下载"
#[ ! -d dbeaver ] && git clone --depth 1 https://github.com/dbeaver/dbeaver.git
#[ ! -d dbeaver-common ] && git clone --depth 1 https://github.com/dbeaver/dbeaver-common.git
#[ ! -d dbeaver-jdbc-libsql ] && git clone --depth 1 https://github.com/dbeaver/dbeaver-jdbc-libsql.git


cd cloudbeaver/deploy

echo "Build CloudBeaver server"

cd ../server/product/aggregate
mvn clean verify $MAVEN_COMMON_OPTS -Dheadless-platform
if [[ "$?" -ne 0 ]] ; then
  echo 'Could not perform package'; exit $rc
fi
cd ../../../deploy

echo "Copy server packages"

cp -rp ../server/product/web-server/target/products/io.cloudbeaver.product/all/all/all/* ./indaasmdc/server
cp -p ./scripts/* ./indaasmdc
cp -p ./license/* ./indaasmdc/license
cp -p ./security/* ./indaasmdc/security
mkdir indaasmdc/samples

cp -rp  ../config/core/* indaasmdc/conf
cp -rp ../config/GlobalConfiguration/.dbeaver/data-sources.json indaasmdc/conf/initial-data-sources.conf
mv drivers indaasmdc

echo "End of backend build"