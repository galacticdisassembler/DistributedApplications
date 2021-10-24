#!/bin/sh


SCRIPT=`realpath -s $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOT_PROJECT_DIR="$SCRIPTPATH/.."

source $SCRIPTPATH/__common.sh

processArgumentsSimpleImpl $@

java -jar \
  -Dlog4j.configurationFile="file:/$ROOT_PROJECT_DIR/proxy-app/data-warehouse/target/classes/log4j2.xml" \
  $ROOT_PROJECT_DIR/proxy-app/data-warehouse/target/data-warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar \
  port=$g_port
