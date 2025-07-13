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