# WebSocket Chat API Documentation

## Overview
Chat real-time giữa Buyer và Seller sử dụng STOMP protocol over WebSocket.

---

## Connection Setup

### 1. Connect to WebSocket
```javascript
const socket = new SockJS('http://localhost:8080/ws-chat');
const stompClient = Stomp.over(socket);

stompClient.connect({
    'Authorization': 'Bearer ' + jwtToken // JWT token từ login
}, onConnected, onError);
```

### 2. Subscribe to Topics
```javascript
function onConnected() {
    // Subscribe to chatroom messages
    stompClient.subscribe('/topic/chatroom/' + chatroomId, onMessageReceived);
    
    // Subscribe to private messages (optional)
    stompClient.subscribe('/user/queue/messages', onPrivateMessage);
}
```

---

## REST API Endpoints

### 1. Create or Get Chatroom
**POST** `/api/chat/chatrooms`

**Request Body:**
```json
{
    "sellerId": 123,
    "orderId": 456
}
```

**Response:**
```json
{
    "status": "success",
    "chatroom": {
        "chatroomId": 789,
        "buyerId": 100,
        "buyerName": "John Doe",
        "sellerId": 123,
        "sellerName": "Jane Smith",
        "orderId": 456,
        "createdAt": "2025-01-15T10:30:00"
    }
}
```

---

### 2. Get User's Chatrooms
**GET** `/api/chat/chatrooms`

**Response:**
```json
{
    "status": "success",
    "chatrooms": [
        {
            "chatroomId": 789,
            "otherUserId": 123,
            "otherUserName": "Jane Smith",
            "orderId": 456,
            "unreadCount": 3,
            "createdAt": "2025-01-15T10:30:00"
        }
    ]
}
```

---

### 3. Get Chatroom Messages (History)
**GET** `/api/chat/chatrooms/{chatroomId}/messages`

**Response:**
```json
{
    "status": "success",
    "messages": [
        {
            "messageId": 1,
            "senderId": 100,
            "senderName": "John Doe",
            "content": "Hello, is this item still available?",
            "messageType": "TEXT",
            "attachUrl": "",
            "timestamp": "2025-01-15T10:31:00",
            "isRead": true
        }
    ]
}
```

---

## WebSocket Messages

### 1. Send Message
**Destination:** `/app/chat.sendMessage`

**Message Payload:**
```json
{
    "chatroomId": 789,
    "senderId": 100,
    "senderName": "John Doe",
    "content": "Hello!",
    "messageType": "TEXT"
}
```

**Received at:** `/topic/chatroom/{chatroomId}`

```javascript
function sendMessage() {
    const messagePayload = {
        chatroomId: chatroomId,
        senderId: userId,
        senderName: userName,
        content: messageInput.value,
        messageType: 'TEXT'
    };
    
    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(messagePayload));
}
```

---

### 2. Join Chatroom
**Destination:** `/app/chat.joinRoom`

**Message Payload:**
```json
{
    "chatroomId": 789,
    "userId": 100
}
```

```javascript
function joinChatroom(chatroomId, userId) {
    stompClient.send('/app/chat.joinRoom', {}, JSON.stringify({
        chatroomId: chatroomId,
        userId: userId
    }));
}
```

---

### 3. Typing Indicator
**Destination:** `/app/chat.typing`

**Message Payload:**
```json
{
    "chatroomId": 789,
    "userId": 100,
    "isTyping": true
}
```

```javascript
function handleTyping(isTyping) {
    stompClient.send('/app/chat.typing', {}, JSON.stringify({
        chatroomId: chatroomId,
        userId: userId,
        isTyping: isTyping
    }));
}
```

---

## Message Types

- **TEXT**: Text message
- **IMAGE**: Image attachment
- **FILE**: File attachment

---

## Complete Frontend Example (React)

```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { useState, useEffect } from 'react';

function ChatComponent({ chatroomId, userId, userName, jwtToken }) {
    const [messages, setMessages] = useState([]);
    const [stompClient, setStompClient] = useState(null);
    const [messageInput, setMessageInput] = useState('');

    useEffect(() => {
        // Connect WebSocket
        const socket = new SockJS('http://localhost:8080/ws-chat');
        const client = Stomp.over(socket);

        client.connect(
            { 'Authorization': 'Bearer ' + jwtToken },
            () => {
                console.log('Connected to WebSocket');
                
                // Subscribe to chatroom
                client.subscribe(`/topic/chatroom/${chatroomId}`, (message) => {
                    const receivedMessage = JSON.parse(message.body);
                    setMessages(prev => [...prev, receivedMessage]);
                });

                // Join chatroom
                client.send('/app/chat.joinRoom', {}, JSON.stringify({
                    chatroomId: chatroomId,
                    userId: userId
                }));

                setStompClient(client);
            },
            (error) => {
                console.error('WebSocket connection error:', error);
            }
        );

        // Load message history
        fetch(`http://localhost:8080/api/chat/chatrooms/${chatroomId}/messages`, {
            headers: {
                'Authorization': 'Bearer ' + jwtToken
            }
        })
        .then(res => res.json())
        .then(data => setMessages(data.messages));

        // Cleanup on unmount
        return () => {
            if (client && client.connected) {
                client.disconnect();
            }
        };
    }, [chatroomId, userId, jwtToken]);

    const sendMessage = () => {
        if (messageInput.trim() && stompClient) {
            const messagePayload = {
                chatroomId: chatroomId,
                senderId: userId,
                senderName: userName,
                content: messageInput,
                messageType: 'TEXT'
            };

            stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(messagePayload));
            setMessageInput('');
        }
    };

    return (
        <div>
            <div className="messages">
                {messages.map(msg => (
                    <div key={msg.messageId} className={msg.senderId === userId ? 'sent' : 'received'}>
                        <strong>{msg.senderName}:</strong> {msg.content}
                    </div>
                ))}
            </div>
            <input 
                value={messageInput} 
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
            />
            <button onClick={sendMessage}>Send</button>
        </div>
    );
}

export default ChatComponent;
```

---

## Testing with Postman

### 1. Create Chatroom
- **Method**: POST
- **URL**: `http://localhost:8080/api/chat/chatrooms`
- **Headers**: 
  - `Authorization: Bearer {your_jwt_token}`
  - `Content-Type: application/json`
- **Body**:
```json
{
    "sellerId": 2,
    "orderId": null
}
```

### 2. Get Chatrooms
- **Method**: GET
- **URL**: `http://localhost:8080/api/chat/chatrooms`
- **Headers**: `Authorization: Bearer {your_jwt_token}`

### 3. Get Messages
- **Method**: GET
- **URL**: `http://localhost:8080/api/chat/chatrooms/1/messages`
- **Headers**: `Authorization: Bearer {your_jwt_token}`

---

## Notes

1. **Authentication**: Tất cả endpoints (trừ WebSocket connection) đều yêu cầu JWT token
2. **Auto-read**: Khi load messages, hệ thống tự động đánh dấu là đã đọc
3. **Real-time**: Messages được broadcast real-time đến tất cả subscribers
4. **Unread Count**: API trả về số messages chưa đọc cho mỗi chatroom
5. **Typing Indicator**: Có thể implement typing indicator bằng `/app/chat.typing`

---

## Error Handling

```javascript
function onError(error) {
    console.error('WebSocket Error:', error);
    // Retry connection sau 5 giây
    setTimeout(() => {
        connectWebSocket();
    }, 5000);
}
```
