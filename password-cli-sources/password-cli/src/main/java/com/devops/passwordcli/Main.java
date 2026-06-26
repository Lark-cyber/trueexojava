package com.devops.passwordcli;

import java.util.Scanner;

/**
 * Point d'entrée principal de l'application CLI.
 * Analyse les arguments de ligne de commande et orchestre la génération + validation.
 *
 * Usage :
 *   java -jar password-cli.jar [options]
 *   Options :
 *     -l <longueur>     Longueur du mot de passe (défaut : 16)
 *     -n <nombre>       Nombre de mots de passe à générer (mode rafale, défaut : 1)
 *     --no-upper        Exclure les majuscules
 *     --no-lower        Exclure les minuscules
 *     --no-digits       Exclure les chiffres
 *     --no-symbols      Exclure les symboles
 *     --host <host>     Hôte du conteneur Docker (défaut : localhost)
 *     --port <port>     Port du conteneur Docker (défaut : 5000)
 *     --no-docker       Désactiver la validation externe et utiliser uniquement le score local
 *     -i                Mode interactif (saisie utilisateur)
 */
public class Main {

    public static void main(String[] args) {
        // Si aucun argument → mode interactif
        if (args.length == 0) {
            runInteractiveMode();
            return;
        }

        // Parsing des arguments de ligne de commande
        CliConfig config = parseArgs(args);
        if (config == null) {
            printHelp();
            System.exit(1);
        }

        runWithConfig(config);
    }

    /**
     * Mode interactif : guide l'utilisateur étape par étape via stdin.
     * Utile pour les utilisateurs qui ne connaissent pas les flags CLI.
     */
    private static void runInteractiveMode() {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== Générateur de Mots de Passe Sécurisé ===\n");

        System.out.print("Longueur du mot de passe [16] : ");
        String lengthInput = sc.nextLine().trim();
        int length = lengthInput.isEmpty() ? 16 : Integer.parseInt(lengthInput);

        System.out.print("Nombre de mots de passe à générer [1] : ");
        String countInput = sc.nextLine().trim();
        int count = countInput.isEmpty() ? 1 : Integer.parseInt(countInput);

        System.out.print("Inclure les MAJUSCULES ? [O/n] : ");
        boolean upper = !sc.nextLine().trim().equalsIgnoreCase("n");

        System.out.print("Inclure les minuscules ? [O/n] : ");
        boolean lower = !sc.nextLine().trim().equalsIgnoreCase("n");

        System.out.print("Inclure les CHIFFRES ? [O/n] : ");
        boolean digits = !sc.nextLine().trim().equalsIgnoreCase("n");

        System.out.print("Inclure les SYMBOLES (@#$…) ? [O/n] : ");
        boolean symbols = !sc.nextLine().trim().equalsIgnoreCase("n");

        System.out.print("Hôte Docker [localhost] : ");
        String hostInput = sc.nextLine().trim();
        String host = hostInput.isEmpty() ? "localhost" : hostInput;

        System.out.print("Port Docker [5000] : ");
        String portInput = sc.nextLine().trim();
        int port = portInput.isEmpty() ? 5000 : Integer.parseInt(portInput);

        CliConfig config = new CliConfig(length, count, upper, lower, digits, symbols, host, port, false);
        runWithConfig(config);
        sc.close();
    }

    /**
     * Exécute la génération + validation pour une configuration donnée.
     * Délègue à PasswordGenerator pour la génération et DockerValidator pour le score externe.
     */
    private static void runWithConfig(CliConfig config) {
        PasswordGenerator generator = new PasswordGenerator(config);
        DockerValidator validator = config.isNoDocker()
                ? null
                : new DockerValidator(config.getDockerHost(), config.getDockerPort());

        System.out.println("\n--- Génération en cours ---\n");

        for (int i = 0; i < config.getCount(); i++) {
            String password = generator.generate();

            // Score local toujours calculé (fallback si Docker indisponible)
            StrengthResult localResult = StrengthEvaluator.evaluate(password);

            // Score Docker (optionnel, enrichit le score local)
            StrengthResult finalResult = localResult;
            if (validator != null) {
                try {
                    StrengthResult dockerResult = validator.validate(password);
                    // On fusionne : le score Docker prime, mais on conserve les détails locaux
                    finalResult = dockerResult;
                } catch (Exception e) {
                    System.err.println("[AVERTISSEMENT] Validation Docker indisponible : " + e.getMessage());
                    System.err.println("           → Utilisation du score local uniquement.\n");
                }
            }

            printResult(i + 1, password, finalResult, config.getCount());
        }
    }

    /**
     * Affiche un résultat formaté pour un mot de passe donné.
     */
    private static void printResult(int index, String password, StrengthResult result, int total) {
        if (total > 1) {
            System.out.printf("[%d] ", index);
        }
        System.out.println("Mot de passe : " + password);
        System.out.println("Force       : " + result.getLabel() + " " + result.getStars());
        System.out.println("Détails     : " + result.getDetails());
        System.out.println();
    }

    /**
     * Analyse les arguments CLI et construit un objet CliConfig.
     * Retourne null si les arguments sont invalides.
     */
    private static CliConfig parseArgs(String[] args) {
        int length = 16;
        int count = 1;
        boolean upper = true, lower = true, digits = true, symbols = true;
        String host = "localhost";
        int port = 5000;
        boolean noDocker = false;
        boolean interactive = false;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-l" -> length = Integer.parseInt(args[++i]);
                    case "-n" -> count = Integer.parseInt(args[++i]);
                    case "--no-upper" -> upper = false;
                    case "--no-lower" -> lower = false;
                    case "--no-digits" -> digits = false;
                    case "--no-symbols" -> symbols = false;
                    case "--host" -> host = args[++i];
                    case "--port" -> port = Integer.parseInt(args[++i]);
                    case "--no-docker" -> noDocker = true;
                    case "-i" -> interactive = true;
                    case "-h", "--help" -> { printHelp(); return null; }
                    default -> {
                        System.err.println("Argument inconnu : " + args[i]);
                        return null;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.err.println("Erreur de parsing des arguments : " + e.getMessage());
            return null;
        }

        if (interactive) {
            runInteractiveMode();
            return null; // déjà géré
        }

        // Validation : au moins un type de caractère doit être activé
        if (!upper && !lower && !digits && !symbols) {
            System.err.println("Erreur : au moins un type de caractère doit être sélectionné.");
            return null;
        }

        if (length < 4 || length > 256) {
            System.err.println("Erreur : la longueur doit être comprise entre 4 et 256.");
            return null;
        }

        return new CliConfig(length, count, upper, lower, digits, symbols, host, port, noDocker);
    }

    private static void printHelp() {
        System.out.println("""
            Usage: java -jar password-cli.jar [options]

            Options:
              -l <n>         Longueur du mot de passe (4-256, défaut: 16)
              -n <n>         Nombre de mots de passe (mode rafale, défaut: 1)
              --no-upper     Exclure les majuscules (A-Z)
              --no-lower     Exclure les minuscules (a-z)
              --no-digits    Exclure les chiffres (0-9)
              --no-symbols   Exclure les symboles (@#$!...)
              --host <h>     Hôte du conteneur Docker (défaut: localhost)
              --port <p>     Port du conteneur Docker (défaut: 5000)
              --no-docker    Désactiver la validation externe Docker
              -i             Mode interactif guidé
              -h, --help     Afficher cette aide

            Exemples:
              java -jar password-cli.jar -l 20 -n 5
              java -jar password-cli.jar -l 12 --no-symbols --no-docker
              java -jar password-cli.jar -i
            """);
    }
}
