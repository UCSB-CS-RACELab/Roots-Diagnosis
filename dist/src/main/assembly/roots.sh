#!/bin/sh

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set ROOTS_HOME if not already set
[ -z "$ROOTS_HOME" ] && ROOTS_HOME=`cd "$PRGDIR/.." ; pwd`
ROOTS_CLASSPATH="$ROOTS_HOME/lib"
for f in $ROOTS_HOME/lib/*.jar
do
    ROOTS_CLASSPATH=$ROOTS_CLASSPATH:$f
done

ROOTS_CLASSPATH=$ROOTS_CLASSPATH:$ROOTS_HOME/lib

java -Duser.dir=$ROOTS_HOME -classpath $ROOTS_CLASSPATH edu.ucsb.cs.roots.RootsEnvironment $*