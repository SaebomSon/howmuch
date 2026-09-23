# 엔티티 어노테이션 정리 (howmuch 프로젝트)

이 문서는 `Trip`, `Participant`, `Expense`, `ExpenseShare`, `Transfer` 엔티티에 쓴 어노테이션을 정리한 것이다.
각 항목은 **무엇을 하는지 → 왜 이렇게 썼는지 → 면접에서 나오는 질문** 순서로 되어 있다.

---

## 0. 어노테이션이란

`@`로 시작하는 표시로, 그 자체로는 아무 동작도 하지 않는다.
**다른 프로그램(프레임워크, 컴파일러)에게 남기는 메모**에 가깝다.

- `@Entity` → JPA가 읽고 "이 클래스를 테이블로 관리하자"라고 판단한다.
- `@Getter` → Lombok이 읽고 컴파일 시점에 getter 메서드 코드를 만들어 넣는다.

즉 어노테이션은 **누가 읽느냐**에 따라 역할이 갈린다. 이 프로젝트에서 읽는 주체는 세 곳이다.

| 읽는 주체 | 해당 어노테이션 | 언제 동작하나 |
|---|---|---|
| JPA / Hibernate | `@Entity`, `@Id`, `@Column`, `@ManyToOne` 등 | 실행 중 (테이블 생성, SQL 발행) |
| Lombok | `@Getter`, `@NoArgsConstructor` | 컴파일 시점 (코드 생성) |
| Hibernate 전용 | `@CreationTimestamp` | 실행 중 (저장 직전 값 주입) |

> **용어 정리**
> - **JPA**: 자바 객체와 DB 테이블을 연결하는 방법을 정해둔 표준(명세). 인터페이스만 정의되어 있다.
> - **Hibernate**: 그 표준을 실제로 구현한 라이브러리. Spring Boot의 JPA 스타터는 기본으로 Hibernate를 쓴다.
> - **ORM**: Object-Relational Mapping. 객체와 관계형 DB를 이어주는 기술 전반을 뜻한다.

---

## 1. 기본 매핑 어노테이션

### `@Entity`

```java
@Entity
public class Trip { ... }
```

이 클래스를 DB 테이블과 연결한다. JPA가 관리하는 대상이 되어, 저장·조회·수정·삭제가 가능해진다.
테이블 이름을 따로 지정하지 않으면 **클래스 이름이 그대로 테이블 이름**이 된다 (`Trip` → `TRIP`).
카멜 케이스는 스네이크 케이스로 바뀐다 (`ExpenseShare` → `EXPENSE_SHARE`).

**조건**
- 기본 생성자가 반드시 있어야 한다 (뒤의 `@NoArgsConstructor` 참고).
- `final` 클래스면 안 된다. Hibernate가 프록시 객체를 만들기 위해 상속해야 하기 때문이다.

**면접 포인트**
> "`@Entity`가 붙은 클래스에 기본 생성자가 필요한 이유는?"
> Hibernate가 DB에서 조회한 결과로 객체를 만들 때, 리플렉션으로 빈 객체를 생성한 뒤 필드에 값을 채우기 때문이다.

---

### `@Id`

```java
@Id
private Long id;
```

이 필드가 **기본키(Primary Key)**임을 알린다. 각 행을 구분하는 고유 값이다.
엔티티에는 반드시 하나 있어야 한다.

**타입을 `Long`으로 쓰는 이유**
- `long`(기본형)은 값이 없을 때 자동으로 `0`이 된다. 그러면 "아직 저장 전이라 id가 없는 상태"와 "id가 0인 상태"를 구분할 수 없다.
- `Long`(참조형)은 `null`을 담을 수 있어서, 저장 전에는 `null`로 둘 수 있다.
- JPA도 `isNew()` 판단에 `null` 여부를 쓴다.

---

### `@GeneratedValue`

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

id 값을 **DB가 자동으로 매기도록** 한다. 직접 1, 2, 3을 넣지 않아도 된다.

**전략 네 가지**

