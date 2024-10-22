# JAVA-CRUD
## 프로젝트 개요
본 프로젝트는 게시판 시스템으로, 사용자가 게시물을 생성, 수정, 삭제 및 조회할 수 있는 기능을 제공합니다. 사용자는 각 게시물에 대해 제목과 내용을 입력할 수 있으며 게시물은 특정 게시판에 소속됩니다.  
  
제가 Spring을 전에 공부한 경험이 있어서 Spring에서 주로 사용되는 개념들을 차용해서 만들어 보려고 노력하였습니다.

## 주요 기능 및 사용 예제
### 입력
실제로 웹에서 사용되는 HTTP의 Request, Response 형태를 따라해 만들어 보려고 했습니다.  
입력시 `반드시` 제가 미리 지정한 포맷으로 입력해야 정상적으로 작동합니다.  
제가 프로그램에서 상정한 HTTP Request의 형식은 아래와 같습니다.  
```plantuml
POST /accounts/signup HTTP/1.1
{"id": "id", "name": "pwd", "email": "email@email", "password": "pass"}
```
첫째 줄에 HTTP 메서드, 경로, HTTP 버전이 필요합니다.  
두번째 줄에 `json` 이라는 javascript 객체 표기법을 따라 입력해주어야 합니다. java의 map과 비슷한 형태입니다.
#### HTTP method
프로그램에서 지원하는 HTTP method는 아래와 같습니다.  
  * GET (Read)
  * POST (Create)
  * PUT (Update)
  * DELETE (Delete)   
  
Restful API의 명세를 따르려고 시도하였습니다.
### 출력
출력의 경우 미리 지정해주신대로 출력합니다. 내부적으로는 HTTP Response를 파싱하여 출력하도록 하여 실제 프론트엔드에서 사용하듯이 구현해보려고 했습니다.
### 회원 관리
#### 회원 가입

- 기능 설명 : 사용자가 회원가입을 할 수 있는 기능
- 입력

  - 회원 등록 dto (memberRegisterDto)
    
    - 회원 아이디 `id`
    - 회원 이름 `name`
    - 회원 이메일 `email`
    - 회원 비밀번호 `password`
- 출력 : 회원 가입 성공 메세지
- HTTP 메서드 : POST
- API 엔드포인트 : /accounts/signup
- 요청 예제
  ```plantuml
  POST /accounts/signup HTTP/1.1
  {"id": "id", "name": "pwd", "email": "email@email", "password": "pass"}
  ```

#### 회원 상세 정보

- 기능 설명 : 검색한 회원의 상세 정보를 볼 수 있는 기능
- 입력

  - 회원 아이디 `accountId` : 검색하고자 하는 회원의 아이디
- 출력
  
  - 계정 : 해당 회원의 아이디
  - 회원 : 해당 회원의 이름
  - 이메일 : 해당 회원의 이메일
  - 가입일 : 해당 회원이 가입한 날짜
- HTTP 메서드 : GET
- API 엔드포인트 : /accounts/detail
- 요청 예제

  ```plantuml
  GET /accounts/detail?accountId=id HTTP/1.1
  ```
#### 회원 정보 수정

- 기능 설명 : 회원의 정보를 수정할 수 있는 기능
- 입력

  - 회원 정보 수정 dto (memberUpdateDto)

    - 회원 이메일 `email`
    - 회원 비밀번호 `password`
- 출력 : 회원 수정 성공 메세지
- HTTP 메서드 : PUT
- API 엔드포인트 : /accounts/edit
- 요청 예제
  ```plantuml
  PUT /accounts/edit?accountId=id HTTP/1.1
  {"password": "pass1", "email": "yubin@email"}
  ```
#### 회원 탈퇴

- 기능 설명 : 로그아웃을 시킨 이후 회원 정보 삭제
- 입력
  
  - 회원 아이디 `accountId`
- 출력 : 회원 탈퇴 성공 메세지
- HTTP 메서드 : DELETE
- API 엔드포인트 : /accounts/remove
- 요청 예제
  ```plantuml
  DELETE /accounts/remove?accountId=id HTTP/1.1
  ```
#### 로그인

- 기능 설명 : 회원이 로그인 할 수 있는 기능
- 입력

  - 회원 로그인 dto (memberLoginDto)
  
    - 회원 아이디 `id`
    - 회원 비밀번호 `password`
- 출력 : 로그인 성공 메세지
- HTTP 메서드 : POST
- API 엔드포인트 : /accounts/signup
- 요청 예제

  ```plantuml
  POST /accounts/signin HTTP/1.1
  {"id": "id", "password": "pass"}
  ```
#### 로그아웃

