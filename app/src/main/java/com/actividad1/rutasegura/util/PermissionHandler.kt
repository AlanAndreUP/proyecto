package com.actividad1.rutasegura.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.actividad1.rutasegura.ui.theme.screen.LoadingScreen
import com.actividad1.rutasegura.ui.theme.screen.RetryButton

@Composable
fun PermissionHandler(
    permissions: List<String>,
    onPermissionsGranted: @Composable () -> Unit,
    rationaleContent: @Composable (Boolean, (Boolean) -> Unit) -> Unit,
    onPermissionsDenied: @Composable (Boolean) -> Unit,
    permissionRequester: (List<String>, (Map<String, Boolean>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var permissionState by remember { mutableStateOf<PermissionStatus>(PermissionStatus.Unknown) }
    val showRationale by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = checkAppPermissions(permissions, context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        permissionState = checkAppPermissions(permissions, context)
    }

    when (permissionState) {
        PermissionStatus.Granted -> onPermissionsGranted()
        PermissionStatus.Denied -> {
            onPermissionsDenied(false)
            RetryButton { permissionState = PermissionStatus.ShouldRequest }
        }
        PermissionStatus.PermanentlyDenied -> onPermissionsDenied(true)
        PermissionStatus.ShouldShowRationale -> {
            rationaleContent(true) { confirmed ->
                permissionState = if (confirmed) PermissionStatus.ShouldRequest else PermissionStatus.Denied
            }
        }
        PermissionStatus.ShouldRequest -> {
            LaunchedEffect(Unit) {
                permissionRequester(permissions) { result ->
                    permissionState = evaluatePermissionsResult(result, context)
                }
            }
            LoadingScreen()
        }
        PermissionStatus.Unknown -> LoadingScreen()
    }
}
