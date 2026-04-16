<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chat Room - ${username}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="chat-app">
        <%-- Sidebar hiển thị thông tin và users online --%>
        <aside class="sidebar">
            <div class="sidebar-header">
                <div class="user-info">
                    <div class="user-avatar">
                        ${username.substring(0, 1).toUpperCase()}
                    </div>
                    <div class="user-details">
                        <span class="user-name">${username}</span>
                        <span class="user-status">
                            <span class="status-dot online"></span>
                            Online
                        </span>
                    </div>
                </div>
                <a href="${pageContext.request.contextPath}/logout" class="btn-logout" title="Đăng xuất">
                    <svg viewBox="0 0 24 24" width="20" height="20">
                        <path fill="currentColor" d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>
                    </svg>
                </a>
            </div>

            <div class="sidebar-section">
                <h3 class="section-title">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
                    </svg>
                    Người dùng Online
                </h3>
                <ul id="userList" class="user-list">
                    <li class="user-item loading">Đang tải...</li>
                </ul>
            </div>

            <div class="sidebar-footer">
                <div class="connection-status" id="connectionStatus">
                    <span class="status-indicator"></span>
                    <span class="status-text">Đang kết nối...</span>
                </div>
            </div>
        </aside>

        <%-- Main chat area --%>
        <main class="chat-main">
            <%-- Chat header --%>
            <header class="chat-header">
                <div class="room-info">
                    <h1>
                        <svg viewBox="0 0 24 24" width="24" height="24">
                            <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
                        </svg>
                        Phòng chat: <span id="roomName">${room}</span>
                    </h1>
                </div>
                <div class="chat-actions">
                    <span class="online-count">
                        <span id="onlineCount">0</span> người online
                    </span>
                </div>
            </header>

            <%-- Messages container --%>
            <div class="messages-container" id="messagesContainer">
                <div class="welcome-message">
                    <p>Chào mừng bạn đến với phòng chat!</p>
                    <p>Bắt đầu nhắn tin để trò chuyện với mọi người.</p>
                </div>
            </div>

            <%-- Message input area --%>
            <footer class="chat-input-area">
                <form id="messageForm" class="message-form">
                    <input type="text"
                           id="messageInput"
                           placeholder="Nhập tin nhắn..."
                           autocomplete="off"
                           maxlength="500">
                    <button type="submit" class="btn-send">
                        <svg viewBox="0 0 24 24" width="24" height="24">
                            <path fill="currentColor" d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                        </svg>
                    </button>
                </form>
            </footer>
        </main>
    </div>

    <%-- Data attributes cho JavaScript --%>
    <script>
        window.ChatConfig = {
            username: '${username}',
            room: '${room}',
            contextPath: '${pageContext.request.contextPath}'
        };
    </script>
    <script src="${pageContext.request.contextPath}/js/chat.js"></script>
</body>
</html>
