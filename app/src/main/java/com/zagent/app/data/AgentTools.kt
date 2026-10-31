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
