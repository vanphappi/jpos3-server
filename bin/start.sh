#!/bin/bash
# jPOS 3.0.0 Enhanced Start Script with Virtual Threads

JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 23)}
JPOS_HOME="$(cd "$(dirname "$0")/.." && pwd)"

echo "🚀 Starting jPOS 3.0.0 Server..."
echo "📍 jPOS Home: $JPOS_HOME"  
echo "☕ Java Home: $JAVA_HOME"

# Check Java version
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n1 | cut -d'"' -f2)
if [[ ! "$JAVA_VERSION" =~ ^23\. ]]; then
    echo "❌ Java 23+ required. Found: $JAVA_VERSION"
    exit 1
fi

# JVM Options for Virtual Threads
JVM_OPTS="-server"
JVM_OPTS="$JVM_OPTS --enable-preview"
JVM_OPTS="$JVM_OPTS -Xmx4g -Xms1g" 
JVM_OPTS="$JVM_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JVM_OPTS="$JVM_OPTS --add-opens java.base/java.util=ALL-UNNAMED"

# Virtual Threads tuning
JVM_OPTS="$JVM_OPTS -Djdk.virtualThreadScheduler.parallelism=200"
JVM_OPTS="$JVM_OPTS -Djdk.virtualThreadScheduler.maxPoolSize=256"

# GC tuning for high throughput
JVM_OPTS="$JVM_OPTS -XX:+UseZGC"
JVM_OPTS="$JVM_OPTS -XX:+UnlockExperimentalVMOptions"

# jPOS system properties
JVM_OPTS="$JVM_OPTS -Djpos.config.dir=$JPOS_HOME/cfg"
JVM_OPTS="$JVM_OPTS -Djpos.deploy.dir=$JPOS_HOME/deploy"
JVM_OPTS="$JVM_OPTS -Djpos.log.dir=$JPOS_HOME/log"

# Performance monitoring
JVM_OPTS="$JVM_OPTS -XX:+FlightRecorder"
JVM_OPTS="$JVM_OPTS -XX:StartFlightRecording=duration=1h,filename=jpos-flight-recording.jfr"

cd "$JPOS_HOME"

# Run with Gradle
if [ -f "./gradlew" ]; then
    echo "🔧 Starting with Gradle..."
    ./gradlew startServer
else
    echo "🔧 Starting with Java directly..."
    "$JAVA_HOME/bin/java" $JVM_OPTS -cp "build/install/jpos3-server/lib/*" \
        com.company.jpos.JPOSServerMain
fi