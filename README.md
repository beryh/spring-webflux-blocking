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
```
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
