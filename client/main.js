// Create WebSocket connection
const socket = new WebSocket("ws://localhost:8080/ws");

// Event: Connection opened
socket.addEventListener("open", () => {
    console.log("✅ Connected to WebSocket server");
    socket.send("Hello from the browser!");
});

// Event: Message received from server
socket.addEventListener("message", (event) => {
    console.log("📩 Raw server message:", event.data);  // Log full JSON data

    try {
        let data = JSON.parse(event.data);

        if (data.event === "MOVE") {
            console.log(`🎮 Player ${data.id} moved to (${data.x}, ${data.y})`);
        }
        else if (data.event === "NEW_PLAYER") {
            console.log(`👤 New player joined: ${data.id} at (${data.x}, ${data.y})`);
        }
        else if (data.event === "REMOVE_PLAYER") {
            console.log(`❌ Player left: ${data.id}`);
        }
    } catch (error) {
        console.error("⚠️ JSON Parse Error:", error, event.data);
    }
});


// Event: Connection closed
socket.addEventListener("close", () => {
    console.log("❌ Disconnected from WebSocket server");
});

// Event: Error
socket.addEventListener("error", (error) => {
    console.error("⚠️ WebSocket Error:", error);
});

// Function to send player actions
function sendPlayerAction(action) {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send(action);
    } else {
        console.log("⚠️ WebSocket is not open yet.");
    }
}
