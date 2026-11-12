package com.zagent.app.data

import kotlinx.serialization.json.*

/**
 * corr-018 — Catalogue des outils exécutables par l'agent (function calling).
 *
 * Expose le schéma transmis au modèle, l'exécution des outils et les
 * garde-fous associés : confirmation des écritures, des suppressions et des
 * commandes, périmètre du projet. Chaque outil suit le contrat OpenAI
 * « tools » : nom, description JSON-Schema, arguments sérialisés.
 */
object AgentTools {
    val toolsJson = buildJsonArray {
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "list_dir")
                put("description", "Liste récursivement les fichiers d'un dossier (max profondeur 4, max 300 entrées).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du dossier (ex: '.', 'src')")
                        })
                    })
                    put("required", buildJsonArray {})
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "read_file")
                put("description", "Lit un fichier texte UTF-8 (tronqué à 20000 caractères). Pour un gros fichier, lis une plage : offset_line (1re ligne, 1-based) + lines (nombre de lignes).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier")
                        })
                        put("offset_line", buildJsonObject {
                            put("type", "integer")
                            put("description", "Première ligne à lire (1-based, optionnel)")
                        })
                        put("lines", buildJsonObject {
                            put("type", "integer")
                            put("description", "Nombre de lignes à lire à partir de offset_line (optionnel)")
                        })
                    })
                    put("required", buildJsonArray { add("path") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "make_dir")
                put("description", "Crée un dossier et ses parents.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du dossier à créer")
                        })
                    })
                    put("required", buildJsonArray { add("path") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "write_file")
                put("description", "Crée ou remplace un fichier avec le contenu COMPLET, en créant les dossiers parents.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier")
                        })
                        put("content", buildJsonObject {
                            put("type", "string")
                            put("description", "Contenu complet du fichier")
                        })
                    })
                    put("required", buildJsonArray { add("path"); add("content") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "delete_file")
                put("description", "Supprime un fichier ou un dossier du projet (l'utilisateur doit confirmer). Pour seulement vider un fichier, utilise write_file avec un contenu vide.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier ou dossier à supprimer")
                        })
                    })
                    put("required", buildJsonArray { add("path") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "edit_file")
                put("description", "Modifie un fichier existant en remplaçant UN passage exact par un autre. Préfère-le à write_file pour tout changement partiel. old_string doit apparaître exactement une fois (ajoute du contexte autour sinon).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier")
                        })
                        put("old_string", buildJsonObject {
                            put("type", "string")
                            put("description", "Texte exact à remplacer (indentation comprise)")
                        })
                        put("new_string", buildJsonObject {
                            put("type", "string")
                            put("description", "Texte de remplacement")
                        })
                    })
                    put("required", buildJsonArray { add("path"); add("old_string"); add("new_string") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "search_in_files")
                put("description", "Cherche un texte dans les fichiers du projet. Renvoie chemin:ligne: contenu (max 100 résultats).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("query", buildJsonObject {
                            put("type", "string")
                            put("description", "Texte à chercher")
                        })
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Dossier où chercher (défaut '.')")
                        })
                        put("case_sensitive", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Respecter la casse (défaut false)")
                        })
                    })
                    put("required", buildJsonArray { add("query") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "run_command")
                put("description", "Exécute une commande shell (bash) dans le dossier du projet, avec le terminal de l'appli : git, npm, node, python, pip, clang, pkg, etc. Toutes les commandes sont autorisées. Renvoie le code de sortie et la sortie (stdout et stderr). Les programmes interactifs (éditeur, saisie clavier) ne marchent pas : utilise des options non interactives (ex: pkg install -y, npm init -y).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("command", buildJsonObject {
                            put("type", "string")
                            put("description", "Commande bash complète (ex: 'npm install && npm test')")
                        })
                        put("timeout_seconds", buildJsonObject {
                            put("type", "integer")
                            put("description", "Délai maximal en secondes (défaut 120, max 600)")
                        })
                    })
                    put("required", buildJsonArray { add("command") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "project_memory")
                put("description", "Mémoire de projet persistante (fichier .zagent/memoire.md), conservée entre les conversations : lit, enrichit ou vide la liste des faits durables à retenir (stack technique, conventions, préférences de l'utilisateur, décisions prises). Utilise-le quand l'utilisateur dit « retiens » / « souviens-toi », et consulte-le (action=read) au début d'une tâche où ces faits sont utiles au lieu de redemander.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("action", buildJsonObject {
                            put("type", "string")
                            put("description", "'read' pour lire la mémoire, 'write' pour ajouter un fait, 'clear' pour la vider")
                        })
                        put("fact", buildJsonObject {
                            put("type", "string")
                            put("description", "Le fait à enregistrer, une ligne courte — requis pour 'write'")
                        })
                    })
                    put("required", buildJsonArray { add("action") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "start_dev_server")
                put("description", "Lance un serveur de développement (ex: npm run dev) en arrière-plan, sans bloquer. Il reste actif jusqu'à stop_dev_server. Renvoie l'adresse locale détectée. L'utilisateur ouvre lui-même l'aperçu : ne l'ouvre pas.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("command", buildJsonObject {
                            put("type", "string")
                            put("description", "Commande du serveur (défaut : 'npm run dev')")
                        })
                    })
                    put("required", buildJsonArray {})
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "stop_dev_server")
                put("description", "Arrête le serveur de développement lancé avec start_dev_server.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                    put("required", buildJsonArray {})
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "update_todos")
                put("description", "Affiche et met à jour la liste de tâches en haut du chat. Pour toute demande de 3 étapes ou plus : appelle-le au début avec toutes les tâches (done=false), puis rappelle-le avec la liste COMPLÈTE à chaque tâche terminée (done=true).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("todos", buildJsonObject {
                            put("type", "array")
                            put("description", "Liste complète des tâches, dans l'ordre")
                            put("items", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("text", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Intitulé court de la tâche")
                                    })
                                    put("done", buildJsonObject {
                                        put("type", "boolean")
                                        put("description", "true si la tâche est terminée")
                                    })
                                })
                                put("required", buildJsonArray { add("text"); add("done") })
                            })
                        })
                    })
                    put("required", buildJsonArray { add("todos") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "ask_user")
                put("description", "Pose 1 à 4 questions à choix à l'utilisateur (cartes à toucher) quand la demande est floue ou qu'un choix important manque. Renvoie ses réponses.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("questions", buildJsonObject {
                            put("type", "array")
                            put("description", "Liste de 1 à 4 questions")
                            put("items", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("title", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Titre court (1 à 2 mots), ex: Audience")
                                    })
                                    put("question", buildJsonObject {
                                        put("type", "string")
                                        put("description", "La question posée")
                                    })
                                    put("options", buildJsonObject {
                                        put("type", "array")
                                        put("description", "2 à 4 options courtes")
                                        put("items", buildJsonObject { put("type", "string") })
                                    })
                                    put("multiple", buildJsonObject {
                                        put("type", "boolean")
                                        put("description", "true si plusieurs réponses sont possibles")
                                    })
                                })
                                put("required", buildJsonArray { add("title"); add("question"); add("options") })
                            })
                        })
                    })
                    put("required", buildJsonArray { add("questions") })
                })
            })
        })
        // ── feat-026 : find_files (glob) ────────────────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "find_files")
                put("description", "Cherche des fichiers par motif glob (ex: \"*.kt\", \"test_*.py\", \"src/**/*.ts\"). Renvoie les chemins relatifs (max 300).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("pattern", buildJsonObject {
                            put("type", "string")
                            put("description", "Motif glob : * = tout sauf /, ** = tout, ? = 1 caractère")
                        })
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Dossier de départ (défaut '.')")
                        })
                        put("max", buildJsonObject {
                            put("type", "integer")
                            put("description", "Nombre max de résultats (défaut 100)")
                        })
                    })
                    put("required", buildJsonArray { add("pattern") })
                })
            })
        })
        // ── feat-036 : file_info ─────────────────────────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "file_info")
                put("description", "Donne les informations d'un fichier : type, taille, date de modification, nombre de lignes si textuel.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier ou dossier")
                        })
                    })
                    put("required", buildJsonArray { add("path") })
                })
            })
        })
        // ── feat-037 : diff entre deux fichiers ─────────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "diff_files")
                put("description", "Compare deux fichiers ligne à ligne (+ ajouté / - supprimé / espace identique), max 400 lignes de diff.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path_a", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin du premier fichier (référence)")
                        })
                        put("path_b", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin du second fichier (comparé)")
                        })
                    })
                    put("required", buildJsonArray { add("path_a"); add("path_b") })
                })
            })
        })
        // ── feat-031 : fetch_url (lecture seule, taille limitée) ────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "fetch_url")
                put("description", "Télécharge une page/texte par URL (HTTPS obligatoire, sauf localhost) et renvoie le contenu textuel brut, plafonné. Utilise-le pour lire de la documentation.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("url", buildJsonObject {
                            put("type", "string")
                            put("description", "URL complète, ex: https://example.com/doc.md")
                        })
                        put("max_chars", buildJsonObject {
                            put("type", "integer")
                            put("description", "Nombre max de caractères renvoyés (défaut 20000)")
                        })
                    })
                    put("required", buildJsonArray { add("url") })
                })
            })
        })
        // ── feat-040 : recherche de symboles ────────────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "search_symbols")
                put("description", "Cherche des déclarations (fonction, classe, interface, struct, def…) par regex dans les fichiers texte du projet. Renvoie chemin:ligne: déclaration (max 80).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("query", buildJsonObject {
                            put("type", "string")
                            put("description", "Regex du nom ou motif à retrouver (ex: \"getUser\", \"parse.*Json\")")
                        })
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Dossier de départ (défaut '.')")
                        })
                    })
                    put("required", buildJsonArray { add("query") })
                })
            })
        })
        // ── feat-041 : infos du projet ──────────────────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "get_project_info")
                put("description", "Détecte le type de projet (langages, gestionnaire de paquets, scripts npm) et les extensions les plus fréquentes. Appelle-le au début si la stack est inconnue.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                    put("required", buildJsonArray {})
                })
            })
        })
        // ── feat-027/028 : move_file et copy_file (confirmés côté appli) ────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "move_file")
                put("description", "Déplace ou renomme un fichier du projet (l'utilisateur doit confirmer). Les dossiers parents de la destination sont créés si besoin.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("source", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier à déplacer")
                        })
                        put("destination", buildJsonObject {
                            put("type", "string")
                            put("description", "Nouveau chemin relatif (peut renommer le fichier)")
                        })
                    })
                    put("required", buildJsonArray { add("source"); add("destination") })
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "copy_file")
                put("description", "Copie un fichier du projet vers un autre chemin (l'utilisateur doit confirmer si l'écriture n'est pas autorisée).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("source", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier source")
                        })
                        put("destination", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif de la copie")
                        })
                    })
                    put("required", buildJsonArray { add("source"); add("destination") })
                })
            })
        })
        // ── feat-029 : apply_patch (diff multi-fichiers, atomique) ──────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "apply_patch")
                put("description", "Applique un patch à un ou plusieurs fichiers de façon ATOMIQUE (tout vérifié avant la première écriture). Formats acceptés : diff unifié (---/+++/@@) ou bloc \"*** Begin Patch / *** Update File: chemin / *** Add File: chemin / *** Delete File: chemin / *** End Patch\" avec lignes ' ancien, -supprimée, +ajoutée. Préfère edit_file pour un simple remplacement.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("patch", buildJsonObject {
                            put("type", "string")
                            put("description", "Le patch complet, plusieurs fichiers autorisés")
                        })
                    })
                    put("required", buildJsonArray { add("patch") })
                })
            })
        })
        // ── feat-030 : git_status / git_diff / git_commit ───────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "git_status")
                put("description", "Statut Git du projet (branche + fichiers modifiés), lecture seule.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                    put("required", buildJsonArray {})
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "git_diff")
                put("description", "Diff Git non commité (git diff HEAD), lecture seule, tronqué à 8000 caractères.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Limiter le diff à un fichier (optionnel)")
                        })
                    })
                    put("required", buildJsonArray {})
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "git_commit")
                put("description", "Commit Git avec message (TOUJOURS confirmé par l'utilisateur avec le diff des changements). Ne pousse pas : pour ça, utilise run_command avec git push.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("message", buildJsonObject {
                            put("type", "string")
                            put("description", "Message de commit (1re ligne si plusieurs)")
                        })
                    })
                    put("required", buildJsonArray { add("message") })
                })
            })
        })
        // ── feat-033/034 : run_tests et lint_code ───────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "run_tests")
                put("description", "Détecte le gestionnaire de tests du projet (gradlew, npm, pytest, make) et lance les tests, puis résume le résultat. Confirmé comme une commande.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("command", buildJsonObject {
                            put("type", "string")
                            put("description", "Commande de tests précise (optionnel — sinon détection auto)")
                        })
                    })
                    put("required", buildJsonArray {})
                })
            })
        })
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "lint_code")
                put("description", "Détecte l'outil de lint/format du projet (eslint, prettier, black, gradlew lint) et le lance avec correction automatique si disponible. Confirmé comme une commande.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                    put("required", buildJsonArray {})
                })
            })
        })
        // ── feat-032 : recherche web (optionnelle, réglage) ─────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "web_search")
                put("description", "Recherche web (DuckDuckGo) si l'utilisateur l'a activée dans les réglages. Renvoie titres + liens + extraits. Nécessite fetch_url pour lire une page.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("query", buildJsonObject {
                            put("type", "string")
                            put("description", "La recherche à lancer")
                        })
                    })
                    put("required", buildJsonArray { add("query") })
                })
            })
        })
        // ── feat-043 : logs du serveur de dev ───────────────────────────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "dev_server_logs")
                put("description", "Lit la sortie du serveur de développement en cours (erreurs, URLs, requêtes). Lecture seule.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                    put("required", buildJsonArray {})
                })
            })
        })
        // ── feat-044 : installation de paquets (toujours confirmée) ─────
        add(buildJsonObject {
            put("type", "function")
            put("function", buildJsonObject {
                put("name", "pkg_install")
                put("description", "Installe (action=install) ou désinstalle (action=uninstall) des paquets du terminal (pkg/apt). Toujours confirmé par l'utilisateur.")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("packages", buildJsonObject {
                            put("type", "array")
                            put("description", "Noms de paquets simples (ex: \"python\", \"nodejs\")")
                            put("items", buildJsonObject { put("type", "string") })
                        })
                        put("action", buildJsonObject {
                            put("type", "string")
                            put("description", "'install' (défaut) ou 'uninstall'")
                        })
                    })
                    put("required", buildJsonArray { add("packages") })
                })
            })
        })
    }

    /**
     * feat-045 — Liste des outils configurables individuellement
     * (autoriser / demander / refuser) dans Permissions & sécurité.
     */
    val nomsOutils = listOf(
        "list_dir", "read_file", "search_in_files", "find_files", "search_symbols",
        "file_info", "diff_files", "fetch_url", "web_search",
        "write_file", "edit_file", "apply_patch", "make_dir", "move_file", "copy_file",
        "delete_file", "run_command", "run_tests", "lint_code", "git_status", "git_diff",
        "git_commit", "pkg_install", "start_dev_server", "stop_dev_server", "dev_server_logs",
        "project_memory", "update_todos", "ask_user"
    )

    /**
     * err-034 — répare un JSON d'arguments tronqué ou malformé : fences markdown,
     * texte avant l'accolade, virgule finale, accolades/crochets non fermés,
     * chaîne coupée en plein milieu. Retourne toujours un objet (éventuellement vide).
     */
    fun argumentsLenients(brut: String): JsonObject {
        var s = brut.trim()
        if (s.isEmpty()) return JsonObject(emptyMap())
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            val fin = s.lastIndexOf("```")
            if (fin >= 0) s = s.substring(0, fin)
            s = s.trim()
        }
        val debut = s.indexOf('{')
        if (debut > 0) s = s.substring(debut)
        try {
            return (Json.parseToJsonElement(s) as? JsonObject) ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            // réparation : rééquilibre ce qui manque en ignorant le contenu des chaînes
        }
        var enChaine = false
        var echappe = false
        // err-098 — virgules finales avant } ou ] : JSON strict les refuse ; on les
        // retire hors des chaînes (ce correctif ne court qu'après échec du premier
        // parse, donc sur du JSON déjà malformé — jamais sur du JSON valide).
        val sansVirgules = StringBuilder()
        enChaine = false
        echappe = false
        for (c in s) {
            if (echappe) { echappe = false; sansVirgules.append(c); continue }
            if (c == '\\' && enChaine) { echappe = true; sansVirgules.append(c); continue }
            if (c == '"') { enChaine = !enChaine; sansVirgules.append(c); continue }
            if (enChaine) { sansVirgules.append(c); continue }
            if (c == '}' || c == ']') {
                var i = sansVirgules.length - 1
                while (i >= 0 && sansVirgules[i].isWhitespace()) i--
                if (i >= 0 && sansVirgules[i] == ',') sansVirgules.delete(i, sansVirgules.length)
            }
            sansVirgules.append(c)
        }
        s = sansVirgules.toString()
        val pile = ArrayDeque<Char>()
        enChaine = false
        echappe = false
        for (c in s) {
            if (echappe) { echappe = false; continue }
            if (c == '\\' && enChaine) { echappe = true; continue }
            if (c == '"') { enChaine = !enChaine; continue }
            if (enChaine) continue
            if (c == '{' || c == '[') pile.addLast(c)
            if (c == '}' || c == ']') {
                if (pile.isNotEmpty() &&
                    ((pile.last() == '{' && c == '}') || (pile.last() == '[' && c == ']'))
                ) pile.removeLast()
            }
        }
        var t = s.trimEnd().removeSuffix(",").trimEnd()
        if (enChaine) t += "\""
        while (pile.isNotEmpty()) t += if (pile.removeLast() == '{') "}" else "]"
        return try {
            (Json.parseToJsonElement(t) as? JsonObject) ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
    }

    fun executeTool(fileRepo: FileRepository, name: String, argumentsJson: String): String {
        return try {
            // err-034 — tolère les arguments JSON partiels ou malformés.
            val jsonObject = argumentsLenients(argumentsJson)
            when (name) {
                "list_dir" -> {
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: "."
                    fileRepo.listDir(path)
                }
                "read_file" -> {
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: ""
                    val tout = fileRepo.readFile(path)
                    // feat-035 — lecture par lignes pour les gros fichiers :
                    // offset_line (1-based) + lines (nombre) optionnels.
                    val offset = jsonObject["offset_line"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                    val nb = jsonObject["lines"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                    if (tout.startsWith("Erreur") || (offset <= 1 && nb <= 0)) tout
                    else {
                        val lignes = tout.lines()
                        val debut = (offset.coerceAtLeast(1) - 1).coerceAtMost(lignes.lastIndex.coerceAtLeast(0))
                        val fin = if (nb > 0) (debut + nb).coerceAtMost(lignes.size) else lignes.size
                        "[lignes " + (debut + 1) + "-" + fin + " sur " + lignes.size + "]\n" +
                            lignes.subList(debut, fin).joinToString("\n")
                    }
                }
                "make_dir" -> {
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: ""
                    fileRepo.makeDir(path)
                }
                "write_file" -> {
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: ""
                    val content = jsonObject["content"]?.jsonPrimitive?.content ?: ""
                    fileRepo.writeFile(path, content)
                }
                "search_in_files" -> {
                    val query = jsonObject["query"]?.jsonPrimitive?.content ?: ""
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: "."
                    val cs = jsonObject["case_sensitive"]?.jsonPrimitive?.booleanOrNull ?: false
                    ProjectSearch.search(fileRepo, query, path, cs)
                }
                "find_files" -> {
                    // feat-026 — recherche par motif glob sur l'arborescence.
                    val pattern = (jsonObject["pattern"]?.jsonPrimitive?.contentOrNull ?: "").trim()
                    val base = jsonObject["path"]?.jsonPrimitive?.contentOrNull ?: "."
                    val max = (jsonObject["max"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100).coerceIn(1, 300)
                    if (pattern.isEmpty()) "Erreur: pattern requis (ex: \"*.kt\")."
                    else {
                        val re = globVersRegex(pattern)
                        val out = ArrayList<String>()
                        parcourir(fileRepo, base, max) { p, isDir ->
                            if (!isDir) {
                                val ok = if (pattern.contains('/')) re.matches(p) else re.matches(p.substringAfterLast('/'))
                                if (ok) out.add(p)
                            }
                        }
                        if (out.isEmpty()) "Aucun fichier ne correspond à $pattern"
                        else out.joinToString("\n")
                    }
                }
                "file_info" -> {
                    // feat-036 — taille, date, type d'un fichier.
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: ""
                    fileRepo.fileInfo(path)
                }
                "diff_files" -> {
                    // feat-037 — diff ligne à ligne entre deux fichiers.
                    val a = jsonObject["path_a"]?.jsonPrimitive?.content ?: ""
                    val b = jsonObject["path_b"]?.jsonPrimitive?.content ?: ""
                    if (a.isEmpty() || b.isEmpty()) "Erreur: path_a et path_b requis."
                    else {
                        val ca = fileRepo.readFile(a)
                        val cb = fileRepo.readFile(b)
                        if (ca.startsWith("Erreur:")) ca
                        else if (cb.startsWith("Erreur:")) cb
                        else {
                            val d = DiffUtil.diff(ca, cb)
                            buildString {
                                append("Diff ").append(a).append(" → ").append(b)
                                append(" : +").append(d.added).append(" / -").append(d.removed).append(" lignes\n")
                                val lignes = d.lines.take(400)
                                for (l in lignes) {
                                    when (l.kind) {
                                        DiffUtil.Kind.ADD -> append("+ ")
                                        DiffUtil.Kind.DEL -> append("- ")
                                        DiffUtil.Kind.CTX -> append("  ")
                                    }
                                    append(l.text).append('\n')
                                }
                                if (d.lines.size > 400) append("… (diff tronqué à 400 lignes)")
                            }
                        }
                    }
                }
                "fetch_url" -> {
                    // feat-031 — lecture seule HTTPS, taille plafonnée.
                    val url = (jsonObject["url"]?.jsonPrimitive?.contentOrNull ?: "").trim()
                    val maxChars = (jsonObject["max_chars"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 20_000).coerceIn(500, 100_000)
                    if (url.isEmpty()) "Erreur: url requise."
                    else fetchUrlTexte(url, maxChars)
                }
                "search_symbols" -> {
                    // feat-040 — déclarations par regex dans les fichiers texte.
                    val query = (jsonObject["query"]?.jsonPrimitive?.contentOrNull ?: "").trim()
                    val base = jsonObject["path"]?.jsonPrimitive?.contentOrNull ?: "."
                    if (query.isEmpty()) "Erreur: query (regex) requise."
                    else {
                        val re = runCatching { Regex(query, RegexOption.IGNORE_CASE) }.getOrElse {
                            return "Erreur: regex invalide : " + (it.message ?: "?")
                        }
                        val decl = Regex(
                            "\\b(fun|function|def|class|interface|object|struct|enum|impl|trait|type|record)\\b[\\w:<> ,]*"
                        )
                        val out = ArrayList<String>()
                        var fichiersLus = 0
                        parcourir(fileRepo, base, 400) { p, isDir ->
                            if (!isDir && out.size < 80 && fichiersLus < 60 && fileRepo.isTextFile(p)) {
                                fichiersLus++
                                val contenu = fileRepo.readFile(p)
                                if (!contenu.startsWith("Erreur:")) {
                                    contenu.lines().forEachIndexed { i, ligne ->
                                        if (out.size < 80 && decl.containsMatchIn(ligne) && re.containsMatchIn(ligne)) {
                                            out.add(p + ":" + (i + 1) + ": " + ligne.trim().take(140))
                                        }
                                    }
                                }
                            }
                        }
                        if (out.isEmpty()) "Aucune déclaration trouvée pour « $query »"
                        else out.joinToString("\n")
                    }
                }
                "get_project_info" -> {
                    // feat-041 — langages, gestionnaire de paquets, scripts.
                    val infos = ArrayList<String>()
                    val racine = fileRepo.listEntries(".")
                    val noms = racine.map { it.name }.toSet()
                    fun si(nom: String, info: String) { if (nom in noms) infos.add(info) }
                    si("package.json", "Node.js / npm — scripts possibles via package.json")
                    si("yarn.lock", "gestionnaire : Yarn")
                    si("pnpm-lock.yaml", "gestionnaire : pnpm")
                    si("build.gradle.kts", "Gradle (Kotlin DSL) — projet Android/JVM")
                    si("build.gradle", "Gradle (Groovy DSL)")
                    si("settings.gradle.kts", "build multi-modules Gradle")
                    si("pom.xml", "Maven")
                    si("requirements.txt", "Python (pip)")
                    si("pyproject.toml", "Python (pyproject)")
                    si("Cargo.toml", "Rust (Cargo)")
                    si("go.mod", "Go")
                    si("pubspec.yaml", "Flutter/Dart")
                    si("composer.json", "PHP (Composer)")
                    si("Gemfile", "Ruby (Bundler)")
                    si("Dockerfile", "conteneurisation : Docker")
                    si("Makefile", "cibles Make disponibles")
                    if ("gradlew" in noms) infos.add("wrapper Gradle présent (./gradlew)")
                    // Scripts npm lisibles.
                    if ("package.json" in noms) {
                        val pj = runCatching {
                            Json.parseToJsonElement(fileRepo.readFile("package.json")).jsonObject
                        }.getOrNull()
                        val scripts = (pj?.get("scripts") as? JsonObject)?.keys?.take(8)
                        if (!scripts.isNullOrEmpty()) infos.add("scripts npm : " + scripts.joinToString(", "))
                    }
                    // Extensions les plus fréquentes.
                    val extensions = HashMap<String, Int>()
                    parcourir(fileRepo, ".", 400) { chemin, isDir ->
                        if (!isDir) {
                            val ext = chemin.substringAfterLast('.', "")
                            if (ext.isNotEmpty() && ext.length <= 6) extensions[ext] = (extensions[ext] ?: 0) + 1
                        }
                    }
                    val top = extensions.entries.sortedByDescending { it.value }.take(6)
                        .joinToString(", ") { (k, v) -> ".$k ($v)" }
                    buildString {
                        append("Projet : ").append(fileRepo.folderName()).append('\n')
                        if (infos.isEmpty()) append("Type : non reconnu (projet générique)\n")
                        else infos.forEach { append("- ").append(it).append('\n') }
                        if (top.isNotEmpty()) append("Extensions fréquentes : ").append(top)
                    }
                }
                else -> {
                    // err-036 — réponse standard pour un outil inconnu : la liste des
                    // outils disponibles est renvoyée pour que le modèle se corrige.
                    "Erreur: l'outil « $name » n'existe pas. Outils disponibles : " +
                        nomsOutils.joinToString(", ") + " — utilise l'un de ceux-ci."
                }
            }
        } catch (e: Exception) {
            "Erreur d'exécution de l'outil $name: ${e.localizedMessage}"
        }
    }

    // ── feat-026/031/040/041 — helpers purs des nouveaux outils ────────────

    /** Dossiers exclus des parcours d'outils (dépendances, artefacts). */
    private val dossiersIgnores = setOf(
        ".git", "node_modules", "build", ".gradle", ".idea", ".godot",
        "__pycache__", ".venv", "venv", "dist", ".next"
    )

    /**
     * Parcourt récursivement l'arborescence via [FileRepository.listEntries]
     * (max 6 niveaux, plafonné à [max] fichiers) et appelle [visit] pour chaque
     * entrée avec son chemin relatif. Retourne le nombre de fichiers visités.
     */
    private fun parcourir(
        fileRepo: FileRepository,
        path: String,
        max: Int,
        visit: (chemin: String, isDir: Boolean) -> Unit
    ): Int {
        var fichiers = 0
        fun rec(dirPath: String, depth: Int) {
            if (depth > 6 || fichiers >= max) return
            for (e in fileRepo.listEntries(dirPath)) {
                if (fichiers >= max) return
                if (e.isDir) {
                    if (e.name in dossiersIgnores) continue
                    visit(e.path, true)
                    rec(e.path, depth + 1)
                } else {
                    fichiers++
                    visit(e.path, false)
                }
            }
        }
        rec(path.ifEmpty { "." }, 1)
        return fichiers
    }

    /** Convertit un motif glob (*, **, ?) en Regex. */
    private fun globVersRegex(glob: String): Regex {
        val sb = StringBuilder()
        var i = 0
        while (i < glob.length) {
            val ch = glob[i]
            if (ch == '*' && i + 1 < glob.length && glob[i + 1] == '*') {
                sb.append(".*")
                i += 2
                if (i < glob.length && glob[i] == '/') i++
            } else when (ch) {
                '*' -> { sb.append("[^/]*"); i++ }
                '?' -> { sb.append("[^/]"); i++ }
                else -> { sb.append(Regex.escape(ch.toString())); i++ }
            }
        }
        return Regex(sb.toString())
    }

    /**
     * feat-031/032 — Télécharge une URL en texte brut.
     * HTTPS obligatoire (http toléré pour localhost uniquement, cohérent avec
     * corr-042), réponse plafonnée à 500 Ko, contenus textuels seulement.
     */
    fun fetchUrlTexte(urlBrute: String, maxChars: Int): String {
        val u = urlBrute.trim()
        if (u.startsWith("http://")) {
            val hote = u.removePrefix("http://").substringBefore('/').substringBefore(':')
            val local = hote == "localhost" || hote == "127.0.0.1" || hote == "::1"
            if (!local) return "Erreur: seule l'URLs HTTPS est autorisée (http toléré pour localhost uniquement)."
        } else if (!u.startsWith("https://")) {
            return "Erreur: URL invalide — commence par https://"
        }
        return try {
            val conn = java.net.URL(u).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "ZAgent/1.0 (fetch_url; lecture seule)")
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return "Erreur: HTTP $code pour $u"
            }
            val ctype = (conn.contentType ?: "text/html").substringBefore(';')
            val textuel = ctype.startsWith("text/") || ctype.contains("json") ||
                ctype.contains("xml") || ctype.contains("javascript")
            if (!textuel) {
                conn.disconnect()
                return "Erreur: type $ctype non textuel — fetch_url lit uniquement du texte."
            }
            val body = conn.inputStream.use { inp ->
                val buf = ByteArray(8192)
                val sb = StringBuilder()
                var total = 0
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > 512_000) return "Erreur: réponse > 500 Ko — trop grande pour fetch_url."
                    sb.append(String(buf, 0, n, Charsets.UTF_8))
                }
                sb.toString()
            }
            conn.disconnect()
            "Contenu de $u (" + body.length + " caractères) :\n" + body.take(maxChars)
        } catch (e: Exception) {
            "Erreur: fetch_url a échoué : " + (e.message ?: e.javaClass.simpleName)
        }
    }

    // ── feat-029 — parsing du patch (pur, testable) ────────────────────────

    /** Un fichier ciblé par un patch : action, chemin, contenu ancien/nouveau du hunk. */
    data class PatchHunk(val action: String, val path: String, val hunkOld: String, val hunkNew: String)

    /**
     * Analyse un patch au format diff unifié ou bloc "*** Begin Patch".
     * Renvoie les hunks dans l'ordre, groupables par chemin. Liste vide si
     * rien d'exploitable. Les lignes de contexte ' ', suppressions '-',
     * ajouts '+' alimentent respectivement hunkOld, hunkNew.
     */
    fun parsePatch(patch: String): List<PatchHunk> {
        val lignes = patch.replace("\r\n", "\n").lines()
        val out = ArrayList<PatchHunk>()
        var action = ""
        var chemin = ""
        val old = StringBuilder()
        val new = StringBuilder()
        var dansHunk = false

        fun terminerHunk() {
            // feat-029 — un hunk Delete n'a aucune ligne +/- : il est émis sur
            // la seule présence de sa section ; sinon il faut des lignes de hunk.
            if (chemin.isNotEmpty() && (dansHunk || action == "delete")) {
                out.add(PatchHunk(action, chemin, old.toString(), new.toString()))
            }
            old.setLength(0); new.setLength(0); dansHunk = false
        }

        val aiderRe = Regex("^\\*\\*\\* (Update|Add|Delete) File: (.+)$")
        val minusRe = Regex("^--- (?:a/)?(.+)$")
        val plusRe = Regex("^\\+\\+\\+ (?:b/)?(.+)$")

        for (l in lignes) {
            val aider = aiderRe.find(l)
            if (aider != null) {
                terminerHunk()
                action = aider.groupValues[1].lowercase()
                chemin = aider.groupValues[2].trim()
                continue
            }
            if (l.startsWith("*** End Patch") || l.startsWith("*** Begin Patch")) continue
            // En-têtes diff unifié — uniquement HORS hunk (une ligne supprimée
            // commençant par "--- " reste du contenu).
            if (!dansHunk) {
                val minus = minusRe.find(l)
                if (minus != null) {
                    val cible = minus.groupValues[1].trim()
                    if (cible != "/dev/null") {
                        terminerHunk()
                        action = "update"
                        chemin = cible
                    }
                    continue
                }
                val plus = plusRe.find(l)
                if (plus != null) {
                    val cible = plus.groupValues[1].trim()
                    if (cible != "/dev/null") chemin = cible
                    continue
                }
            }
            if (l.startsWith("@@")) { terminerHunk(); dansHunk = true; continue }
            if (l.startsWith("diff ") || l.startsWith("index ")) { terminerHunk(); continue }
            if (action.isNotEmpty()) {
                when {
                    l.startsWith("+") -> { if (!dansHunk) dansHunk = true; new.append(l.substring(1)).append('\n') }
                    l.startsWith("-") -> { if (!dansHunk) dansHunk = true; old.append(l.substring(1)).append('\n') }
                    l.startsWith(" ") -> { if (!dansHunk) dansHunk = true; old.append(l.substring(1)).append('\n'); new.append(l.substring(1)).append('\n') }
                    l.isEmpty() && dansHunk -> { old.append('\n'); new.append('\n') }
                    else -> terminerHunk()
                }
            }
        }
        terminerHunk()
        return out.filter { it.path.isNotEmpty() }
    }
}

