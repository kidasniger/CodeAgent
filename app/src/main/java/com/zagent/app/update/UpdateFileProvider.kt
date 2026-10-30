package com.zagent.app.update

import androidx.core.content.FileProvider

/** Sous-classe dédiée pour éviter tout conflit avec un autre FileProvider de l'application. */
class UpdateFileProvider : FileProvider()