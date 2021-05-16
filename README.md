# Spring WebFlux blocking example
_본 데모는 [JDrive Blog](https://blog.jdriven.com/2020/10/spring-webflux-reactor-meltdown-slow-responses/) 의 내용을 바탕으로 작성하였습니다._
<hr>

## 목적
- Event Loop Thread Blocking 발생 시, 해당 Thread만의 지연이 아닌 전체 서비스의 성능 저하를 발생시킴
- Demo를 통해 1개의 Worker를 강제로 Block 시킨 후, 전체 성능이 어떻게 저하되는지 확인
- Blockhound를 통한 Blocking Call을 탐지
- Block 된 Thread를 별도의 Scheduler에 위임하여, 전체 성능이 향상되는지 확인
- Blockhound의 구체적인 동작방식 및 예외 등록 방법 등을 소개

### 환경
```
reactor.netty.ioWorkerCount=4
```

### 서비스 구성
> /block/time/{sleepMs}<br>
> - PathVariable 으로 전달된 값 만큼 Thead Sleep 발생 (Blocking Call)

> /block/health
> - 즉시 200 OK를 응답으로 내림 (지연 X)

### 테스트 구성
1. ```/block/time/11000``` 을 호출하여 11초간 blocking되는 요청 발생
2. ```/block/health```를 10번 호출하여, blocking 되지 않는 쓰레드에서 정상 시간 내 응답이 오는지 확인

<hr>

### 테스트 결과
```sh
BlockingEndpointIT
health(int)             44s 827 ms
[6] nr=6                411 ms
[7] nr=7            10s 999 ms
[5] nr=5                411 ms
[2] nr=2                  1 ms
[1] nr=1            10s 999 ms
[4] nr=4            10s 993 ms
[3] nr=3            10s 997 ms
[8] nr=8                  8 ms
[9] nr=9            10s 993 ms
testBlocking()          10s 993 ms
```

## Blockhound
- [Blockhound](https://github.com/reactor/BlockHound) 를 설치하여, Blocking Call을 찾을 수 있다.
- Blockhound는 Java Agent로, 미리 정의된 blocking 메소드들이 호출될 때 오류를 발생시킨다.
- 해당 메소드들의 byte code를 바꿔치는 식으로 사용된다.
- 아래의 설정으로 의존성을 추가한다.<br>

### Gradle
~~~
repositories {
  mavenCentral()
  // maven { url 'https://repo.spring.io/milestone' }
  // maven { url 'https://repo.spring.io/snapshot' }
}

dependencies {
  testCompile 'io.projectreactor.tools:blockhound:$LATEST_RELEASE'
  // testCompile 'io.projectreactor.tools:blockhound:$LATEST_MILESTONE'
  // testCompile 'io.projectreactor.tools:blockhound:$LATEST_SNAPSHOT'
}
~~~

### Maven
~~~
<dependencies>
  <dependency>
    <groupId>io.projectreactor.tools</groupId>
    <artifactId>blockhound</artifactId>
    <version>$LATEST_RELEASE</version>
  </dependency>
</dependencies>
~~~

- 테스트 코드에 아래의 코드블록을 삽입 후 실행한다.
~~~JAVA
static {
    Blockhound.install();
}
~~~

### 결과
```
reactor.blockhound.BlockingOperationError: Blocking call! java.lang.Thread.sleep
at java.base/java.lang.Thread.sleep(Thread.java) ~[na:na]
Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
``` 
- 위의 오류가 발생하며, Blocking Call을 찾을 수 있다.

## 해결
- Blockhound를 다시 제거한 후, blocking call을 해결해보자.
```JAVA
return Mono.fromSupplier(() -> blockingFunction(sleepMs));
```
- 이 메소드는 Blocking Call이지만, 별도의 Scheduler를 지정해주지 않아 Event Loop 내에서 호출된다.
- 아래와 같이 별도의 데몬 스케쥴러를 지정하여, Event Loop 밖에서 처리해줄 수 있도록 변경한다.
```JAVA
return Mono.fromSupplier(() -> blockingFunction(sleepMs))
        .subscribeOn(Schedulers.boundedElastic());
```
### 결과
```sh
BlockingEndpointIT
health(int)
    [3] nr=3    1s 771 ms
    [6] nr=6        19 ms
    [1] nr=1       424 ms
    [5] nr=5        21 ms
    [4] nr=4       424 ms
    [2] nr=2         7 ms
    [8] nr=8         7 ms
    [9] nr=9        21 ms
    [7] nr=7    11s 38 ms
testBlocking()  11s 38 ms
```
- 위와 같이, 모든 요청이 Blocking 되지 않고 정상적인 시간 내에 도달함을 알 수 있다.
