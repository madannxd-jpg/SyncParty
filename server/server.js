const { WebSocketServer } = require('ws');

const PORT = process.env.PORT || 8080;
const wss = new WebSocketServer({ port: PORT });

// Room storage: roomId -> Set of client WebSockets
const rooms = new Map();

wss.on('connection', (ws) => {
  let currentRoomId = null;
  let currentUserId = null;

  console.log('Client connected to SyncParty Signaling Server');

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      const { type, roomId, senderId } = msg;

      if (!roomId) return;

      currentRoomId = roomId;
      currentUserId = senderId;

      if (!rooms.has(roomId)) {
        rooms.set(roomId, new Set());
      }
      rooms.get(roomId).add(ws);

      // Broadcast message to all other participants in the room
      const clients = rooms.get(roomId);
      clients.forEach((client) => {
        if (client !== ws && client.readyState === 1) { // 1 = OPEN
          client.send(JSON.stringify(msg));
        }
      });
    } catch (err) {
      console.error('Error processing message:', err);
    }
  });

  ws.on('close', () => {
    if (currentRoomId && rooms.has(currentRoomId)) {
      rooms.get(currentRoomId).delete(ws);
      if (rooms.get(currentRoomId).size === 0) {
        rooms.delete(currentRoomId);
      }
    }
    console.log(`Client ${currentUserId} disconnected from room ${currentRoomId}`);
  });
});

console.log(`🚀 SyncParty Signaling Server is listening on ws://0.0.0.0:${PORT}`);
