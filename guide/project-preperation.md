![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

`Querydsl`을 사용하기 위해 프로젝트 설정부터 차근차근 달려봅시다!

먼저 자바 버전은 `11`, 스프링 버전은 `2.5.2`를 선택하였고 `gradle` 프로젝트로 생성하여 아래 네 가지 `dependency`를 설정하였습니다.

* `spring-boot-starter-web`
* `spring-boot-starter-data-jpa`
* `lombok`
* `h2`

### build.gradle

위와 같이 설정하셨다면 `build.gradle` 파일이 아래 처럼 작성되었을텐데요,

```groovy
plugins {
    id 'org.springframework.boot' version '2.5.2'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
}

group = 'io.lcalmsky'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}

```

여기에 `Querydsl` 사용을 위한 설정들을 추가해야 합니다.

```groovy
plugins {
    id 'org.springframework.boot' version '2.5.2'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id "com.ewerk.gradle.plugins.querydsl" version "1.0.10" // (1)
    id 'java'
}

group = 'io.lcalmsky'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.querydsl:querydsl-jpa' // (2)
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}

def querydslDir = "$buildDir/generated/querydsl" // (3)

querydsl { // (4)
    jpa = true
    querydslSourcesDir = querydslDir
}

sourceSets { // (5)
    main.java.srcDir querydslDir
}

configurations { // (6)
    querydsl.extendsFrom compileClasspath
}

compileQuerydsl { // (7)
    options.annotationProcessorPath = configurations.querydsl
}
```

> (1) `querydsl` 플러그인을 추가합니다.  
> (2) 라이브러리 `dependency`를 추가합니다.  
> (3) `querydsl`에서 사용할 경로를 선언합니다.  
> (4) `querydsl` 설정을 추가합니다. `JPA` 사용 여부와 사용할 경로를 지정하였습니다.  
> (5) `build`시 사용할 `sourceSet`을 추가합니다.  
> (6) `querydsl`이 `compileClassPath`를 상속하도록 설정합니다.  
> (7) `querydsl` 컴파일시 사용할 옵션을 설정합니다.

`Querydsl`을 사용하면서 어떻게 보면 가장 불편한 점이 `gradle` 설정하는 부분이라고 할 수 있는데요, `lombok`도 예전에는 꽤 불편하게 설정했던 것으로 기억하는데 지금은 아주 단순해진 거 보면 이 부분도 나아지지 않을까 기대해봅니다. 😁

이제 잘 동작하는지 확인하기 위해 `Test`용 `Entity`를 하나 급조해서 만들어보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import lombok.Getter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
public class Item {
    @Id
    @GeneratedValue
    private Long id;
}
```

이렇게 작성한 뒤 `gradle task` 중 `compileQuerydsl`을 실행시킵니다.

`IDE`를 사용하시는 분은 `gradle` 탭에서 해당 `task`를 찾아서 실행시키시면 되고, 터미널에서 직접 실행시키실 분은 아래 명령어를 프로젝트 루트 폴더에서 수행하시면 됩니다.

```shell
./gradlew compileQuerydsl
```

```text
Welcome to Gradle 7.1.1!

Here are the highlights of this release:
 - Faster incremental Java compilation
 - Easier source set configuration in the Kotlin DSL

For more details see https://docs.gradle.org/7.1.1/release-notes.html

Starting a Gradle Daemon, 1 incompatible Daemon could not be reused, use --status for details

> Task :compileQuerydsl
Note: Running JPAAnnotationProcessor
Note: Serializing Entity types
Note: Generating io.lcalmsky.querydsl.domain.QItem for [io.lcalmsky.querydsl.domain.Item]
Note: Running JPAAnnotationProcessor
Note: Running JPAAnnotationProcessor

Deprecated Gradle features were used in this build, making it incompatible with Gradle 8.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

See https://docs.gradle.org/7.1.1/userguide/command_line_interface.html#sec:command_line_warnings

BUILD SUCCESSFUL in 4s
2 actionable tasks: 2 executed
```

로그에서 **BUILD SUCCESSFUL**를 확인하셨다면 아까 지정한 경로를 확인해봅시다.

프로젝트 하위 디렉토리 중 `build/generated/querydsl` 여기 진입하면 아까 생성한 `Item Entity`가 `QItem`으로 변해있는 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/querydsl/master/guide/qitem.png)

`QItem.java` 파일을 열어보면

```java
package io.lcalmsky.querydsl.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

@Generated("com.querydsl.codegen.EntitySerializer")
public class QItem extends EntityPathBase<Item> {

    private static final long serialVersionUID = 1540314452L;

    public static final QItem item = new QItem("item");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QItem(String variable) {
        super(Item.class, forVariable(variable));
    }

    public QItem(Path<? extends Item> path) {
        super(path.getType(), path.getMetadata());
    }

    public QItem(PathMetadata metadata) {
        super(Item.class, metadata);
    }

}
```

`Item Entity`를 상속받아 뭔가를 하는 클래스임을 확인할 수 있습니다.

⚠️ `Entity`가 변경되었다면 이 과정을 다시 수행해서 `QEntity`가 제대로 생성될 수 있게 해야합니다.

그리고 `build`시에 포함되므로 굳이 `git`에 포함시킬 필요가 없습니다.

저 같은 경우 프로젝트를 먼저 만들고 프로젝트 내에서 `git init` 명령어를 통해 `git nature`를 생성하는데 그 때 `.gitignore` 파일에 `build` 경로가 포함이 되어있어서 신경쓸 필요가 없긴한데, 그렇지 않은 분들은 `git`으로 소스 코드를 관리할 땐 반드시 해당 경로를 무시하도록 처리해주셔야 합니다.

특히 지금 처럼 `build` 경로로 설정하지 않고 `src` 경로나 다른 경로로 설정해 `.gitignore`에서 추가로 설정해야 하는 일이 없게 기본적인 설정을 따라주시는 게 편리합니다.

그럼 `Querydsl`을 이용해 정상적으로 쿼리를 수행하는지 확인해보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ItemTest {
    @Autowired
    EntityManager entityManager;

    @Test
    void test() {
        // given
        Item item = new Item();
        entityManager.persist(item);

        // when
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager); // (1)
        QItem qItem = new QItem("i"); // (2)
        Item found = queryFactory.selectFrom(qItem).fetchOne(); // (3)

        // then
        assertEquals(found, item); // (4)
    }
}
```

> (1) `JPAQueryFactory`를 생성합니다. 이 때 생성자로 `EntityManager`를 주입해줍니다.  
> (2) `QItem` 객체를 생성합니다. 생성자에는 `Entity`의 `alias`로 사용할 변수명을 입력합니다.  
> (3) `JPQL`을 작성하듯이 자바 코드로 쿼리를 작성합니다.  
> (4) `DB`에 저장된 데이터와 다시 조회해 온 데이터가 동일한지 확인합니다.  

잘 동작했는지 확인하기 위해 아래 설정을 추가해줍니다.

`H2 데이터베이스`가 실행되며 테이블을 직접 생성하고 포매팅된 `SQL` 로그를 확인할 수 있기 위함입니다.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: debug
```

테스트를 실행시킨 결과는 다음과 같습니다.

```text
2021-07-15 19:53:33.992 DEBUG 4334 --- [           main] org.hibernate.SQL                        : 
    
    create table item (
       id bigint not null,
        primary key (id)
    )
// 생략
2021-07-15 19:53:35.898 DEBUG 4334 --- [           main] org.hibernate.SQL                        : 
    insert 
    into
        item
        (id) 
    values
        (?)
2021-07-15 19:53:35.906 DEBUG 4334 --- [           main] org.hibernate.SQL                        : 
    select
        item0_.id as id1_0_ 
    from
        item item0_
```

정상적으로 테이블을 생성한 뒤 하나의 데이터를 넣고 다시 조회해오는 쿼리가 모두 로그로 잘 출력되었습니다.

내부적으로 `EntityManager`를 이용해 쿼리하기 때문에 로깅 관련해서 추가로 설정해줄 필요가 없습니다.

아까 `QItem` 클래스를 유심히 본 분이라면 굳이 `new`를 사용하지 않아도 객체를 사용할 수 있다는 것을 눈치채셨을 텐데요, `QItem.hello`로 `static final`로 선언된 객체에 접근할 수 있습니다.

이렇게 `Querydsl`을 사용하기위한 프로젝트 설정 및 간단한 테스트를 해보았습니다.

---

저는 개인적으로 `Querydsl`의 기능은 매우 좋다고 생각하지만 여기서 사용하는 컨벤션 만큼은 정말 별로라고 생각합니다.

`JPAQueryFactory`는 자바 컨벤션에 따르면 `JpaQueryFactory`가 되어야하고, `QItem.hello` 역시 `QItem.HELLO`로 상수임을 표현하는 게 더 좋은 방법이라고 생각합니다.

스프링에 `JpaQueryMethodFactory`가 있긴 한데 이것과 구분하려고 했던 걸까요?🤔

이렇게 좋은 기술을 만들 수 있는 개발자도 컨벤션을 다 준수하지는 않는다는 생각을 하니 한 편으로는 꽤 흥미롭기도하고 왜 지키지 않았을지 궁금하기도 합니다.

사실 컨벤션의 종류가 여러 가지이기도 하고 꼭 지켜야 제대로 된 소스 코드라고 할 수 있는 것도 아니지만...

그래도 하루 빨리 스프링 진영에서 `Querydsl`을 다듬어서 귀찮은 설정과 불편한 컨벤션을 보지 않아도 되도록 정식 버전에 포함시켜줬으면 하는 바람이네요😀