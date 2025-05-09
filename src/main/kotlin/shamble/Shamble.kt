package shamble

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import java.net.Socket

private const val SHAMBLE_PORT = 33445
private const val SHAMBLE_HOST = "127.0.0.1"
private const val BACKEND_EXECUTABLE = "./shamble-grpc.0.3.0.AppImage"

// Create a state holder class that can be accessed from anywhere
class AppState {
    val nameState: MutableState<String> = mutableStateOf("UserName")
    val myPublicKeyState: MutableState<String> = mutableStateOf("123")
    val theirsPublicKeyState: MutableState<String> = mutableStateOf("456")

    val playbackDevices = mutableStateOf(listOf<String>())
    val recordingDevices = mutableStateOf(listOf<String>())

    val selectedPlaybackDevice = mutableStateOf<String?>(null)
    val selectedRecordingDevice = mutableStateOf<String?>(null)

    val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(SHAMBLE_HOST, SHAMBLE_PORT)
        .usePlaintext()
        .build()
    val client = ShambleGrpcKt.ShambleCoroutineStub(channel)

    val backendScope = CoroutineScope(Dispatchers.Default)

    var shambleProcess: Process? = null


    // Update functions that can be called from any thread
    fun updateName(newName: String) {
        // When called from a non-UI thread, ensure updates happen on the UI thread
        // using a dispatcher like Dispatchers.Main
        nameState.value = newName
    }

    fun updateMyPublicKey(newKey: String) {
        myPublicKeyState.value = newKey
    }

    fun updateTheirPublicKey(newKey: String) {
        theirsPublicKeyState.value = newKey
    }
}


@Composable
@Preview
fun ShambleApp(appState: AppState) {
    MaterialTheme {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                TextField(
                    label = { Text("Name") },
                    value = appState.nameState.value,
                    onValueChange = { appState.updateName(it) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                TextField(
                    label = { Text("My Public Key") },
                    value = appState.myPublicKeyState.value,
                    onValueChange = { appState.updateMyPublicKey(it) },
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    TextField(
                        label = { Text("Their Public Key") },
                        value = appState.theirsPublicKeyState.value,
                        onValueChange = { appState.updateTheirPublicKey(it) },
                    )
                    TextButton(
                        onClick = {
                            runBlocking {
                                appState.client.connectTo(
                                    ShambleInterface.PubKey.newBuilder().setKey(appState.theirsPublicKeyState.value).build()
                                )
                            }
                        },
                    ) {
                        Text("Connect")
                    }
                }
            }

            TextButton(
                onClick = {
                    appState.backendScope.startServiceOrEnsureRunning(appState)
                },
                modifier = Modifier.fillMaxWidth().padding(8.dp)

            ) {
                Text("Start/Connect to Service")
            }

            TextButton(
                onClick = {
                    val result =
                        runBlocking {
                            appState.client.init(
                                ShambleInterface.InitShamble.newBuilder().setName(appState.nameState.value).build()
                            )
                        }
                    appState.myPublicKeyState.value = result.publicKey
                    println("Connected to backend, fetching playback devices")
                    runBlocking {
                        val audioDevices = appState.client.getAudioDevices(ShambleInterface.Void.newBuilder().build())
                        appState.playbackDevices.value = audioDevices.playbackDevicesList
                        appState.recordingDevices.value = audioDevices.recordingDevicesList
                    }

                },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Connect to backend")
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    Text("Playback devices:")
                    appState.playbackDevices.value.forEach { device ->
                        TextButton({
                            appState.selectedPlaybackDevice.value = device
                            println("Selected playback device: $device")
                            runBlocking {
                                appState.client.usePlaybackDevice(
                                    ShambleInterface.PlaybackDevice.newBuilder().setDeviceName(appState.selectedPlaybackDevice.value).build()
                                )
                            }
                        }) {
                            Text(
                                if (device == appState.selectedPlaybackDevice.value) { " Selected: $device" } else { device },
                            )
                        }

                    }
                }
                Column {
                    Text("Recording devices:")
                    appState.recordingDevices.value.forEach { device ->
                        TextButton({
                            appState.selectedRecordingDevice.value = device
                            println("Selected recording device: $device")
                            runBlocking {
                                appState.client.useRecordingDevice(
                                    ShambleInterface.RecordingDevice.newBuilder().setDeviceName(appState.selectedRecordingDevice.value).build()
                                )
                            }

                        }) {
                            Text(
                                if (device == appState.selectedRecordingDevice.value) { " Selected: $device" } else { device }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {

    // Create a singleton instance or use dependency injection
    val appState = AppState()

    Window(
        onCloseRequest = {
            appState.shambleProcess?.destroy()
            exitApplication()
        },
        title = "Hackachat2000"
    ) {
        ShambleApp(appState)
    }
}

fun CoroutineScope.startServiceOrEnsureRunning(appState: AppState) {
    try {
        Socket(SHAMBLE_HOST, SHAMBLE_PORT).use {
            println("Service is already running on port $SHAMBLE_PORT")
            return
        }
    } catch (_: Exception) {
        println("No service found on port $SHAMBLE_PORT, starting new instance...")
    }

    val processBuilder = ProcessBuilder(BACKEND_EXECUTABLE)
    processBuilder.redirectErrorStream(true)
    processBuilder.environment()["PORT"] = SHAMBLE_PORT.toString()
    appState.shambleProcess = processBuilder.start()
    appState.shambleProcess?.let { process ->
        val processOutput = process.inputReader()
        launch {
            repeat(10) {
                println("Waiting for service to start...")
                println(processOutput.readLine())
                if (!process.isAlive) {
                    print(".")
                    delay(500)
                } else {
                    println("Service started!")
                    return@launch
                }
            }
        }
    }
}