- 기능 설명 : 회원이 로그아웃 할 수 있는 기능
- 입력 : none
- 출력 : 로그아웃 성공 메세지
- HTTP 메서드 : GET
- API 엔드포인트 : /accounts/signout
- 요청 예제

  ```plantuml
  GET /accounts/signout HTTP/1.1
  ```
### 게시판 관리

#### 게시판 추가  

- 기능 설명 : 관리자가 게시판을 추가할 수 있는 기능
- 입력

  - 게시판 이름 `boardName`
  - 게시판 설명 `description`
  - 게시판 권한 `grade`
  
    - 게시판 권한에 `USER`을 명시하지 않으면 게시판 내부의 글도 관리자만 접근 가능합니다
- 출력 : 게시판 생성 성공 메세지
- HTTP 메서드 : POST
- API 엔드포인트 : /boards/add
- 요청 예제
  ```plantuml
  POST /boards/add HTTP/1.1
  {"boardName": "게시판이름", "description": "게시판설명", "grade": "USER"}
  ```
  
#### 게시판 수정
- 기능 설명 : 관리자가 게시판을 수정할 수 있는 기능
- 입력

  - 게시판 아이디 `id`
  - 게시판 이름 `boardName`
  - 게시판 설명 `description`
  - 게시판 권한 `grade`

    - 게시판 권한에 `USER`을 명시하지 않으면 게시판 내부의 글도 관리자만 접근 가능합니다
- 출력 : 게시판 수정 성공 메세지
- HTTP 메서드 : PUT
- API 엔드포인트 : /boards/edit
- 요청 예제
  ```plantuml
  PUT /boards/edit?boardId=1 HTTP/1.1
  {"boardName": "새로운게시판이름", "description": "새로운게시판설명", "grade": "USER"}
  ```

#### 게시판 삭제
- 기능 설명 : 관리자가 게시판을 삭제할 수 있는 기능
- 입력

  - 게시판 아이디 `id`
- 출력 : 게시판 삭제 성공 메세지
- HTTP 메서드 : DELETE
- API 엔드포인트 : /boards/remove
- 요청 예제
  ```plantuml
  DELETE /boards/remove?boardId=id HTTP/1.1
  ```

#### 게시판 열람
- 기능 설명 : 사용자가 게시판을 열람할 수 있는 기능
- 입력

  - 게시판 이름 `boardName`
- 출력 : 게시판 삭제 성공 메세지
- HTTP 메서드 : GET
- API 엔드포인트 : /boards/view
- 요청 예제
  ```plantuml
  GET /boards/view?boardName=게시판이름 HTTP/1.1
  ```

### 게시물 관리
#### 게시물 추가
- 기능 설명 : 사용자가 게시물을 추가할 수 있는 기능
- 입력
  - 게시물 제목 `postName`
  - 게시물 내용 `postContent`
  - 게시판 ID `boardId`
- 출력 : 게시물 생성 성공 메세지
- HTTP 메서드 : POST
- API 엔드포인트 : /post/add
- 요청 예제
  ```plantuml
  POST /posts/add?boardId=1 HTTP/1.1  
  {"postName": "게시글제목", "postContent": "게시글내용"}
  ```
#### 게시물 수정
- 기능 설명 : 사용자가 기존 게시물을 수정할 수 있는 기능
- 입력
  - 게시물 ID `postId`
  - 게시판 ID `boardId`
  - 수정할 게시물 정보 `PostUpdateDTO`
- 출력 : 게시물 수정 성공 메세지
- HTTP 메서드 : PUT
- API 엔드포인드 : /post/edit
- 요청 예제
  ```plantuml
   PUT /posts/edit?postId=1&boardId=1 HTTP/1.1
   {"postName": "새로운제목", "postContent": "새로운내용"}
  ```

#### 게시물 삭제
- 기능 설명 : 사용자가 기존 게시물을 삭제할 수 있는 기능
- 입력
  - 게시물 ID `postId`
  - 게시판 ID `boardId`
- 출력 : 게시물 삭제 성공 메세지
- HTTP 메서드 : PUT
- API 엔드포인드 : /post/remove
- 요청 예제
  ```plantuml
   DELETE /posts/remove?boardId=1&postId=1 HTTP/1.1
  ```

#### 특정 게시물 열람
- 기능 설명 : 사용자가 기존 게시물을 열람할 수 있는 기능
- 입력
  - 게시물 ID `postId`
  - 게시판 ID `boardId`
- 출력 
  
  - 게시글 번호 : 게시글 번호
  - 작성일 : 게시글 작성일
  - 수정일 : 게시글 수정일
  - 제목 : 게시글 제목
  - 내용 : 게시글 내용
