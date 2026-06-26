package com.devops.passwordcli;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Communique avec le conteneur Docker exécutant le service zxcvbn (via une API REST Flask).
 *
 * Architecture de communication :
 *   Java CLI ──(HTTP POST /score)──► Flask/Python (Docker) ──► zxcvbn ──► JSON ──► Java
 *
 * Le conteneur expose un endpoint POST /score attendant :
 *   { "password": "..." }
 * et retournant :
 *   { "score": 0-4, "feedback": "...", "crack_time": "..." }
 *
 * On utilise le client HTTP natif de Java 11+ (java.net.http.HttpClient),
 * sans dépendance externe, pour garder le JAR autonome.
 *
 * Timeout court (3s) : l'application reste utilisable même si Docker est absent.
 */
public class DockerValidator {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String baseUrl;
    private final HttpClient httpClient;

    public DockerValidator(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        // HttpClient réutilisable et thread-safe
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Envoie le mot de passe au conteneur Docker et récupère le score zxcvbn.
     *
     * @param password Le mot de passe à analyser
     * @return Un StrengthResult construit depuis la réponse JSON du service
     * @throws IOException          Si la connexion échoue
     * @throws InterruptedException Si le thread est interrompu pendant la requête
     */
    public StrengthResult validate(String password) throws IOException, InterruptedException {
        // Construction du body JSON à la main (pas de dépendance Jackson)
        // On échappe les caractères spéciaux pour ne pas casser le JSON
        String escapedPassword = password
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        String body = "{\"password\":\"" + escapedPassword + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/score"))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " : " + response.body());
        }

        return parseResponse(response.body());
    }

    /**
     * Parse manuellement la réponse JSON du service Docker.
     * Le format attendu est simple et stable, un parsing léger suffit
     * et évite d'introduire une dépendance à Jackson ou Gson.
     */
    private StrengthResult parseResponse(String json) {
        int score = extractInt(json, "score");
        String feedback = extractString(json, "feedback");
        String crackTime = extractString(json, "crack_time");

        String details = feedback + " | Temps d'attaque estimé : " + crackTime;
        return new StrengthResult(score, details, "docker-zxcvbn");
    }

    /** Extrait un entier du JSON brut par pattern matching simple. */
    private int extractInt(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return 0;
        int start = idx + marker.length();
        // Lire jusqu'au prochain délimiteur
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
            else if (!sb.isEmpty()) break;
        }
        return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
    }

    /** Extrait une chaîne de caractères du JSON brut. */
    private String extractString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return "";
        int start = idx + marker.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? "" : json.substring(start, end);
    }
}
