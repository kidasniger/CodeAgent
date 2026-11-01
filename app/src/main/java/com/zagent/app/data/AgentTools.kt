package com.zagent.app.data

import kotlinx.serialization.json.*

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
                put("description", "Lit un fichier texte UTF-8 (tronqué à 20000 caractères).")
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Chemin relatif du fichier")
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
    }

    fun executeTool(fileRepo: FileRepository, name: String, argumentsJson: String): String {
        return try {
            val jsonObject = Json.parseToJsonElement(argumentsJson).jsonObject
            when (name) {
                "list_dir" -> {
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: "."
                    fileRepo.listDir(path)
                }
                "read_file" -> {
                    val path = jsonObject["path"]?.jsonPrimitive?.content ?: ""
                    fileRepo.readFile(path)
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
                else -> "Erreur: Outil inconnu $name"
            }
        } catch (e: Exception) {
            "Erreur d'exécution de l'outil $name: ${e.localizedMessage}"
        }
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

/** Une tâche de la liste affichée en haut du chat (outil update_todos). */
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