- HTTP 메서드 : GET
- API 엔드포인드 : /post/remove
- 요청 예제
  ```plantuml
   GET /posts/view?postId=1&boardId=1 HTTP/1.1
  ```

---

## 적용된 개념 소개 및 설명
### Container, 컨테이너 객체
객체가 사용될 때마다 생성되는게 유리한 경우도 있고(다른 데이터를 가지고 있다던지) 그렇지 않은 경우도 있습니다. 만약 재활용이 가능한 객체라면 한번 생성하여 계속 사용할 수 있게 하는게 좋은 구조이고 많은 프레임워크에서 실제로 사용되는 패턴입니다. 이를 구현하기 위해 `Container` 라는 객체를 만들고 저장소 역할을 하는 `Repository`
실제 비지니스 로직이 있는 `Service`, 실제적으로 보여주는 부분(`view`) 과 통신하는 `Controller` 를 각자 어노테이션으로 등록하고 리플렉션을 사용하여 패키지 이름을 기준으로 하위 파일을 읽어 해당하는 어노테이션이 있다면 미리 필요한 의존성을 주입하여 생성해두는 컴포넌트 스캐너를 만들어 두었습니다.  

만약 위에 설명한 레이어에 해당 되지 않는 별개의 유틸 기능을 하는 클래스 같은 경우는 `Component`의 이름으로 어노테이션을 지정하였고 이 역시 미리 생성해두어 생성된 인스턴스를 쓸 수 있도록 하였습니다.  

구현하며 고려한 것은 제가 실제로 참고한 스프링의 경우 컴포넌트를 스캔하여 의존성을 주입할 때 생성자 주입 방식을 한다는 것이라, 최초로 인스턴스가 만들어질 때 미리 필요한 객체의 인스턴스가 이미 있어야 했습니다. 그래서 컴포넌트를 스캔하는 순서가 중요해서 이를 제어하기 위해 `Priority` 어노테이션과 필요한 클래스가 어떤 것인지 명시 해두는 `Requires`
어노테이션을 사용하여 인위적으로 제어하며 생성할 수 있게 구성되어 있습니다.  

하지만 `RequestHandler` 를 구현하며 미리 생성된 모든 컨트롤러를 순회해야 했기 때문에 모든 컨트롤러가 주입되어 있어야 했기 때문에, 이 부분은 컴포넌트 스캔이 모두 끝나고, `setter` 를 사용하여 넣는 수정자 주입 방법을 사용했습니다.

### 리플렉션 활용
#### controller 로 매핑하기
컨트롤러 마다 `GetMapping`, `PostMapping`, `PutMapping`, `DeleteMapping` 을 정의하고 내부에 path 값을 넣어두도록 했습니다. `RequestHandler` 에서는 `ControllerInterface` 에 할당 가능한 클래스들을 가지고 있다가 요청이 들어오면 모든 컨트롤러를 순회하고 내부 메서드에 요청의 path 값과 어노테이션 내부의 path 값을 비교하여 맞는 메서드를 찾으면 해당 메서드를 실행할 수 있도록 리플렉션을 활용하여 구현하였습니다.
#### controller 의 매개변수에 맞춰 파라미터 할당하기
실제 스프링에서는 컨트롤러 매개변수에 모델이나 받고 싶은 자료형을 미리 넣어두면 매개변수의 이름으로 값을 매핑하여 넣어줍니다. 이걸 따라해보고 싶어서 `RequestHandler` 에서 맞는 메서드를 찾아 실행하기 전에 파라미터들의 정보를 모두 가져와 제가 만든 객체의 경우 `ObjectMapper` 로 파싱하여 넣어주도록 하였습니다. 그 외에는 현재 프로그램에서 `String` 과 `Long` 타입을 주로 사용하기에 여기까지는 지원하고 있습니다.
#### 프록시 패턴으로 보안 검사
이 또한 리플렉션으로 구성할 수 있었는데, 게시판의 생성, 수정, 삭제는 모두 관리자의 권한을 가진 회원만 가능합니다. 원래는 이를 위해 `BoardService` 에서 해당 회원이 `ADMIN` 권한을 가졌는지 확인하는 로직이 있었는데 비지니스 로직과 직접적인 관련이 적기 때문에 따로 처리하고 싶다는 생각이 있었습니다.  

