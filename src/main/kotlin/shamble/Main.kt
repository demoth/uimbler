package shamble

import androidx.compose.desktop.ui.tooling.preview.Preview
import shamble.ShambleInterface.InitShamble
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking

// Data classes for our chat app
data class Contact(val id: Int, val name: String)
data class Message(val contactId: Int, val content: String, val isFromMe: Boolean)


@Composable
@Preview
fun App() {
    // Sample data
    val contacts = remember {
        listOf(
            Contact(1, "Sorseg"),
            Contact(2, "Kanedias"),
            Contact(3, "Denolia"),
            Contact(4, "Demoth")
        )
    }

    val messagesMap = remember {
        mutableStateMapOf(
            1 to mutableStateListOf(
                Message(1, "Hey there!", false),
                Message(1, "Hello Alice! How are you?", true)
            ),
            2 to mutableStateListOf(
                Message(2, "What's up?", false),
                Message(2, "Not much, working on a Compose app", true),
                Message(2, "That sounds cool!", false)
            ),
            3 to mutableStateListOf(),
            4 to mutableStateListOf()
        )
    }

    // App state
    var selectedContactId by remember { mutableStateOf(1) }
    var isContactListVisible by remember { mutableStateOf(true) }
    var newMessageText by remember { mutableStateOf("") }

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            // Collapsible Contact List
            if (isContactListVisible) {
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Text("Contacts", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(contacts.size) { contactIndex ->
                            val contact = contacts[contactIndex]
                            ContactItem(
                                contact = contact,
                                isSelected = contact.id == selectedContactId,
                                onClick = { selectedContactId = contact.id }
                            )
                        }
                    }
                }
            }

            // Chat area
            Column(modifier = Modifier.fillMaxSize()) {
                // Chat header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isContactListVisible = !isContactListVisible }) {
                        Icon(
                            if (isContactListVisible) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                            contentDescription = "Toggle Contact List"
                        )
                    }

                    Text(
                        "Chat with " + (contacts.find { it.id == selectedContactId }?.name ?: ""),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
                }

                Divider()

                // Messages area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    val messages = messagesMap[selectedContactId] ?: mutableStateListOf()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = false
                    ) {
                        items(messages.size) { index ->
                            MessageItem(message = messages[index])
                        }
                    }
                }

                // Input area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newMessageText,
                        onValueChange = { newMessageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            extracted(newMessageText, messagesMap, selectedContactId)
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

private fun extracted(
    newMessageText: String,
    messagesMap: SnapshotStateMap<Int, SnapshotStateList<Message>>,
    selectedContactId: Int
) {
    var newMessageText1 = newMessageText
    if (newMessageText1.isNotBlank()) {
        val messages = messagesMap[selectedContactId] ?: mutableStateListOf()
        messages.add(Message(selectedContactId, newMessageText1, true))
        messagesMap[selectedContactId] = messages
        newMessageText1 = ""
    }
}

@Composable
fun MessageItem(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                    )
                )
                .background(if (message.isFromMe) Color(0xFF448AFF) else Color.LightGray)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.isFromMe) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun ContactItem(contact: Contact, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color.Gray.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF3F51B5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.first().toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = contact.name)
    }
}


fun main() = application {

    val channel = ManagedChannelBuilder
        .forAddress("localhost", 42147)
        .usePlaintext()
        .build()

    val client = ShambleGrpcKt.ShambleCoroutineStub(channel)
    val result = runBlocking {
        client.init(InitShamble.newBuilder().setName("Tupitsa").build())
    }

    println("Result: $result")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hackachat2000"
    ) {
        App()
    }
}