| 전략 | 동작 | 비고 |
|---|---|---|
| `IDENTITY` | DB의 auto_increment 기능을 사용 | MySQL, H2에서 가장 흔함 |
| `SEQUENCE` | DB의 시퀀스 객체를 사용 | Oracle, PostgreSQL |
| `TABLE` | id 관리용 테이블을 따로 둠 | 느려서 거의 안 씀 |
| `AUTO` | DB에 맞춰 위 중 하나를 자동 선택 | 기본값 |

**`IDENTITY`를 고른 이유**
지금은 H2, 나중에는 MySQL을 쓸 예정인데 둘 다 auto_increment를 지원한다. 설정이 가장 단순하다.

**면접 포인트**
> "`IDENTITY` 전략의 단점은?"
> id 값을 DB가 정하기 때문에, `persist()` 시점에 **즉시 INSERT 쿼리가 나간다.**
> 그래서 여러 INSERT를 모아 한 번에 보내는 쓰기 지연(batch insert) 최적화를 쓸 수 없다.
> 대량 삽입이 중요한 상황이라면 `SEQUENCE`가 유리하다.

---

### `@Column`

```java
@Column(nullable = false, length = 50)
private String name;
```

컬럼의 세부 조건을 지정한다. **생략해도 컬럼은 만들어진다.** 조건을 걸 필요가 있을 때만 붙인다.
`Expense`의 `spentOn`, `memo`에 안 붙인 이유가 이것이다. 선택 입력값이라 제약이 없다.

**자주 쓰는 속성**

| 속성 | 의미 | 예시 |
|---|---|---|
| `nullable = false` | NOT NULL 제약을 건다 | 이름 없는 여행은 존재할 수 없다 |
| `length = 50` | 문자열 최대 길이 (기본 255) | 여행 이름은 50자면 충분 |
| `updatable = false` | 최초 저장 후 수정 불가 | 생성 시각은 바뀌면 안 된다 |
| `unique = true` | 해당 컬럼 단독 유니크 제약 | 단일 컬럼일 때만 사용 |
| `name = "..."` | 컬럼 이름을 직접 지정 | 기본은 필드명의 스네이크 케이스 |

**`nullable = false`를 쓰는 진짜 이유**
자바 코드에서 검사하는 것만으로는 부족하다. DB에 직접 접속해서 INSERT 하거나, 다른 애플리케이션이 같은 DB를 쓰는 경우까지 막으려면 **DB 차원의 제약**이 필요하다.
애플리케이션 검증은 "친절한 안내", DB 제약은 "마지막 방어선"이라고 생각하면 된다.

**주의**
`@Column(nullable = false)`는 DDL을 만들 때만 쓰인다. `ddl-auto`가 꺼져 있고 테이블이 이미 있다면 이 설정은 아무 효과가 없다.
입력값 검증은 `@NotNull`(Bean Validation)이 담당하며, 이건 나중에 DTO에 붙일 예정이다.

---

### `@Table`

```java
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_participant_trip_name",
        columnNames = {"trip_id", "name"}))
public class Participant { ... }
```

테이블 **전체**에 거는 설정이다. 이름 변경(`name = "..."`)이나 복합 제약을 걸 때 쓴다.

여기서는 **"같은 여행 안에서 참가자 이름은 중복될 수 없다"**는 규칙을 걸었다.
`name`만 유니크하게 하면 다른 여행에 있는 동명이인까지 막히므로, `trip_id`와 `name`을 묶은 **복합 유니크 제약**이 필요하다.
이런 복합 제약은 `@Column(unique = true)`로는 표현할 수 없다.

`name = "uk_participant_trip_name"`은 제약조건의 이름이다. 지정하지 않으면 `UK_a8f3d9...` 같은 임의 문자열이 붙어서, 나중에 제약 위반 에러 로그를 봤을 때 어떤 규칙이 깨졌는지 알기 어렵다.