실제 스프링과는 다르지만 비슷하게 구현해보려고 하였습니다. 우선은 프록시 객체(`MethodProxyHandler`)를 만들고 `BoardService` 생성에 필요한 인스턴스(의존성) 또한 프록시 객체 내부에 가지고 있습니다. 사실은 메서드(생성, 수정, 삭제) 하나씩을 프록시로 관리하는게 더 적절 했을 거 같지만 다른 방법이 떠오르지 않아 컴포넌트 스캔시
`BeforeAuthCheck` 라는 어노테이션이 있다면 해당 클래스 전체를 프록시로 감싸도록 처리했습니다.
만약 해당 클래스에 접근하면 프록시 객체에 먼저 접근하게 되고 지금 실행하려는 메서드에 또 `BeforeAuthCheck` 라는 어노테이션이 있다면
현재 로그인한 회원의 권한을 검사하고 만약 `ADMIN` 이 아니라면 메서드를 실행하지 못하도록 구성하였습니다.

### MVC 패턴
실제 저장소에 해당되는 `Repository`, 비지니스 로직이 있는 `Service`, 사용자에게 직접 노출되는 `view`, `view` 에서 받은 요청을 처리하고 응답 객체를 만드는 `Controller`로 구분하여 MVC 패턴을 구현하고자 했습니다.

MVC 패턴에서 M은 비지니스 로직과 데이터 처리를 담당하므로 `domain`, `repository`, `service` 패키지에 해당하는 객체들이 해당 될 것 같습니다.

V는 사용자에게 데이터를 보여주는 프레젠테이션 계층이므로 실질적으로 output을 담당하는 `PrintProcessor` 가 해당될 것 같습니다.

C는 사용자의 입력을 받고 Model과 View를 중재하는 역할을 하므로 `controller` 패키지 하위 객체들이 해당할 것 같습니다.
### RedisClient로 세션 구현
### 비밀번호 암호화
실제로 비밀번호는 데이터베이스에 암호화된 채로 저장되기 때문에 실제로 암호화 과정을 거쳐 저장이 되도록 구현하였습니다. 회원가입시 처음부터 암호화를 시켜 저장되고 로그인 시에도 사용자가 입력한 `password`를 암호화하여 비교하도록 처리하였습니다.

---

## 트러블 슈팅
### 1. 의존성 주입시 주입한 객체가 null..
처음 컴포넌트 스캐너를 만들었을 경우 `Requires` 어노테이션을 살펴보며 해당하는 인스턴스를 넣어서 주입 후 생성하도록 했는데 테스트 코드에서 계속
null이 발생하였습니다. 컴포넌트 스캐너를 디버깅하며 살펴보니 제가 최초에 `repository` 패키지 부터 생성하도록 했지만(`repository`는 의존하는 객체가 없습니다) `service`에서
다른 `service`를 사용하는 경우 다른 `service`가 먼저 생성 되었어야 했습니다. 그래서 `@Service` 어노테이션 내부에 값을 주고 이를 sorting 한 이후 그 순서대로 객체를 생성하도록 했습니다.
후에는 `util` 패키지에서도 객체 생성 순서가 필요한 경우가 생겨 `@Priority` 어노테이션을 따로 만들어 처리하였습니다.
### 2. 프록시 객체 사용시 인터페이스로만 할당 가능
제가 사용한 프록시는 자바의 동적 프록시 입니다. 자바의 다이나믹 프록시 같은 경우 원래 구현체로 캐스팅이 불가한 이슈가 있었습니다.
저는 이를 몰랐지만 에러 메세지를 보고 알게 되었습니다. 다이나믹 프록시는 런타임에 인터페이스를 기반으로 생성된 객체 이기 때문에
실제 구현체에 대한 정보를 가질 수 없다고 합니다. 그리고 제 컴포넌트 스캐너에서는 인터페이스에 대한 주입은 따로 하지 않았기 때문에
구현체로 캐스팅하려면 캐스팅 불가 에러가 뜨고, 인터페이스로 캐스팅하면 null 이 발생하는 오류였습니다. 이를 해결하기 위해 컴포넌트 스캐너에서
주입을 할때 해당 구현체에 대한 인터페이스를 가져와서 인터페이스에도 인스턴스를 주입해주는 방법으로 해결하였습니다.
### 3. 콘솔에 입력시 이전에 작성했던 값이 들어감
비지니스 로직의 경우 컨테이너 객체도 활용한 serviceTests 들을 미리 만들어 두었고, controller에서도 테스트를 완료 하였는데,
`ADMIN` 계정으로 유저도 사용가능한 게시판을 만들고 나서 운영자용 게시판을 만들면 기존 값으로 계속 들어가는 오류가 있었습니다.
이를 디버깅했더니 `service`, `requestHandler` 등등 모든 곳에서 기존 값이 계속 들어가길래 굉장히 당황스러웠습니다. 하지만 테스트 코드에서는 
의도한대로 동작함을 확인하고 콘솔에 입력시 버퍼를 비우지 않아서 생긴 문제임을 확인하였습니다.





