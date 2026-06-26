package com.devops.passwordcli;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Génère des mots de passe aléatoires selon les paramètres de configuration.
 *
 * Stratégie :
 *   1. Construire l'alphabet global à partir des types activés.
 *   2. Garantir la présence d'au moins un caractère de chaque type activé
 *      (évite les mots de passe qui "passent" la config sans respecter le mélange).
 *   3. Compléter jusqu'à la longueur demandée avec des caractères aléatoires.
 *   4. Mélanger le résultat pour éviter tout pattern prévisible en début de chaîne.
 *
 * On utilise SecureRandom (CSPRNG) plutôt que Random pour une entropie cryptographique.
 */
public class PasswordGenerator {

    // Alphabets disponibles par catégorie
    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    // Symboles courants, compatibles avec la plupart des systèmes
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private final CliConfig config;
    private final SecureRandom random;
    private final String alphabet;          // pool de caractères utilisé pour le remplissage
    private final List<String> enabledSets; // ensembles activés (pour la garantie minimale)

    public PasswordGenerator(CliConfig config) {
        this.config = config;
        this.random = new SecureRandom();

        // Construction de l'alphabet et enregistrement des sets actifs
        StringBuilder sb = new StringBuilder();
        enabledSets = new ArrayList<>();

        if (config.isUseUpper())  { sb.append(UPPER);   enabledSets.add(UPPER); }
        if (config.isUseLower())  { sb.append(LOWER);   enabledSets.add(LOWER); }
        if (config.isUseDigits()) { sb.append(DIGITS);  enabledSets.add(DIGITS); }
        if (config.isUseSymbols()){ sb.append(SYMBOLS); enabledSets.add(SYMBOLS); }

        this.alphabet = sb.toString();
    }

    /**
     * Génère un mot de passe unique respectant la configuration.
     * Garantit que chaque type de caractère activé est représenté au moins une fois.
     */
    public String generate() {
        List<Character> chars = new ArrayList<>(config.getLength());

        // Étape 1 : insérer au moins un caractère de chaque set activé
        for (String set : enabledSets) {
            chars.add(randomCharFrom(set));
        }

        // Étape 2 : compléter avec des caractères tirés de l'alphabet complet
        while (chars.size() < config.getLength()) {
            chars.add(randomCharFrom(alphabet));
        }

        // Étape 3 : mélange cryptographiquement sûr (Fisher-Yates via Collections.shuffle + SecureRandom)
        Collections.shuffle(chars, random);

        // Conversion en String
        StringBuilder result = new StringBuilder(config.getLength());
        for (char c : chars) result.append(c);
        return result.toString();
    }

    /** Tire un caractère aléatoire depuis un ensemble donné. */
    private char randomCharFrom(String set) {
        return set.charAt(random.nextInt(set.length()));
    }
}
