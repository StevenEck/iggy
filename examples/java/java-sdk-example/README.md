ISSUE #1893
Following the reorganization of our examples directory structure (with Rust examples now in examples/rust/), we need to create comprehensive Java examples to showcase the Iggy Java SDK capabilities.


Task
Create a new examples/java/ directory with Java examples that demonstrate various usage patterns of the Iggy Java SDK, similar to what we have for Rust.

Requirements
Create examples that mirror the functionality of existing Rust examples where applicable
Use Java 11 or later for modern Java features
Include a README.md with clear instructions on how to run each example
Use Java best practices and idiomatic code
Add Maven pom.xml or Gradle build files with all required dependencies
Consider adding a script to test all examples (similar to scripts/run-rust-examples-from-readme.sh)
Additional Context
Java SDK is located in foreign/java/
Reference the Rust examples in examples/rust/ for feature parity
Examples should work with the latest version of the Java SDK
Consider using CompletableFuture for async operations%


NOTES: Requirements say 11, but some functionality like Java Records in the Java SDK require at least 17:
"java: cannot access java.lang.Record
class file for java.lang.Record not found"

### Notes on BasicProducer:

The `BasicProducer.java` now closely mirrors the Rust `main.rs` producer example in both structure and logic:

***Key Parity Points:***
- **Argument Parsing:** Both parse similar arguments for host, port, stream, topic, partition, batch size, and batch limit.
- **Client Setup:** Both connect, authenticate, and ping the server.
- **Resource Initialization:** Both check for and create streams/topics if missing.
- **Message Production Loop:** Both send batches of messages, incrementing a counter for each payload, and respect a batch limit.
- **Partitioning:** Both use explicit partitioning (`Partitioning.partitionId` in Java, `Partitioning::partition_id` in Rust).
- **Logging:** Both log sent messages and batch progress.

**Minor Differences:**
- **Interval/Throttling:** Rust uses an optional interval (from args), Java uses a fixed `Thread.sleep(1000)`. You could make this configurable for full parity.
- **Async:** Rust is async, Java is blocking (matching the SDKs' idioms).
- **Error Handling:** Rust uses `Result`, Java throws exceptions.

**Summary:**  
The Java example is now idiomatic and matches the Rust example's functionality and flow very well, fulfilling the README's requirements. Only minor enhancements (like configurable interval) could bring it even closer.
