#!/bin/bash
export CLASSPATH=`find ./lib -name "*.jar" | tr '\n' ':'`
export MAINCLASS="AMALA"
java -cp ${CLASSPATH}:src/bin $MAINCLASS
