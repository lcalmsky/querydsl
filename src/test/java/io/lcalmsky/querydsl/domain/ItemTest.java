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
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QItem qItem = new QItem("i");
        Item found = queryFactory.selectFrom(qItem).fetchOne();

        // then
        assertEquals(found, item);
    }
}