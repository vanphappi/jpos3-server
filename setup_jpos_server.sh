#!/bin/bash

# jPOS 3.0.0 Server Auto Setup Script
# Usage: ./setup_jpos_server.sh [project_name] [port]

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
PROJECT_NAME=${1:-"jpos-server"}
SERVER_PORT=${2:-"8000"}
GROUP_ID="com.example.jpos"
VERSION="1.0.0-SNAPSHOT"

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check Java version
check_java_version() {
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "$JAVA_VERSION" -ge 23 ]]; then
            print_status "Java $JAVA_VERSION detected - OK"
            return 0
        else
            print_warning "Java version $JAVA_VERSION detected. jPOS 3.0.0 requires Java 23+"
            return 1
        fi
    else
        print_warning "Java not found"
        return 1
    fi
}

# Function to install SDKMAN
install_sdkman() {
    print_status "Installing SDKMAN..."
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    print_status "SDKMAN installed successfully"
}

# Function to install Java 23
install_java() {
    print_status "Installing Java 23..."
    if ! command_exists sdk; then
        install_sdkman
    fi
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 23-open
    sdk use java 23-open
    print_status "Java 23 installed successfully"
}

# Function to install Gradle
install_gradle() {
    print_status "Installing Gradle 8.11.1..."
    if ! command_exists sdk; then
        install_sdkman
    fi
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install gradle 8.11.1
    sdk use gradle 8.11.1
    print_status "Gradle 8.11.1 installed successfully"
}

# Function to create project structure
create_project_structure() {
    print_status "Creating project structure for $PROJECT_NAME..."
    
    mkdir -p "$PROJECT_NAME"
    cd "$PROJECT_NAME"
    
    # Create directory structure
    mkdir -p src/main/java/com/example
    mkdir -p src/test/java/com/example
    mkdir -p src/dist/bin
    mkdir -p src/dist/cfg/packager
    mkdir -p src/dist/deploy
    mkdir -p src/dist/log
    mkdir -p gradle/wrapper
    
    print_status "Project structure created"
}

# Function to create Gradle files
create_gradle_files() {
    print_status "Creating Gradle configuration files..."
    
    # settings.gradle
    cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        maven { url = uri('https://jpos.org/maven') }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = 'PROJECT_NAME_PLACEHOLDER'
EOF
    sed -i "s/PROJECT_NAME_PLACEHOLDER/$PROJECT_NAME/g" settings.gradle
    
    # build.gradle
    cat > build.gradle << 'EOF'
plugins {
    id 'org.jpos.jposapp' version '0.0.13'
    id 'java-library'
}

group = 'GROUP_ID_PLACEHOLDER'
version = 'VERSION_PLACEHOLDER'

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

repositories {
    mavenCentral()
    maven { url = uri('https://jpos.org/maven') }
}

dependencies {
    implementation 'org.jpos:jpos:3.0.0'
    
    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.9'
    implementation 'ch.qos.logback:logback-classic:1.4.14'
    
    // Testing
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
    testImplementation 'org.mockito:mockito-core:5.8.0'
}

test {
    useJUnitPlatform()
}

// jPOS plugin configuration
jpos {
    target = "devel"
    addGitRevision = true
    addBuildTime = true
    archiveJarName = "${project.name}-${version}.jar"
    installDir = "${buildDir}/install/${project.name}"
}
EOF
    sed -i "s/GROUP_ID_PLACEHOLDER/$GROUP_ID/g" build.gradle
    sed -i "s/VERSION_PLACEHOLDER/$VERSION/g" build.gradle
    
    # gradle.properties
    cat > gradle.properties << 'EOF'
# JVM arguments for development
org.gradle.jvmargs=-Xmx2g -XX:+UseG1GC --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED
org.gradle.parallel=true
org.gradle.daemon=true
org.gradle.configureondemand=true
EOF
    
    # devel.properties
    cat > devel.properties << 'EOF'
# Development configuration
jpos.logger.Q2=INFO,STDOUT
jpos.logger.org.jpos=DEBUG
server.port=SERVER_PORT_PLACEHOLDER
EOF
    sed -i "s/SERVER_PORT_PLACEHOLDER/$SERVER_PORT/g" devel.properties
    
    print_status "Gradle files created"
}

