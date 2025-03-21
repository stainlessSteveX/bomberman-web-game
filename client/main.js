const socket = new WebSocket("ws://localhost:8080/ws");

const gameBoard = document.getElementById("gameBoard");

// Create a 10x10 game grid
for (let i = 0; i < 10; i++) {
    for (let j = 0; j < 10; j++) {
        let cell = document.createElement("div");
        cell.classList.add("cell");
        cell.dataset.x = j;
        cell.dataset.y = i;
        gameBoard.appendChild(cell);
    }
}

// Track players on the board
const players = {};

// WebSocket event handlers
socket.addEventListener("open", () => {
    console.log("✅ Connected to WebSocket server");
});

socket.addEventListener("message", (event) => {
    console.log("📩 Raw server message:", event.data);

    try {
        let data = JSON.parse(event.data);

        if (data.event === "NEW_PLAYER" || data.event === "MOVE") {
            updatePlayerPosition(data.id, data.x, data.y);
        }
        else if (data.event === "REMOVE_PLAYER") {
            removePlayer(data.id);
        }
    } catch (error) {
        console.error("⚠️ JSON Parse Error:", error, event.data);
    }
});

// Function to update player position
function updatePlayerPosition(playerId, x, y) {
    // Remove old player position if it exists
    if (players[playerId]) {
        players[playerId].classList.remove("player");
    }

    // Find the correct grid cell
    let cell = document.querySelector(`[data-x="${x}"][data-y="${y}"]`);
    if (cell) {
        cell.classList.add("player");
        cell.textContent = "👾"; // Player icon
        players[playerId] = cell;
    }
}

// Function to remove a player
function removePlayer(playerId) {
    if (players[playerId]) {
        players[playerId].classList.remove("player");
        players[playerId].textContent = "";
        delete players[playerId];
    }
}

// Send player action
function sendPlayerAction(action) {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send(action);
    } else {
        console.log("⚠️ WebSocket is not open yet.");
    }
}
