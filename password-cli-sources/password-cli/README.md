# Password CLI — Générateur de Mots de Passe Sécurisé

Outil en ligne de commande Java 21 pour générer et valider la robustesse de mots de passe,
avec validation externe via un conteneur Docker hébergeant l'algorithme **zxcvbn**.

## Fonctionnalités

- ✅ Génération configurable (longueur, types de caractères)
- ✅ Mode rafale (plusieurs mots de passe en une seule exécution)
- ✅ Indicateur de force : Très faible → Très fort (★☆☆☆☆ → ★★★★★)
- ✅ Validation externe Docker (zxcvbn via API REST Flask)
- ✅ Fallback local si le conteneur est indisponible
- ✅ Mode interactif guidé (`-i`)

## Installation & Lancement

### 1. Cloner le dépôt

```bash
git clone https://github.com/VOTRE_NOM/password-cli.git
cd password-cli
```

### 2. Lancer le conteneur Docker (service zxcvbn)

```bash
cd docker
docker build -t zxcvbn-service .
docker run -d -p 5000:5000 --name zxcvbn zxcvbn-service
# Vérification
curl http://localhost:5000/health
```

### 3. Compiler le JAR Java

```bash
cd ..
mvn clean package
```

### 4. Utiliser le CLI

```bash
# Usage de base (16 caractères, tous types, Docker activé)
java -jar target/password-cli.jar

# Mode rafale : 5 mots de passe de 20 caractères
java -jar target/password-cli.jar -l 20 -n 5

# Sans symboles, sans Docker
java -jar target/password-cli.jar -l 12 --no-symbols --no-docker

# Mode interactif
java -jar target/password-cli.jar -i
```

## Options CLI

| Option | Description | Défaut |
|---|---|---|
| `-l <n>` | Longueur du mot de passe (4-256) | 16 |
| `-n <n>` | Nombre de mots de passe (mode rafale) | 1 |
| `--no-upper` | Exclure les majuscules | activées |
| `--no-lower` | Exclure les minuscules | activées |
| `--no-digits` | Exclure les chiffres | activés |
| `--no-symbols` | Exclure les symboles | activés |
| `--host <h>` | Hôte du conteneur Docker | localhost |
| `--port <p>` | Port du conteneur Docker | 5000 |
| `--no-docker` | Score local uniquement | Docker activé |
| `-i` | Mode interactif guidé | — |

## Architecture

```
password-cli/
├── src/main/java/com/devops/passwordcli/
│   ├── Main.java              # Point d'entrée, parsing CLI
│   ├── CliConfig.java         # Configuration immuable
│   ├── PasswordGenerator.java # Génération (SecureRandom + CSPRNG)
│   ├── StrengthEvaluator.java # Score local (longueur + diversité + patterns)
│   ├── StrengthResult.java    # Résultat (score, label, étoiles)
│   └── DockerValidator.java   # Client HTTP vers le conteneur Docker
├── docker/
│   ├── Dockerfile             # Image Python/Flask/zxcvbn
│   ├── app.py                 # API REST /score
│   └── requirements.txt
└── pom.xml
```

## Tests

```bash
mvn test
```
