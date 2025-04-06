package com.actividad1.rutasegura.util

import android.content.Context
import androidx.activity.ComponentActivity

fun checkAppPermissions(
    permissions: List<String>,
    context: Context
): PermissionStatus {
    val denied = permissions.filter {
        context.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    if (denied.isEmpty()) return PermissionStatus.Granted

    val permanentlyDenied = denied.any {
        !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            context as ComponentActivity, it
        )
    }

    return if (permanentlyDenied) PermissionStatus.PermanentlyDenied
    else PermissionStatus.ShouldShowRationale
}

fun evaluatePermissionsResult(
    result: Map<String, Boolean>,
    context: Context
): PermissionStatus {
    return when {
        result.all { it.value } -> PermissionStatus.Granted
        result.any { !it.value } -> checkAppPermissions(result.keys.toList(), context)
        else -> PermissionStatus.Unknown
    }
}
