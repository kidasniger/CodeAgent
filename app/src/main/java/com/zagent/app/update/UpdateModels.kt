package com.zagent.app.update

data class ApkAsset(
    val name: String,
    val size: Long,
    val url: String,
    val sha256: String?
)

data class ReleaseInfo(
    val tag: String,
    val notes: String,
    val apk: ApkAsset,
    val sha256Url: String?
)

sealed class ParseResult {
    class Success(val info: ReleaseInfo) : ParseResult()
    class Failure(val message: String) : ParseResult()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    class UpToDate(val currentVersion: String) : UpdateState()
    class Available(val info: ReleaseInfo, val currentVersion: String) : UpdateState()
    class Downloading(val info: ReleaseInfo, val percent: Int, val downloaded: Long, val total: Long) : UpdateState()
    class Ready(val info: ReleaseInfo, val apkPath: String) : UpdateState()
    class NeedsPermission(val info: ReleaseInfo, val apkPath: String) : UpdateState()
    class Error(val message: String, val info: ReleaseInfo? = null) : UpdateState()
}