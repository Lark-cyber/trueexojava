"""
Service de validation de mots de passe via zxcvbn.
Expose un endpoint REST POST /score consommé par le CLI Java.

Retourne un JSON structuré :
  { "score": 0-4, "feedback": "...", "crack_time": "..." }

zxcvbn est l'algorithme de Dropbox qui estime la résistance d'un mot de passe
aux attaques par dictionnaire, bruteforce et patterns communs.
"""

from flask import Flask, request, jsonify
import zxcvbn as zxc

app = Flask(__name__)


@app.route("/health", methods=["GET"])
def health():
    """Endpoint de vérification de disponibilité du service."""
    return jsonify({"status": "ok"})


@app.route("/score", methods=["POST"])
def score():
    """
    Analyse un mot de passe et retourne son score de robustesse.
    Entrée  : { "password": "<mot_de_passe>" }
    Sortie  : { "score": 0-4, "feedback": "...", "crack_time": "..." }
    """
    data = request.get_json(force=True, silent=True)
    if not data or "password" not in data:
        return jsonify({"error": "Champ 'password' manquant"}), 400

    password = data["password"]
    result = zxc.zxcvbn(password)

    # crack_times_display contient le temps estimé pour une attaque offline rapide
    crack_time = result["crack_times_display"].get(
        "offline_fast_hashing_1e10_per_second", "inconnu"
    )

    # Consolidation du feedback : suggestion principale ou message par défaut
    suggestions = result["feedback"].get("suggestions", [])
    warning = result["feedback"].get("warning", "")
    feedback_parts = []
    if warning:
        feedback_parts.append(warning)
    if suggestions:
        feedback_parts.extend(suggestions[:2])  # On limite à 2 suggestions

    feedback = "; ".join(feedback_parts) if feedback_parts else "Aucune suggestion"

    return jsonify({
        "score": result["score"],
        "feedback": feedback,
        "crack_time": crack_time
    })


if __name__ == "__main__":
    # Écoute sur toutes les interfaces pour être accessible depuis l'hôte Docker
    app.run(host="0.0.0.0", port=5000)