/** Une question à choix posée par l'agent via l'outil ask_user. */
data class AskQuestion(
    val title: String,
    val question: String,
    val options: List<String>,
    val multiple: Boolean
)

/** Lit les arguments JSON de ask_user ; renvoie une liste vide si rien d'exploitable. */
fun parseAskQuestions(argumentsJson: String): List<AskQuestion> = try {
    val root = Json.parseToJsonElement(argumentsJson) as? JsonObject
    val arr = root?.get("questions") as? JsonArray
    arr.orEmpty().mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val question = (o["question"] as? JsonPrimitive)?.contentOrNull?.trim()
        if (question.isNullOrBlank()) return@mapNotNull null
        val title = (o["title"] as? JsonPrimitive)?.contentOrNull?.trim()
            .takeUnless { it.isNullOrBlank() } ?: question.take(24)
        val options = (o["options"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            .filter { it.isNotBlank() }
            .take(6)
        val multiple = (o["multiple"] as? JsonPrimitive)?.booleanOrNull ?: false
        AskQuestion(title, question, options, multiple)
    }.take(4)
} catch (e: Exception) {
    emptyList()
}

/** Une tâche de la liste affichée en haut du chat (outil update_todos) ; corr-077 : sérialisable pour la persistance par conversation. */
@kotlinx.serialization.Serializable
data class TodoItem(val text: String, val done: Boolean)

/** Lit les arguments JSON de update_todos ; renvoie une liste vide si rien d'exploitable. */
fun parseTodos(argumentsJson: String): List<TodoItem> = try {
    val root = Json.parseToJsonElement(argumentsJson) as? JsonObject
    val arr = root?.get("todos") as? JsonArray
    arr.orEmpty().mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val text = (o["text"] as? JsonPrimitive)?.contentOrNull?.trim()
        if (text.isNullOrBlank()) return@mapNotNull null
        val done = (o["done"] as? JsonPrimitive)?.booleanOrNull ?: false
        TodoItem(text.take(120), done)
    }.take(30)
} catch (e: Exception) {
    emptyList()
}
