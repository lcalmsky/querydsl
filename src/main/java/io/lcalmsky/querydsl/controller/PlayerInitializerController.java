package io.lcalmsky.querydsl.controller;

import io.lcalmsky.querydsl.domain.Player;
import io.lcalmsky.querydsl.domain.Team;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
public class PlayerInitializerController {
    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        Team tottenhamHotspur = new Team("Tottenham Hotspur F.C.");
        Team manchesterCity = new Team("Manchester City F.C.");
        entityManager.persist(tottenhamHotspur);
        entityManager.persist(manchesterCity);

        Player harryKane = new Player("Harry Kane", 27, tottenhamHotspur);
        harryKane.contactSalary(200000);
        harryKane.begins();
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        heungminSon.contactSalary(140000);
        heungminSon.begins();
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        kevinDeBruyne.contactSalary(350000);
        kevinDeBruyne.begins();
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);
        raheemSterling.contactSalary(300000);
        raheemSterling.begins();
        Player deleAlli = new Player("Dele Alli", 25, tottenhamHotspur);
        deleAlli.contactSalary(100000);
        deleAlli.begins();
        Player hugoLloris = new Player("Hugo Lloris", 34, tottenhamHotspur);
        hugoLloris.contactSalary(10000);
        hugoLloris.begins();
        Player tobyAlderweireld = new Player("Toby Alderweireld", 32, tottenhamHotspur);
        tobyAlderweireld.contactSalary(80000);
        tobyAlderweireld.begins();
        Player moussaSissoko = new Player("Moussa Sissoko", 31, tottenhamHotspur);
        moussaSissoko.contactSalary(80000);
        moussaSissoko.begins();
        Player erikLamela = new Player("Erik Lamela", 29, tottenhamHotspur);
        erikLamela.contactSalary(80000);
        erikLamela.begins();
        Player lukasMoura = new Player("Lukas Moura", 28, tottenhamHotspur);
        lukasMoura.contactSalary(80000);
        lukasMoura.begins();

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
        entityManager.persist(deleAlli);
        entityManager.persist(hugoLloris);
        entityManager.persist(tobyAlderweireld);
        entityManager.persist(moussaSissoko);
        entityManager.persist(erikLamela);
        entityManager.persist(lukasMoura);
    }
}