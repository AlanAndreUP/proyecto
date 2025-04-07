package com.actividad1.rutasegura.data.model


sealed class ScanResult {
    data class Success(val content: String) : ScanResult()
    object Error : ScanResult()
    object Cancelled : ScanResult()
    object Idle : ScanResult()
}