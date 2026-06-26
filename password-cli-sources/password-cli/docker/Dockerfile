# Image légère Python 3.11 alpine pour minimiser la taille du conteneur
FROM python:3.11-slim

WORKDIR /app

# Installation des dépendances Python
# zxcvbn : algorithme d'estimation d'entropie de Dropbox (port Python)
# flask  : micro-framework HTTP pour exposer l'API REST
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

# Le service écoute sur le port 5000 (mappé depuis l'hôte via -p 5000:5000)
EXPOSE 5000

# Lancement via gunicorn en production (plus robuste que le serveur de dev Flask)
# 4 workers pour gérer les appels simultanés
CMD ["python", "app.py"]
