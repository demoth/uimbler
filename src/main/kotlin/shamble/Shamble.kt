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
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking


// Create a state holder class that can be accessed from anywhere
class AppState {
    val nameState: MutableState<String> = mutableStateOf("Demoto")
    val myPublicKeyState: MutableState<String> = mutableStateOf("123")
    val theirsPublicKeyState: MutableState<String> = mutableStateOf("456")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", 33445)
        .usePlaintext()
        .build()
    val client = ShambleGrpcKt.ShambleCoroutineStub(channel)


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
                modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                TextField(
                    label = { Text("My Public Key") },
                    value = appState.myPublicKeyState.value,
                    onValueChange = { appState.updateMyPublicKey(it) },
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                TextField(
                    label = { Text("Their Public Key") },
                    value = appState.theirsPublicKeyState.value,
                    onValueChange = { appState.updateTheirPublicKey(it) },
                )
            }

            TextButton(
                onClick = {
                    val result = runBlocking {
                        appState.client.init(ShambleInterface.InitShamble.newBuilder().setName(appState.nameState.value).build())
                    }
                    appState.myPublicKeyState.value = result.publicKey
                    println("Connected to backend")
                },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Connect to backend")
            }
        }
    }
}

fun main() = application {

    // Create a singleton instance or use dependency injection
    val appState = AppState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hackachat2000"
    ) {
        ShambleApp(appState)
    }


    /*
    */

}