# Function to create jPOS configuration files
create_jpos_configs() {
    print_status "Creating jPOS configuration files..."
    
    # cfg/default.yml
    cat > src/dist/cfg/default.yml << 'EOF'
# jPOS Default Configuration
logger:
  realm: Q2
  level: INFO
  
database:
  enabled: false
  
server:
  port: SERVER_PORT_PLACEHOLDER
  packager: org.jpos.iso.packager.GenericPackager
  
transaction:
  timeout: 30000
  max-sessions: 10
EOF
    sed -i "s/SERVER_PORT_PLACEHOLDER/$SERVER_PORT/g" src/dist/cfg/default.yml
    
    # cfg/packager/iso87ascii.xml
    cat > src/dist/cfg/packager/iso87ascii.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE isopackager SYSTEM "genericpackager.dtd">
<isopackager>
    <isofield id="0" length="4" name="MESSAGE TYPE INDICATOR" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="1" length="16" name="BIT MAP" class="org.jpos.iso.IFA_BITMAP"/>
    <isofield id="2" length="19" name="PAN - PRIMARY ACCOUNT NUMBER" class="org.jpos.iso.IFA_LLNUM"/>
    <isofield id="3" length="6" name="PROCESSING CODE" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="4" length="12" name="AMOUNT, TRANSACTION" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="7" length="10" name="TRANSMISSION DATE AND TIME" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="11" length="6" name="SYSTEM TRACE AUDIT NUMBER" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="12" length="6" name="TIME, LOCAL TRANSACTION" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="13" length="4" name="DATE, LOCAL TRANSACTION" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="15" length="4" name="DATE, SETTLEMENT" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="37" length="12" name="RETRIEVAL REFERENCE NUMBER" class="org.jpos.iso.IFA_ALPHA"/>
    <isofield id="39" length="2" name="RESPONSE CODE" class="org.jpos.iso.IFA_ALPHA"/>
    <isofield id="41" length="16" name="CARD ACCEPTOR TERMINAL IDENTIFICATION" class="org.jpos.iso.IFA_ALPHA"/>
    <isofield id="42" length="15" name="CARD ACCEPTOR IDENTIFICATION CODE" class="org.jpos.iso.IFA_ALPHA"/>
    <isofield id="49" length="3" name="CURRENCY CODE, TRANSACTION" class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="102" length="28" name="ACCOUNT IDENTIFICATION 1" class="org.jpos.iso.IFA_LLNUM"/>
    <isofield id="103" length="28" name="ACCOUNT IDENTIFICATION 2" class="org.jpos.iso.IFA_LLNUM"/>
</isopackager>
EOF
    
    print_status "jPOS configuration files created"
}

