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