**면접 포인트**
> "서비스 코드에서 중복 검사를 하는데 DB 제약도 필요한가요?"
> 필요하다. 두 요청이 동시에 들어오면 둘 다 "중복 없음"으로 통과한 뒤 둘 다 INSERT 될 수 있다.
> 이런 경쟁 상태(race condition)는 DB 유니크 제약으로만 확실히 막을 수 있다.

---

## 2. 연관관계 어노테이션

### `@ManyToOne`

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "trip_id", nullable = false)
private Trip trip;
```

**다대일** 관계다. 참가자 여러 명(Many)이 여행 하나(One)에 속한다.
이 어노테이션이 붙은 쪽 테이블에 상대방의 id를 담는 **외래키 컬럼**이 생긴다. 즉 `PARTICIPANT` 테이블에 `trip_id` 컬럼이 만들어진다.

외래키를 가진 쪽을 **연관관계의 주인**이라고 부른다. 관계를 맺고 끊는 권한이 이쪽에 있다.

**속성**
- `fetch = FetchType.LAZY`: 아래 별도 항목 참고.
- `optional = false`: 이 연결은 반드시 있어야 한다. 소속 여행이 없는 참가자는 성립하지 않는다. NOT NULL 제약이 걸리고, Hibernate가 내부 최적화에도 이 정보를 활용한다.

---

### `@JoinColumn`

```java
@JoinColumn(name = "sender_id", nullable = false)
private Participant sender;
```

외래키 컬럼의 이름을 직접 정한다. 생략하면 `필드명_상대PK명` 규칙으로 자동 생성된다(`sender_id`가 되긴 한다).

**직접 쓰는 것을 권하는 이유**
`Transfer`처럼 **같은 엔티티를 두 번 참조**하는 경우, 컬럼 이름이 명확해야 한다.

```java
private Participant sender;    // sender_id
private Participant receiver;  // receiver_id
```

두 필드 모두 `Participant`를 가리키지만 의미가 전혀 다르다. 컬럼 이름을 직접 지정하면 DB만 봐도 구조가 읽힌다.

---

### `@OneToMany`와 `mappedBy`

```java
// Expense.java
@OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ExpenseShare> shares = new ArrayList<>();
```

**일대다** 관계다. 지출 하나에 분담자 여러 명이 붙는다.

**`mappedBy`가 가장 헷갈리는 부분이다.**

핵심은 이것이다. DB에서 두 테이블의 관계는 **외래키 컬럼 하나**로만 표현된다. 여기서는 `EXPENSE_SHARE.expense_id`가 전부다.
그런데 자바 코드에서는 양쪽에서 서로를 참조할 수 있다.

```java
expense.getShares()       // 지출 → 분담자 목록
expenseShare.getExpense() // 분담자 → 지출
```

방향은 둘인데 실제 컬럼은 하나뿐이다. 그래서 JPA는 **"둘 중 누가 진짜 주인인가"**를 정해야 한다.

- 외래키를 **가진 쪽**(`ExpenseShare.expense`)이 주인이다. 이쪽 값이 바뀌어야 DB가 바뀐다.
- 반대쪽(`Expense.shares`)은 주인이 아니며, `mappedBy`로 그 사실을 선언한다.

`mappedBy = "expense"`의 `"expense"`는 **상대 클래스(`ExpenseShare`)에 있는 필드 이름**이다. 테이블명이나 컬럼명이 아니다.
"나는 주인이 아니고, `ExpenseShare`의 `expense` 필드가 이 관계를 관리한다"는 뜻이다.

**`mappedBy`를 빠뜨리면 생기는 일**
JPA가 양쪽을 서로 다른 두 개의 관계로 오해한다. 그러면 `EXPENSE_SHARE.expense_id`와는 별개로 **중간 테이블(`EXPENSE_SHARES`)을 추가로 만들어버린다.** 테이블이 의도와 다르게 생성되면 이걸 의심해야 한다.

**면접 포인트**
> "연관관계의 주인을 어떻게 정하나요?"
> 외래키를 가진 쪽, 즉 `@ManyToOne`이 있는 쪽이 주인이다.
> 주인이 아닌 쪽에 값을 넣어도 DB에는 반영되지 않는다. 이걸 모르면 "분명 목록에 추가했는데 저장이 안 된다"는 버그를 만나게 된다.

---

### `cascade`와 `orphanRemoval`

```java
@OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ExpenseShare> shares = new ArrayList<>();
```

**`cascade = CascadeType.ALL` (영속성 전이)**
지출에 한 작업을 분담자에게도 그대로 전파한다.

```java
expense.addShare(참가자A);
expense.addShare(참가자B);
expenseRepository.save(expense);   // 분담자 2건도 함께 INSERT 된다
```

`cascade`가 없으면 `ExpenseShare`를 하나씩 따로 저장해야 하고, 빠뜨리면 `TransientObjectException`이 난다.

주요 옵션은 `PERSIST`(저장), `MERGE`(수정), `REMOVE`(삭제), `ALL`(전부)이다.

**`orphanRemoval = true` (고아 객체 제거)**
부모와의 연결이 끊긴 자식을 DB에서도 지운다.

```java
expense.getShares().remove(0);   // 목록에서 제거하면 DELETE 쿼리가 나간다
```

이게 없으면 목록에서만 빠지고 DB에는 그대로 남아, 다음 조회 때 되살아난다.

**둘을 쓸 수 있는 조건**
자식이 **오직 이 부모에게만 속할 때**만 안전하다.
`ExpenseShare`는 특정 지출에만 속하므로 조건을 만족한다.
반면 `Participant`는 지출·이체 등 여러 곳에서 참조되므로, 여기에 `cascade = REMOVE`를 걸면 지출 하나 지웠다가 참가자까지 사라지는 사고가 난다.

**면접 포인트**
> "`cascade = REMOVE`와 `orphanRemoval = true`의 차이는?"
> `cascade = REMOVE`는 **부모를 삭제할 때** 자식도 삭제한다.
> `orphanRemoval`은 부모가 살아 있어도 **컬렉션에서 빠지기만 하면** 자식을 삭제한다. 적용 범위가 더 넓다.

---

### `FetchType.LAZY` vs `EAGER`

```java
@ManyToOne(fetch = FetchType.LAZY)
private Trip trip;
```

연관된 객체를 **언제 DB에서 가져올지** 정한다.

- **EAGER (즉시 로딩)**: 참가자를 조회하는 순간 여행 정보까지 JOIN해서 함께 가져온다.
- **LAZY (지연 로딩)**: 참가자만 가져온다. `participant.getTrip().getName()`처럼 **실제로 사용하는 순간** 추가 쿼리를 날린다.

**기본값에 주의해야 한다.**

| 관계 | 기본 fetch | 바꿔야 하나 |
|---|---|---|
| `@ManyToOne` | **EAGER** | 반드시 LAZY로 바꾼다 |
| `@OneToOne` | **EAGER** | 반드시 LAZY로 바꾼다 |
| `@OneToMany` | LAZY | 그대로 둔다 |
| `@ManyToMany` | LAZY | 그대로 둔다 |

**왜 LAZY로 바꾸나 (N+1 문제)**
지출 100건을 조회한다고 하자. `Expense`에는 `trip`과 `payer`가 EAGER로 걸려 있다면,

1. 지출 100건을 가져오는 쿼리 1번
2. 각 지출의 여행을 가져오는 쿼리 100번
3. 각 지출의 결제자를 가져오는 쿼리 100번

총 201번의 쿼리가 나간다. 목록 하나 보여주는 데 이만큼의 쿼리가 나가면 서비스가 버티지 못한다.
이것이 **N+1 문제**이며, JPA에서 가장 자주 나오는 성능 이슈다.

LAZY로 두면 일단 1번만 나가고, 정말 필요한 데이터는 나중에 **fetch join**이나 `@EntityGraph`로 한 번에 가져오도록 쿼리를 직접 짜면 된다.

**LAZY의 주의점 (LazyInitializationException)**
LAZY는 실제 객체 대신 **프록시**라는 가짜 객체를 넣어둔다. 값을 꺼낼 때 DB를 조회하는데, 이때 **영속성 컨텍스트가 살아 있어야** 한다.
트랜잭션이 끝난 뒤(예: 컨트롤러에서 엔티티를 그대로 JSON으로 변환할 때) 프록시를 건드리면 `LazyInitializationException`이 난다.
그래서 엔티티를 그대로 응답에 쓰지 않고 **DTO로 변환해서 반환**하는 것이 원칙이다. 이 프로젝트에서도 API 단계에서 DTO를 따로 만들 예정이다.

**면접 포인트**
> "왜 `@ManyToOne`을 LAZY로 바꾸나요?"
> 기본값이 EAGER라서 의도치 않은 조인과 N+1 문제가 생긴다.
> 전부 LAZY로 두고, 필요한 곳에서만 fetch join으로 한 번에 가져오는 방식이 예측 가능하고 안전하다.

---

## 3. Lombok 어노테이션

Lombok은 반복 코드를 **컴파일 시점에 자동 생성**해주는 라이브러리다.
소스 코드에는 안 보이지만 컴파일된 `.class` 파일에는 실제로 들어간다.
IntelliJ 설정의 "어노테이션 처리 활성화"가 이 동작을 위한 것이다.

### `@Getter`

모든 필드의 getter를 만들어준다. `getId()`, `getName()`, `getAmount()` 등이 자동으로 생긴다.

### `@Setter`를 쓰지 않은 이유

**의도적으로 뺐다.** setter가 열려 있으면 어디서든 값을 바꿀 수 있어서, 데이터가 언제 어디서 변경됐는지 추적할 수 없다.

```java
trip.setName("제주도");      // 누가, 왜 바꿨는지 알 수 없다
trip.changeName("제주도");   // "이름 변경"이라는 의도가 드러난다
```

값 변경이 필요해지면 `changeName()`처럼 **의미가 드러나는 메서드**를 그때 추가한다. 이를 "엔티티를 setter 없이 설계한다"고 표현하며, 실무와 면접 모두에서 선호되는 방식이다.

### `@NoArgsConstructor(access = AccessLevel.PROTECTED)`

매개변수 없는 생성자를 만들되 접근 범위를 `protected`로 제한한다.

- **왜 필요한가**: JPA가 DB 조회 결과로 객체를 만들 때 기본 생성자가 반드시 필요하다.
- **왜 `public`이 아닌가**: `new Trip()`으로 이름도 없는 빈 여행을 아무나 만들 수 있으면 곤란하다. 객체는 항상 유효한 상태로 태어나야 한다.
- **왜 `private`이 아닌가**: Hibernate가 프록시를 만들 때 이 클래스를 상속하는데, 그러려면 최소 `protected`여야 한다.

`protected`는 "JPA는 쓸 수 있고, 개발자는 실수로 쓸 수 없는" 딱 그 경계다.

### 생성자를 직접 작성한 이유

```java
public Expense(Trip trip, String title, Long amount, Participant payer,
               LocalDate spentOn, String memo) { ... }