# Function to create deploy files
create_deploy_files() {
    print_status "Creating Q2 deployment files..."
    
    # 00_logger.xml
    cat > src/dist/deploy/00_logger.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<logger name="Q2" class="org.jpos.util.SimpleLogListener">
    <property name="file" value="log/q2.log" />
</logger>
EOF
    
    # 10_qserver.xml
    cat > src/dist/deploy/10_qserver.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<qserver name="qserver" class="org.jpos.q2.qbean.QServer" logger="Q2">
    <attr name="port" type="java.lang.Integer">SERVER_PORT_PLACEHOLDER</attr>
    <attr name="minSessions" type="java.lang.Integer">1</attr>
    <attr name="maxSessions" type="java.lang.Integer">100</attr>
    
    <channel class="org.jpos.iso.channel.NACChannel" 
             packager="org.jpos.iso.packager.GenericPackager"
             header="ISO015000077">
        <property name="packager-config" value="cfg/packager/iso87ascii.xml" />
    </channel>
    
    <request-listener class="org.jpos.iso.ISORequestListener">
        <property name="timeout" value="30000" />
    </request-listener>
</qserver>
EOF
    sed -i "s/SERVER_PORT_PLACEHOLDER/$SERVER_PORT/g" src/dist/deploy/10_qserver.xml
    
    # 20_txnmgr.xml
    cat > src/dist/deploy/20_txnmgr.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<txnmgr name="txnmgr" class="org.jpos.transaction.TransactionManager" logger="Q2">
    <property name="space" value="transient:default" />
    <property name="queue" value="txnqueue" />
    <property name="max-sessions" value="10" />
    <property name="debug" value="true" />
    
    <participant class="com.example.MyParticipant" logger="Q2">
        <property name="timeout" value="30000" />
    </participant>
    
    <participant class="com.example.ResponseParticipant" logger="Q2" />
</txnmgr>
EOF
    
    # 30_channel_adaptor.xml
    cat > src/dist/deploy/30_channel_adaptor.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel-adaptor name="channel-adaptor" class="org.jpos.q2.qbean.ChannelAdaptor" logger="Q2">
    <channel class="org.jpos.iso.channel.NACChannel" 
             packager="org.jpos.iso.packager.GenericPackager"
             header="ISO015000077">
        <property name="packager-config" value="cfg/packager/iso87ascii.xml" />
        <property name="host" value="localhost" />
        <property name="port" value="SERVER_PORT_PLACEHOLDER" />
    </channel>
    
    <in>txnqueue</in>
    <out>txnqueue</out>
    <reconnect-delay>10000</reconnect-delay>
</channel-adaptor>
EOF
    sed -i "s/SERVER_PORT_PLACEHOLDER/$SERVER_PORT/g" src/dist/deploy/30_channel_adaptor.xml
    
    # 99_sysmon.xml
    cat > src/dist/deploy/99_sysmon.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<sysmon logger="Q2">
    <attr name="sleepTime" type="java.lang.Long">3600000</attr>
    <attr name="detailRequired" type="java.lang.Boolean">true</attr>
</sysmon>
EOF
    
    print_status "Deployment files created"
}

