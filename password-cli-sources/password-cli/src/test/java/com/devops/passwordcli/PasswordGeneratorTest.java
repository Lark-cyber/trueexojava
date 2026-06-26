package com.devops.passwordcli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du générateur et de l'évaluateur de mots de passe.
 * Les tests Docker ne sont pas inclus ici (ils nécessitent l'infrastructure).
 */
class PasswordGeneratorTest {

    @Test
    void testLengthRespected() {
        CliConfig config = new CliConfig(20, 1, true, true, true, true, "localhost", 5000, true);
        PasswordGenerator gen = new PasswordGenerator(config);
        assertEquals(20, gen.generate().length());
    }

    @Test
    void testUppercaseIncluded() {
        CliConfig config = new CliConfig(50, 1, true, false, false, false, "localhost", 5000, true);
        PasswordGenerator gen = new PasswordGenerator(config);
        String pw = gen.generate();
        assertTrue(pw.chars().anyMatch(Character::isUpperCase), "Doit contenir au moins une majuscule");
        assertTrue(pw.chars().noneMatch(Character::isLowerCase), "Ne doit pas contenir de minuscule");
    }

    @Test
    void testDigitsOnly() {
        CliConfig config = new CliConfig(10, 1, false, false, true, false, "localhost", 5000, true);
        PasswordGenerator gen = new PasswordGenerator(config);
        String pw = gen.generate();
        assertTrue(pw.matches("[0-9]+"), "Ne doit contenir que des chiffres");
    }

    @Test
    void testAllTypesPresent() {
        // Avec longueur 20, la probabilité d'avoir les 4 types est quasi-certaine
        CliConfig config = new CliConfig(20, 1, true, true, true, true, "localhost", 5000, true);
        PasswordGenerator gen = new PasswordGenerator(config);
        String pw = gen.generate();
        assertTrue(pw.chars().anyMatch(Character::isUpperCase));
        assertTrue(pw.chars().anyMatch(Character::isLowerCase));
        assertTrue(pw.chars().anyMatch(Character::isDigit));
        assertTrue(pw.chars().anyMatch(c -> !Character.isLetterOrDigit(c)));
    }

    @Test
    void testStrengthVeryWeak() {
        // "aaa" : très court, mono-type, répétitions
        StrengthResult r = StrengthEvaluator.evaluate("aaa");
        assertEquals(0, r.getScore());
    }

    @Test
    void testStrengthStrong() {
        // Mot de passe long avec tous les types
        StrengthResult r = StrengthEvaluator.evaluate("Xk#9mP!2vL@nQ7wR");
        assertTrue(r.getScore() >= 3, "Doit être Fort ou Très fort");
    }

    @Test
    void testStrengthResultLabels() {
        assertEquals("Très faible", new StrengthResult(0, "", "test").getLabel());
        assertEquals("Fort",        new StrengthResult(3, "", "test").getLabel());
        assertEquals("Très fort",   new StrengthResult(4, "", "test").getLabel());
    }

    @Test
    void testBurstMode() {
        CliConfig config = new CliConfig(12, 5, true, true, true, false, "localhost", 5000, true);
        PasswordGenerator gen = new PasswordGenerator(config);
        // Vérifie que chaque mot de passe est unique (collisions quasi-impossibles à 12 chars)
        java.util.Set<String> passwords = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) passwords.add(gen.generate());
        assertEquals(5, passwords.size(), "Les 5 mots de passe devraient être différents");
    }
}
