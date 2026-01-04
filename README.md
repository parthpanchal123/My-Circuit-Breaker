# Circuit Breaker Implementation

A simple, thread-safe Circuit Breaker pattern implementation in Java using Spring Boot. This project demonstrates how to build a resilient service that gracefully handles failures in distributed systems.

## 🎯 What is a Circuit Breaker?

Read the blog here : [Circuit Breaker Pattern Explained](https://parths-blog.hashnode.dev/write-your-own-circuit-breaker)

## ✨ Features

- **Three-State Design**: CLOSED, OPEN, and HALF_OPEN states
- **Thread-Safe**: Uses `ReentrantLock`, `AtomicBoolean`, and `AtomicLong` for concurrent access
- **Configurable**: Easy configuration via `application.yml`
- **Rolling Window**: Only counts failures within a time window
- **Automatic Recovery**: Periodically tests if the service has recovered
- **Fallback Support**: Returns fallback values when circuit is open
- **Spring Boot Integration**: Seamlessly integrates with Spring Boot applications

## 🏗️ Architecture

### States

1. **CLOSED** 🔵: Normal operation - requests flow through, failures are counted
2. **OPEN** 🔴: Circuit is open - requests immediately return fallback without calling the service
3. **HALF_OPEN** 🟡: Testing state - makes a single test call to check if service recovered

### State Transitions

```
CLOSED → OPEN: When failure threshold is reached within rolling window
OPEN → HALF_OPEN: After timeout period expires
HALF_OPEN → CLOSED: If test call succeeds
HALF_OPEN → OPEN: If test call fails
```

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+ (or use the included Maven wrapper)
- Spring Boot 3.3.0

## 🚀 Getting Started

### Clone the Repository

```bash
git clone <your-repo-url>
cd demo
```

### Build the Project

```bash
./mvnw clean install
```

Or on Windows:

```bash
mvnw.cmd clean install
```

### Run the Application

```bash
./mvnw spring-boot:run
```

Or on Windows:

```bash
mvnw.cmd spring-boot:run
```

The application will start on port `8282` and automatically run the demo.

## ⚙️ Configuration

Configure the Circuit Breaker in `src/main/resources/application.yml`:

```yaml
app:
  circuitbreaker:
    failureThreshold: 5        # Number of failures before opening circuit
    rollingWindowMillis: 10000   # Time window for counting failures (10 seconds)
    slidingWindowSize: 5000      # How long to stay open before testing (5 seconds)
```

### Configuration Parameters

- **failureThreshold**: Number of failures within the rolling window that will open the circuit (default: 5)
- **rollingWindowMillis**: Time window in milliseconds for counting failures (default: 10000ms = 10 seconds)
- **slidingWindowSize**: Duration in milliseconds the circuit stays OPEN before transitioning to HALF_OPEN (default: 5000ms = 5 seconds)

## 💻 Usage

### Basic Example

```java
@Autowired
CircuitBreaker circuitBreaker;

public void makeServiceCall() {
    // Define your service call
    Supplier<String> serviceCall = () -> {
        // Your actual service call here
        return externalService.getData();
    };
    
    // Define fallback
    Supplier<String> fallback = () -> {
        return "cached-value"; // or get from cache
    };
    
    // Execute with circuit breaker
    String result = circuitBreaker.execute(serviceCall, fallback);
}
```

### Example with Exception Handling

```java
Supplier<User> getUserCall = () -> {
    return userService.getUserById(userId);
};

Supplier<User> fallback = () -> {
    // Return cached user or default user
    return userCache.get(userId).orElse(getDefaultUser());
};

User user = circuitBreaker.execute(getUserCall, fallback);
```

## 🔍 How It Works

1. **CLOSED State**: 
   - All calls go through normally
   - Failures are recorded with timestamps
   - Old failures outside the rolling window are pruned
   - When failure count reaches threshold → transition to OPEN

2. **OPEN State**:
   - All calls immediately return fallback (no service call made)
   - Circuit stays open for `slidingWindowSize` duration
   - After timeout → transition to HALF_OPEN

3. **HALF_OPEN State**:
   - Only one test call is allowed (thread-safe)
   - If test succeeds → reset and transition to CLOSED
   - If test fails → transition back to OPEN

### Thread Safety

The implementation ensures thread safety through:
- `ReentrantLock` for state transitions
- `AtomicBoolean` for half-open trial flag
- `AtomicLong` for open-until timestamp
- `ConcurrentLinkedDeque` for failure timestamps

## 📊 Example Output

When running the demo, you'll see output like:

```
CircuitBreaker initialized: failureThreshold=5, rollingWindowMillis=10000, openStateMillis=5000
Failure recorded. Current failures: 1/5
call 0 -> cached-value (state=CLOSED)
Failure recorded. Current failures: 2/5
call 1 -> cached-value (state=CLOSED)
...
Failure recorded. Current failures: 5/5
Threshold reached! Transitioning to OPEN
Circuit opened! Will stay open until: 1767500502555 (current: 1767500497555)
call 5 -> cached-value (state=OPEN)
...
Timeout elapsed! Transitioning from OPEN to HALF_OPEN
HALF_OPEN: Making test call to check if service recovered...
HALF_OPEN test succeeded! Transitioning back to CLOSED
```

## 🧪 Testing

Run the test suite:

```bash
./mvnw test
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/parthpanchal/springdemo/demo/
│   │       ├── CircuitBreaker.java          # Main circuit breaker implementation
│   │       ├── CircuitBreakerDemo.java      # Demo/example usage
│   │       ├── DemoApplication.java         # Spring Boot application
│   │       ├── config/
│   │       │   └── AppConfig.java           # Configuration class
│   │       └── enums/
│   │           └── State.java               # Circuit breaker states
│   └── resources/
│       └── application.yml                  # Configuration file
└── test/
    └── java/
        └── .../DemoApplicationTests.java     # Unit tests
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## 🙏 Acknowledgments

- Inspired by the Circuit Breaker pattern from Michael Nygard's "Release It!" book
- Built with Spring Boot and Java

## 📚 Learn More

- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- For a detailed explanation, check out the [blog post](circuit-breaker-blog.md)

## ⚠️ Production Notes

For production use, consider using battle-tested libraries like:
- [Resilience4j](https://resilience4j.readme.io/)
- [Hystrix](https://github.com/Netflix/Hystrix) (deprecated, but still used)

This implementation is educational and demonstrates the core concepts. Building your own helps understand how these libraries work under the hood!

---

Made with ❤️ using Spring Boot and Java