# Function to create shell scripts
create_shell_scripts() {
    print_status "Creating shell scripts..."
    
    # q2 script
    cat > src/dist/bin/q2 << 'EOF'
#!/bin/bash
cd `dirname $0`/.. && java $JAVA_OPTS -server -Xmx1G \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    -jar PROJECT_NAME_PLACEHOLDER-VERSION_PLACEHOLDER.jar "$@"
EOF
    sed -i "s/PROJECT_NAME_PLACEHOLDER/$PROJECT_NAME/g" src/dist/bin/q2
    sed -i "s/VERSION_PLACEHOLDER/$VERSION/g" src/dist/bin/q2
    
    # start script
    cat > src/dist/bin/start << 'EOF'
#!/bin/bash
cd `dirname $0`/..
exec java $JAVA_OPTS -server -Xmx1G \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    -jar PROJECT_NAME_PLACEHOLDER-VERSION_PLACEHOLDER.jar > log/Q2.log 2>&1 &
echo $! > log/Q2.pid
echo "jPOS Server started with PID: $(cat log/Q2.pid)"
echo "Log file: log/Q2.log"
EOF
    sed -i "s/PROJECT_NAME_PLACEHOLDER/$PROJECT_NAME/g" src/dist/bin/start
    sed -i "s/VERSION_PLACEHOLDER/$VERSION/g" src/dist/bin/start
    
    # stop script
    cat > src/dist/bin/stop << 'EOF'
#!/bin/bash
cd `dirname $0`/..
if [ -f log/Q2.pid ]; then
    PID=$(cat log/Q2.pid)
    echo "Stopping jPOS Server (PID: $PID)..."
    kill $PID
    rm -f log/Q2.pid
    echo "jPOS Server stopped"
else
    echo "No PID file found. Server may not be running."
fi
EOF
    
    # Make scripts executable
    chmod +x src/dist/bin/*
    
    print_status "Shell scripts created and made executable"
}

# Function to create Java source files
create_java_sources() {
    print_status "Creating Java source files..."
    
    # MyParticipant.java
    cat > src/main/java/com/example/MyParticipant.java << 'EOF'
package com.example;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main transaction participant for processing ISO messages
 */
public class MyParticipant implements TransactionParticipant {
    private Log log;
    
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        
        try {
            ISOMsg request = (ISOMsg) ctx.get("REQUEST");
            
            if (request == null) {
                log.error("No request message found in context");
                return ABORTED | NO_JOIN;
            }
            
            String mti = request.getMTI();
            log.info("Processing transaction " + id + " with MTI: " + mti);
            
            // Log key fields
            if (request.hasField(2)) {
                log.info("PAN: " + maskPAN(request.getString(2)));
            }
            if (request.hasField(4)) {
                log.info("Amount: " + request.getString(4));
            }
            if (request.hasField(11)) {
                log.info("STAN: " + request.getString(11));
            }
            
            // Create response based on message type
            ISOMsg response = createResponse(request);
            ctx.put("RESPONSE", response);
            
            log.info("Transaction " + id + " prepared successfully");
            return PREPARED | NO_JOIN;
            
        } catch (Exception e) {
            log.error("Error processing transaction " + id, e);
            return ABORTED | NO_JOIN;
        }
    }
    
    @Override
    public void commit(long id, Serializable context) {
        log.info("Transaction " + id + " committed successfully");
    }
    
    @Override
    public void abort(long id, Serializable context) {
        log.info("Transaction " + id + " aborted");
    }
    
    private ISOMsg createResponse(ISOMsg request) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        
        // Set response MTI
        String requestMTI = request.getMTI();
        String responseMTI = getResponseMTI(requestMTI);
        response.setMTI(responseMTI);
        
        // Set response code
        response.set(39, "00"); // Approved
        
        // Set transmission date/time if not present
        if (!response.hasField(7)) {
            String timestamp = new SimpleDateFormat("MMddHHmmss").format(new Date());
            response.set(7, timestamp);
        }
        
        return response;
    }
    
    private String getResponseMTI(String requestMTI) {
        // Convert request MTI to response MTI
        switch (requestMTI) {
            case "0200": return "0210"; // Authorization request -> response
            case "0400": return "0410"; // Reversal request -> response
            case "0800": return "0810"; // Network management request -> response
            default: return "0210";     // Default response
        }
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) {
            return "****";
        }
        return pan.substring(0, 6) + "****" + pan.substring(pan.length() - 4);
    }
    
    public void setLog(Log log) {
        this.log = log;
    }
}
EOF
    
    # ResponseParticipant.java
    cat > src/main/java/com/example/ResponseParticipant.java << 'EOF'
package com.example;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.iso.ISOMsg;
import org.jpos.util.Log;

import java.io.Serializable;

/**
 * Participant for sending responses back to clients
 */
public class ResponseParticipant implements TransactionParticipant {
    private Log log;
    
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        
        try {
            ISOMsg response = (ISOMsg) ctx.get("RESPONSE");
            
            if (response != null) {
                log.info("Sending response for transaction " + id + ": " + response.getMTI());
                // Response will be automatically sent by the channel adaptor
                return PREPARED | NO_JOIN;
            } else {
                log.warn("No response to send for transaction " + id);
                return ABORTED | NO_JOIN;
            }
            
        } catch (Exception e) {
            log.error("Error in response participant for transaction " + id, e);
            return ABORTED | NO_JOIN;
        }
    }
    
    @Override
    public void commit(long id, Serializable context) {
        log.debug("Response sent successfully for transaction " + id);
    }
    
    @Override
    public void abort(long id, Serializable context) {
        log.warn("Response sending aborted for transaction " + id);
    }
    
    public void setLog(Log log) {
        this.log = log;
    }
}
EOF
    
    # Test client - FIXED: Proper channel setup and imports
    cat > src/test/java/com/example/TestClient.java << 'EOF'
package com.example;

import org.jpos.iso.*;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;

/**
 * Simple test client for testing the jPOS server
 */
