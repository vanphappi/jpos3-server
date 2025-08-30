# jpos-server

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
cd build/install/jpos-server
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

- Server port: 9150
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
jpos-server/
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
