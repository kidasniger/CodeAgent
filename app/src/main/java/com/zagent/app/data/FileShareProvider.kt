package com.zagent.app.data

import androidx.core.content.FileProvider

/**
 * maj-076 — Sous-classe FileProvider dédiée au partage des fichiers du projet
 * (« Ouvrir avec », « Partager »). Authority séparée (.fileshareprovider,
 * @xml/file_share_paths : dossier privé home/) pour que le provider de mise à
 * jour (.updateprovider, @xml/update_file_paths) ne serve qu'aux APK.
 */
class FileShareProvider : FileProvider()