public class TestClient {
    public static void main(String[] args) throws Exception {
        System.out.println("jPOS Test Client");
        System.out.println("Connecting to localhost:SERVER_PORT_PLACEHOLDER");
        
        // Create packager programmatically to avoid file path issues
        ISOPackager packager = createPackager();
        
        // Create channel - correct way for jPOS 3.0
        NACChannel channel = new NACChannel();
        channel.setHost("localhost");
        channel.setPort(SERVER_PORT_PLACEHOLDER);
        channel.setPackager(packager);
        
        try {
            // Connect to server
            channel.connect();
            System.out.println("Connected successfully");
            
            // Create test message
            ISOMsg msg = new ISOMsg();
            msg.setMTI("0200");
            msg.set(2, "4111111111111111");    // PAN
            msg.set(3, "000000");              // Processing code
            msg.set(4, "000000001000");        // Amount
            msg.set(11, "000001");             // STAN
            msg.set(41, "12345678");           // Terminal ID
            msg.set(42, "123456789012345");    // Merchant ID
            msg.set(49, "840");                // Currency (USD)
            
            System.out.println("Sending message: " + msg);
            
            // Send and receive
            channel.send(msg);
            ISOMsg response = channel.receive();
            
            System.out.println("Received response: " + response);
            System.out.println("Response code: " + response.getString(39));
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Disconnect
            if (channel.isConnected()) {
                channel.disconnect();
                System.out.println("Disconnected");
            }
        }
    }
    
    private static ISOPackager createPackager() throws ISOException {
        // Create a simple packager programmatically
        GenericPackager packager = new GenericPackager();
        
        // Set packager configuration programmatically
        ISOFieldPackager[] packagers = new ISOFieldPackager[128];
        
        // MTI
        packagers[0] = new IFA_NUMERIC(4, "MESSAGE TYPE INDICATOR");
        
        // Primary bitmap
        packagers[1] = new IFA_BITMAP(16, "BIT MAP");
        
        // Common fields
        packagers[2] = new IFA_LLNUM(19, "PAN - PRIMARY ACCOUNT NUMBER");
        packagers[3] = new IFA_NUMERIC(6, "PROCESSING CODE");
        packagers[4] = new IFA_NUMERIC(12, "AMOUNT, TRANSACTION");
        packagers[7] = new IFA_NUMERIC(10, "TRANSMISSION DATE AND TIME");
        packagers[11] = new IFA_NUMERIC(6, "SYSTEM TRACE AUDIT NUMBER");
        packagers[12] = new IFA_NUMERIC(6, "TIME, LOCAL TRANSACTION");
        packagers[13] = new IFA_NUMERIC(4, "DATE, LOCAL TRANSACTION");
        packagers[15] = new IFA_NUMERIC(4, "DATE, SETTLEMENT");
        packagers[37] = new IF_CHAR(12, "RETRIEVAL REFERENCE NUMBER");
        packagers[39] = new IF_CHAR(2, "RESPONSE CODE");
        packagers[41] = new IF_CHAR(16, "CARD ACCEPTOR TERMINAL IDENTIFICATION");
        packagers[42] = new IF_CHAR(15, "CARD ACCEPTOR IDENTIFICATION CODE");
        packagers[49] = new IFA_NUMERIC(3, "CURRENCY CODE, TRANSACTION");
        packagers[102] = new IFA_LLNUM(28, "ACCOUNT IDENTIFICATION 1");
        packagers[103] = new IFA_LLNUM(28, "ACCOUNT IDENTIFICATION 2");
        
        packager.setFieldPackager(packagers);
        return packager;
    }
}
EOF
    sed -i "s/SERVER_PORT_PLACEHOLDER/$SERVER_PORT/g" src/test/java/com/example/TestClient.java
    
    print_status "Java source files created"
}

