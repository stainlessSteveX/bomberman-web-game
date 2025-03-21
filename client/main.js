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

document.addEventListener("keydown", (event) => {
    if (event.key === "b" || event.key === "B") {
        sendPlayerAction("DROP_BOMB");
    }
});


function updatePlayerPosition(playerId, x, y) {
    if (!players[playerId]) {
        players[playerId] = { x, y };
    }

    const oldX = players[playerId].x;
    const oldY = players[playerId].y;
    players[playerId].x = x;
    players[playerId].y = y;

    animatePlayerMovement(playerId, oldX, oldY, x, y);
}

function animatePlayerMovement(playerId, oldX, oldY, newX, newY) {
    let startTime = performance.now();
    let duration = 100; // 100ms smooth transition

    function animate() {
        let now = performance.now();
        let progress = Math.min(1, (now - startTime) / duration);

        let interpolatedX = oldX + (newX - oldX) * progress;
        let interpolatedY = oldY + (newY - oldY) * progress;

        let cell = document.querySelector(`[data-x="${Math.round(interpolatedX)}"][data-y="${Math.round(interpolatedY)}"]`);
        if (cell) {
            cell.classList.add("player");
            cell.textContent = "👾";
        }

        if (progress < 1) {
            requestAnimationFrame(animate);
        }
    }

    requestAnimationFrame(animate);
}


function removePlayer(playerId) {
    if (players[playerId]) { // ✅ Check if player exists before removing
        if (players[playerId].classList) { // ✅ Extra check to avoid undefined errors
            players[playerId].classList.remove("player");
            players[playerId].textContent = "";
        }
        delete players[playerId]; // ✅ Remove player from tracking
    } else {
        console.warn(`⚠️ Tried to remove player ${playerId}, but they were already removed.`);
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

socket.addEventListener("message", (event) => {
    let data = JSON.parse(event.data);

    if (data.event === "BOMB_PLACED") {
        placeBomb(data.x, data.y);
    }
    else if (data.event === "EXPLOSION") {
        triggerExplosion(data.tiles);
    }
});

function placeBomb(x, y) {
    let cell = document.querySelector(`[data-x="${x}"][data-y="${y}"]`);
    if (cell) {
        cell.textContent = "💣";
    }
}

function triggerExplosion(tiles) {
    tiles.forEach(tile => {
        let cell = document.querySelector(`[data-x="${tile.x}"][data-y="${tile.y}"]`);
        if (cell) {
            cell.textContent = "🔥";
            setTimeout(() => { cell.textContent = ""; }, 500); // Remove explosion effect after 500ms
        }
    });
}
