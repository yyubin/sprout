# 빠른 시작

이 가이드에서는 Sprout Framework로 첫 번째 웹 애플리케이션을 만드는 방법을 알아봅니다.

## 전제 조건

- Java 21 이상
- Gradle 8.0 이상

## 프로젝트 설정

### 1. 프로젝트 생성

```bash
mkdir my-sprout-app
cd my-sprout-app
gradle init
```

### 2. 의존성 추가

`build.gradle`에 Sprout 의존성을 추가하세요:

```gradle
dependencies {
    implementation 'io.github.yyubin:sprout-framework:1.0.0'
}
```

### 3. 기본 애플리케이션 클래스 생성

```java
package com.example;

import sprout.web.annotation.SpringBootApplication;
import sprout.web.SpringApplication;

@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

### 4. 첫 번째 컨트롤러 생성

```java
package com.example.controller;

import sprout.web.annotation.RestController;
import sprout.web.annotation.GetMapping;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Sprout!";
    }
}
```

## 애플리케이션 실행

```bash
./gradlew run
```

브라우저에서 `http://localhost:8080/hello`에 접속하면 "Hello, Sprout!" 메시지를 볼 수 있습니다.

## 다음 단계

- [아키텍처 가이드](../architecture/ioc-container)에서 Sprout의 내부 구조를 알아보세요
- [예제 프로젝트](https://github.com/yyubin/sprout/tree/main/examples)를 살펴보세요
- [API 문서](/api/)를 참조하세요

축하합니다! 첫 번째 Sprout 애플리케이션을 성공적으로 만들었습니다. 🎉