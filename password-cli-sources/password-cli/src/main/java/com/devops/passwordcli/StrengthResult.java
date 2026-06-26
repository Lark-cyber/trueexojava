package com.devops.passwordcli;

/**
 * Résultat de l'évaluation de robustesse d'un mot de passe.
 * Contient le score numérique (0-4), le label textuel, et les détails explicatifs.
 */
public class StrengthResult {

    // Mapping score → label et représentation visuelle étoiles
    private static final String[] LABELS = {
        "Très faible", "Faible", "Moyen", "Fort", "Très fort"
    };
    private static final String[] STARS = {
        "★☆☆☆☆", "★★☆☆☆", "★★★☆☆", "★★★★☆", "★★★★★"
    };

    private final int score;       // 0 à 4
    private final String details;  // explication humaine du score
    private final String source;   // "local" ou "docker-zxcvbn"

    public StrengthResult(int score, String details, String source) {
        this.score   = Math.max(0, Math.min(4, score)); // sécurité des bornes
        this.details = details;
        this.source  = source;
    }

    public int    getScore()   { return score; }
    public String getDetails() { return details + " [" + source + "]"; }
    public String getLabel()   { return LABELS[score]; }
    public String getStars()   { return STARS[score]; }
}