```

`@AllArgsConstructor`를 쓰지 않고 직접 썼다. 자동 생성 생성자는 **필드 순서에 의존**하기 때문이다.
나중에 필드 순서를 바꾸면 호출부는 컴파일 에러 없이 조용히 잘못된 값이 들어갈 수 있다. 특히 `String`이 연속될 때 위험하다.
객체 생성은 가장 중요한 지점이라 직접 관리하는 편이 안전하다.

### `ExpenseShare`의 생성자에 접근 제어자가 없는 이유

```java
ExpenseShare(Expense expense, Participant participant) { ... }
```

`public`이 없으면 **같은 패키지에서만** 접근할 수 있다(package-private).
분담자는 반드시 `expense.addShare(참가자)`를 거쳐 만들어지게 하려는 의도다. 이렇게 통로를 하나로 좁히면, 관계를 맺는 로직이 한곳에 모여 실수가 줄어든다.

---

## 4. Hibernate 전용 어노테이션

### `@CreationTimestamp`

```java
@CreationTimestamp
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;
```

저장되는 순간의 시각을 Hibernate가 자동으로 넣어준다. `updatable = false`와 함께 써서 이후 수정도 막는다.

IntelliJ가 "한 번도 대입되지 않았습니다"라고 경고하는데, 코드상 대입이 없으니 맞는 말이다. 실제로는 Hibernate가 저장 직전에 값을 채우므로 문제없다.

**참고**: 이건 JPA 표준이 아니라 **Hibernate 전용**이다. JPA 표준 방식은 `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`(Spring Data JPA)를 쓰는 것이다.
수정 시각까지 필요해지면 그때 Spring Data JPA Auditing으로 바꾸는 편이 낫다.

---

## 5. 직접 확인해보기

가장 빨리 익히는 방법은 어노테이션을 바꿔보고 **생성되는 SQL을 비교**하는 것이다.
`application.yml`의 `show-sql: true` 덕분에 콘솔에 `create table` 문이 그대로 찍힌다.

실험해볼 것들:

1. `@Column(nullable = false)`를 지우고 실행 → `not null`이 사라지는지 확인
2. `@Column(length = 50)`을 지우고 실행 → `varchar(255)`로 바뀌는지 확인
3. `@OneToMany`의 `mappedBy`를 지우고 실행 → 중간 테이블이 생기는지 확인
4. `@JoinColumn(name = "sender_id")`를 지우고 실행 → 컬럼 이름이 어떻게 붙는지 확인

확인 후에는 반드시 원래대로 되돌린다.

IntelliJ에서 어노테이션 이름에 커서를 두고 `F1`(맥에서는 `fn + F1`)을 누르면 공식 문서가 바로 뜨고,
`⌘ + 클릭`으로 해당 어노테이션의 소스 코드까지 들어가볼 수 있다.

---

## 6. 한눈에 보기

| 어노테이션 | 한 줄 요약 |
|---|---|
| `@Entity` | 이 클래스를 DB 테이블로 관리한다 |
| `@Id` | 이 필드가 기본키다 |
| `@GeneratedValue(IDENTITY)` | id를 DB의 auto_increment로 자동 생성한다 |
| `@Column` | 컬럼의 제약조건(NOT NULL, 길이 등)을 지정한다 |
| `@Table(uniqueConstraints)` | 여러 컬럼을 묶은 유니크 제약을 건다 |
| `@ManyToOne` | 다대일 관계. 이쪽 테이블에 외래키가 생긴다 |
| `@JoinColumn` | 외래키 컬럼 이름을 직접 지정한다 |
| `@OneToMany(mappedBy)` | 일대다 관계. 주인이 아님을 선언한다 |
| `fetch = LAZY` | 연관 객체를 실제 사용할 때 가져온다 (N+1 방지) |
| `cascade = ALL` | 부모의 저장·삭제를 자식에게 전파한다 |
| `orphanRemoval = true` | 컬렉션에서 빠진 자식을 DB에서도 지운다 |
| `optional = false` | 이 연관관계는 필수다 |
| `@Getter` | getter 메서드를 자동 생성한다 (Lombok) |
| `@NoArgsConstructor(PROTECTED)` | JPA용 기본 생성자를 만들되 외부 사용을 막는다 |
| `@CreationTimestamp` | 저장 시각을 자동으로 채운다 (Hibernate) |

---

## 7. 더 공부하면 좋은 순서

1. **영속성 컨텍스트**: 1차 캐시, 변경 감지(dirty checking), 쓰기 지연. JPA 동작의 핵심이자 면접 1순위 주제다.
2. **N+1 문제와 해결법**: fetch join, `@EntityGraph`, `@BatchSize`
3. **`@Transactional`**: 트랜잭션 범위와 전파 속성
4. **DTO 변환**: 엔티티를 API 응답에 직접 쓰면 안 되는 이유