# Function to create additional files
create_additional_files() {
    print_status "Creating additional files..."
    
    # README.md
    cat > README.md << 'EOF'
# PROJECT_NAME_PLACEHOLDER

A jPOS 3.0.0 server implementation for ISO-8583 message processing.

## Requirements

- Java 23+
- Gradle 8.11+

## Building

```bash
./gradlew build
```

## Running

### Development Mode
```bash
./gradlew installApp
cd build/install/PROJECT_NAME_PLACEHOLDER
bin/q2
```

### Production Mode
```bash
# Start server
bin/start

# Stop server  
bin/stop

# View logs
tail -f log/q2.log
```

## Testing

Run the test client:
```bash
./gradlew test --tests com.example.TestClient
```

## Configuration

- Server port: SERVER_PORT_PLACEHOLDER
- Logs: `log/q2.log`
- Configuration: `cfg/default.yml`

## Features

- ISO-8583 message processing
- Virtual Threads support (Java 23)
- Transaction management
- Request/Response handling
- Comprehensive logging

## Structure

```
PROJECT_NAME_PLACEHOLDER/
├── build.gradle                    # Build configuration
├── src/
│   ├── main/java/                  # Java source code
│   └── dist/                       # jPOS distribution files
│       ├── bin/                    # Start/stop scripts
│       ├── cfg/                    # Configuration files
│       ├── deploy/                 # Q2 deployment files
│       └── log/                    # Log directory
└── README.md
```
EOF
    sed -i "s/PROJECT_NAME_PLACEHOLDER/$PROJECT_NAME/g" README.md
    sed -i "s/SERVER_PORT_PLACEHOLDER/$SERVER_PORT/g" README.md
    
    # .gitignore
    cat > .gitignore << 'EOF'
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
.vscode/
*.iml
*.iws
*.ipr

# jPOS
log/*.log
*.pid
deploy/*.xml.bak

# Java
*.class
*.jar
!gradle/wrapper/gradle-wrapper.jar
*.war
*.ear
hs_err_pid*

# OS
.DS_Store
Thumbs.db
EOF
    
    # Create log directory placeholder
    touch src/dist/log/.gitkeep
    
    print_status "Additional files created"
}

# Function to build project
build_project() {
    print_status "Building project..."
    
    # Initialize Gradle wrapper if not exists
    if [[ ! -f gradlew ]]; then
        gradle wrapper --gradle-version 8.11.1
    fi
    
    # Build project
    ./gradlew build
    
    # Install app
    ./gradlew installApp
    
    print_status "Project built successfully"
}

# Function to show final instructions
show_final_instructions() {
    print_header "Setup Complete!"
    
    echo -e "${GREEN}Project: ${NC}$PROJECT_NAME"
    echo -e "${GREEN}Port: ${NC}$SERVER_PORT"
    echo -e "${GREEN}Location: ${NC}$(pwd)"
    echo ""
    
    print_header "How to Run"
    echo "1. Start the server:"
    echo "   cd $PROJECT_NAME"
    echo "   ./gradlew installApp"
    echo "   cd build/install/$PROJECT_NAME"
    echo "   bin/q2"
    echo ""
    
    echo "2. Or in background:"
    echo "   bin/start"
    echo ""
    
    echo "3. Stop the server:"
    echo "   bin/stop"
    echo ""
    
    echo "4. View logs:"
    echo "   tail -f log/q2.log"
    echo ""
    
    echo "5. Test the server:"
    echo "   ./gradlew test --tests com.example.TestClient"
    echo ""
    
    print_header "Testing with telnet"
    echo "telnet localhost $SERVER_PORT"
    echo ""
    
    print_status "jPOS 3.0.0 Server setup completed successfully!"
}

# Main execution
main() {
    print_header "jPOS 3.0.0 Server Auto Setup"
    echo "Project: $PROJECT_NAME"
    echo "Port: $SERVER_PORT"
    echo ""
    
    # Check and install prerequisites
    if ! check_java_version; then
        print_warning "Installing Java 23..."
        install_java
    fi
    
    if ! command_exists gradle; then
        print_warning "Installing Gradle..."
        install_gradle
    fi
    
    # Create project
    create_project_structure
    create_gradle_files
    create_jpos_configs
    create_deploy_files
    create_shell_scripts
    create_java_sources
    create_additional_files
    
    # Build project
    build_project
    
    # Show final instructions
    show_final_instructions
}

# Run main function
main "$@"