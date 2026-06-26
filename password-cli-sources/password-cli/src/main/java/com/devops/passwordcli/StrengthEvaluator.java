package com.devops.passwordcli;

import java.util.regex.Pattern;

/**
 * Évalue la robustesse d'un mot de passe localement, sans appel réseau.
 * Utilisé en premier recours et comme fallback si Docker est indisponible.
 *
 * Critères de scoring (inspirés de NIST SP 800-63B et zxcvbn) :
 *   - Longueur : facteur principal (> 8, > 12, > 16, > 20)
 *   - Diversité : présence de majuscules, minuscules, chiffres, symboles
 *   - Pénalités : répétitions consécutives, séquences simples (123, abc)
 *
 * Score 0-4 → labels : Très faible, Faible, Moyen, Fort, Très fort
 */
public class StrengthEvaluator {

    // Patterns de détection de séquences triviales
    private static final Pattern SEQ_ALPHA  = Pattern.compile("(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEQ_DIGIT  = Pattern.compile("(012|123|234|345|456|567|678|789|890)");
    private static final Pattern SEQ_KEYB   = Pattern.compile("(qwerty|azerty|qwert|asdf|zxcv)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEAT     = Pattern.compile("(.)\\1{2,}"); // 3+ caractères identiques consécutifs

    public static StrengthResult evaluate(String password) {
        int score = 0;
        StringBuilder details = new StringBuilder();

        // --- Critère 1 : longueur ---
        int len = password.length();
        if      (len >= 20) { score += 2; details.append("longueur excellente (").append(len).append("); "); }
        else if (len >= 16) { score += 2; details.append("longueur très bonne (").append(len).append("); "); }
        else if (len >= 12) { score += 1; details.append("longueur bonne (").append(len).append("); "); }
        else if (len >= 8)  { score += 0; details.append("longueur minimale (").append(len).append("); "); }
        else                { score -= 1; details.append("longueur insuffisante (").append(len).append("); "); }

        // --- Critère 2 : diversité des caractères ---
        boolean hasUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower   = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol  = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int diversity = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSymbol ? 1 : 0);
        score += diversity - 1; // 0 type → -1 ; 4 types → +3

        details.append(diversity).append("/4 types; ");

        // --- Critère 3 : pénalités pour séquences et répétitions ---
        if (SEQ_ALPHA.matcher(password).find()) {
            score--;
            details.append("séquence alpha détectée; ");
        }
        if (SEQ_DIGIT.matcher(password).find()) {
            score--;
            details.append("séquence chiffres détectée; ");
        }
        if (SEQ_KEYB.matcher(password).find()) {
            score -= 2;
            details.append("motif clavier détecté; ");
        }
        if (REPEAT.matcher(password).find()) {
            score--;
            details.append("répétitions détectées; ");
        }

        // Normalisation dans [0, 4]
        score = Math.max(0, Math.min(4, score));

        return new StrengthResult(score, details.toString().trim(), "local");
    }
}
