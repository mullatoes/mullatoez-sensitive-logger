# Mullatoez Sensitive Logger

A logging-only sensitive data masking library for Java 17+ and Spring Boot 3.

It masks fields annotated with `@Sensitive` when, and only when, you explicitly log them through `MullatoezSensitiveLogger`. It does **not** modify your source objects, API responses, DTOs, JSON serialization, or any business logic. Your application keeps working with the real values; only the log output is masked.

---

## Requirements

- Java 17+
- Spring Boot 3.x (for the auto-configured starter)
- Jackson (pulled in transitively by the starter)

---

## Installation

### Maven (Spring Boot)

```xml
<dependency>
    <groupId>com.mullatoez.security</groupId>
    <artifactId>mullatoez-sensitive-logger-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Spring Boot)

```groovy
implementation 'com.mullatoez.security:mullatoez-sensitive-logger-spring-boot-starter:1.0.0'
```

Kotlin DSL:

```kotlin
implementation("com.mullatoez.security:mullatoez-sensitive-logger-spring-boot-starter:1.0.0")
```

### Core only (plain Java, no Spring)

If you don't use Spring Boot and just want the masking engine and the `@Sensitive` annotation, depend on the core module directly.

Maven:

```xml
<dependency>
    <groupId>com.mullatoez.security</groupId>
    <artifactId>mullatoez-sensitive-logger-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.mullatoez.security:mullatoez-sensitive-logger-core:1.0.0'
```

---

## Usage

### 1. Annotate sensitive fields

Add `@Sensitive` to fields (or record components) you want masked in logs.

```java
import com.mullatoez.security.logger.core.Sensitive;
import com.mullatoez.security.logger.core.SensitiveMode;

public record UserDto(
        String id,
        @Sensitive String email,
        @Sensitive(mode = SensitiveMode.FULL) String password,
        @Sensitive(mode = SensitiveMode.PARTIAL) String phoneNumber
) {}
```

Works the same way on regular POJO fields:

```java
public class UserDto {
    private String id;

    @Sensitive
    private String email;

    @Sensitive(mode = SensitiveMode.FULL)
    private String password;

    // getters / setters / constructors...
}
```

`SensitiveMode` values:

| Mode      | Behavior                                                                 |
|-----------|--------------------------------------------------------------------------|
| `DEFAULT` | Inherits the global default (configured via properties).                 |
| `PARTIAL` | Keeps a few characters visible; masks the rest (e.g. `j***@g****.com`).  |
| `FULL`    | Replaces the entire value with `********`.                               |

### 2. Inject and use `MullatoezSensitiveLogger`

The starter auto-configures a `MullatoezSensitiveLogger` bean. Inject it wherever you log sensitive payloads.

```java
import com.mullatoez.security.logger.spring.MullatoezSensitiveLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final MullatoezSensitiveLogger sensitiveLogger;

    public UserService(MullatoezSensitiveLogger sensitiveLogger) {
        this.sensitiveLogger = sensitiveLogger;
    }

    public void register(UserDto user) {
        log.info("Registering user: {}", sensitiveLogger.mask(user));
        // user.email() / user.password() still hold the real values
    }
}
```

Two `mask` overloads are available:

```java
String mask(Object source);                    // uses the configured default mode
String mask(Object source, boolean maskFull);  // true = FULL, false = PARTIAL
```

### 3. (Optional) Configure via `application.yml` / `application.properties`

```yaml
mullatoez:
  sensitive-logger:
    enabled: true              # default: true; if false, mask() returns the raw String.valueOf(source)
    mask-full-by-default: false # default: false (partial); true means @Sensitive defaults to FULL
    output-json: true          # default: true; if false, output is the map's toString()
```

Equivalent `application.properties`:

```properties
mullatoez.sensitive-logger.enabled=true
mullatoez.sensitive-logger.mask-full-by-default=false
mullatoez.sensitive-logger.output-json=true
```

---

## What gets masked

- Fields and record components annotated with `@Sensitive` on POJOs and records.
- Inherited fields (the masker walks the class hierarchy).
- Nested objects, collections, and maps are traversed recursively.
- `static`, `transient`, and synthetic fields are skipped.

### What does **not** trigger masking

- Map keys are **not** used to infer sensitivity. A field is masked only if it carries `@Sensitive`. A `Map<String, String>` entry named `"password"` will not be masked unless it itself is the value of a `@Sensitive` field.
- Plain `String`/`Number`/etc. passed directly to `mask(...)` are returned as-is (there is no annotation to read).

### Safety guards

- Maximum traversal depth of 5 — deeper graphs are replaced with `[MAX_DEPTH_REACHED]`.
- Cycles are detected and rendered as `[CIRCULAR_REFERENCE]`.
- Fields that cannot be read reflectively are rendered as `[UNREADABLE_FIELD]`.

### Masking examples (partial mode)

| Input                  | Output            |
|------------------------|-------------------|
| `jane.doe@gmail.com`   | `j***@g****.com`  |
| `Str0ngP@ssw0rd!`      | `Str0****rd!`     |
| `12345678`             | `12****8`         |
| `abcd`                 | `a****`           |
| `ab`                   | `********`        |

In `FULL` mode every non-blank value becomes `********`.

---

## Using the core module without Spring

```java
import com.mullatoez.security.logger.core.SensitiveObjectMasker;

SensitiveObjectMasker masker = new SensitiveObjectMasker();
Object masked = masker.mask(user, /* maskFull = */ false);
// Serialize `masked` with your preferred JSON library, or call toString().
```

---

## License

MIT
