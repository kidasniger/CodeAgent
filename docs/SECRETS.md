# Gestion des secrets (corr-047)

## Secrets GitHub utilisés par `.github/workflows/build.yml`

| Secret | Rôle | Déclaration |
| --- | --- | --- |
| `KEYSTORE_BASE64` | Keystore de signature release, encodé base64 (`base64 -w0 keystore.jks`) | lu dans le job `release` |
| `KEYSTORE_PASSWORD` | Mot de passe du keystore **et** de la clé (même valeur) | déclaré une seule fois par job (`KS_PASS`) |
| `KEY_ALIAS` | Alias de la clé de signature | déclaré une seule fois par job (`KS_ALIAS`) |

### Règles (corr-047)

1. **Pas de duplication** : `KEY_PASSWORD` est **dérivé** de `KS_PASS` côté
   shell (`echo "KEY_PASSWORD=$KS_PASS"`) et jamais re-déclaré depuis le
   secret. La valeur « android » n'apparaît que pour le **keystore de repli
   debug** généré à la volée quand `KEYSTORE_BASE64` est absent (jamais pour
   une release signée).
2. **Aucun keystore commité** : vérifié (corr-046) — aucun `.jks`/`.keystore`
   dans le dépôt ; `.gitignore` les exclut.
3. **Portée minimale** : `permissions: contents: read` au niveau workflow ;
   le job de release ne reçoit que ce dont il a besoin.
4. **Rotation** : si un secret est suspecté exposé, régénérer le keystore,
   re-encoder en base64 et mettre à jour `KEYSTORE_BASE64` + `KEYSTORE_PASSWORD`
   en une seule fois.
5. **Debug de repli** : les APK de repli (keystore debug généré au CI) ne sont
   jamais publiés en release ; ils servent uniquement à tester le build.

## Scan de dépendances (corr-044)

Le workflow `build.yml` lance `osv-scanner` (base Open Source Vulnerabilities
de Google) sur le lockfile Gradle à chaque build : les vulnérabilités connues
des dépendances sont signalées en avertissement (`continue-on-error: true`
pour ne pas bloquer les builds tant que le rapport est en cours de traitement,
avec artefact `osv-report` consultable).
