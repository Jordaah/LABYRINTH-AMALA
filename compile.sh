#!/bin/bash
export SOURCES="src"
export CLASSES="src/bin"
export CLASSPATH=`find lib -name "*.jar" | tr '\n' ':'`

javac -cp ${CLASSPATH} -sourcepath ${SOURCES} -d ${CLASSES} $@ `find src -name "[^#.]*.java"`
