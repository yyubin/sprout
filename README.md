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
