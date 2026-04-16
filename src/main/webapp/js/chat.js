/**
 * Java WebSocket Chat Client
 * Xử lý kết nối WebSocket và giao tiếp real-time với server
 */

(function() {
    'use strict';

    // ============= CONFIGURATION =============
    const CONFIG = {
        // Lấy config từ JSP (được inject sẵn)
        username: window.ChatConfig.username,
        room: window.ChatConfig.room || 'general',
        contextPath: window.ChatConfig.contextPath || '',

        // Cấu hình WebSocket
        wsProtocol: window.location.protocol === 'https:' ? 'wss:' : 'ws:',
        wsReconnectInterval: 3000,  // 3 giây
        wsMaxReconnectAttempts: 10,

        // UI Config
        scrollThreshold: 100,  // Pixel threshold để auto-scroll
        maxMessages: 500,      // Số tin nhắn tối đa hiển thị
    };

    // Tính WebSocket URL
    CONFIG.wsUrl = `${CONFIG.wsProtocol}//${window.location.host}${CONFIG.contextPath}/chat/${CONFIG.room}?username=${encodeURIComponent(CONFIG.username)}`;

    // ============= STATE =============
    const state = {
        socket: null,
        connected: false,
        reconnectAttempts: 0,
        users: [],
        typingUsers: new Set(),
    };

    // ============= DOM ELEMENTS =============
    const elements = {
        messagesContainer: document.getElementById('messagesContainer'),
        messageInput: document.getElementById('messageInput'),
        messageForm: document.getElementById('messageForm'),
        userList: document.getElementById('userList'),
        onlineCount: document.getElementById('onlineCount'),
        connectionStatus: document.getElementById('connectionStatus'),
        roomName: document.getElementById('roomName'),
    };

    // ============= WEBSOCKET HANDLER =============

    /**
     * Khởi tạo và kết nối WebSocket
     */
    function initWebSocket() {
        try {
            updateConnectionStatus('connecting');

            // Tạo WebSocket connection
            state.socket = new WebSocket(CONFIG.wsUrl);

            // Event: Connection opened
            state.socket.onopen = handleOpen;

            // Event: Message received
            state.socket.onmessage = handleMessage;

            // Event: Connection closed
            state.socket.onclose = handleClose;

            // Event: Error occurred
            state.socket.onerror = handleError;

        } catch (error) {
            console.error('[ChatClient] WebSocket initialization error:', error);
            scheduleReconnect();
        }
    }

    /**
     * Xử lý khi WebSocket opened
     */
    function handleOpen(event) {
        console.log('[ChatClient] WebSocket connected');
        state.connected = true;
        state.reconnectAttempts = 0;
        updateConnectionStatus('connected');
        showToast('Đã kết nối server!');
    }

    /**
     * Xử lý khi nhận được tin nhắn từ server
     */
    function handleMessage(event) {
        try {
            const message = JSON.parse(event.data);
            processMessage(message);
        } catch (error) {
            console.error('[ChatClient] Error parsing message:', error);
        }
    }

    /**
     * Xử lý khi WebSocket closed
     */
    function handleClose(event) {
        console.log('[ChatClient] WebSocket closed:', event.code, event.reason);
        state.connected = false;
        updateConnectionStatus('disconnected');

        // Thử reconnect nếu không phải do user đóng
        if (event.code !== 1000) {
            scheduleReconnect();
        }
    }

    /**
     * Xử lý WebSocket error
     */
    function handleError(error) {
        console.error('[ChatClient] WebSocket error:', error);
        updateConnectionStatus('disconnected');
    }

    /**
     * Lên lịch reconnect
     */
    function scheduleReconnect() {
        if (state.reconnectAttempts >= CONFIG.wsMaxReconnectAttempts) {
            console.log('[ChatClient] Max reconnect attempts reached');
            showToast('Không thể kết nối. Vui lòng refresh trang.');
            return;
        }

        state.reconnectAttempts++;
        console.log(`[ChatClient] Scheduling reconnect attempt ${state.reconnectAttempts}`);

        setTimeout(() => {
            if (!state.connected) {
                initWebSocket();
            }
        }, CONFIG.wsReconnectInterval);
    }

    /**
     * Gửi tin nhắn qua WebSocket
     */
    function sendMessage(content) {
        if (!state.connected || !state.socket) {
            showToast('Chưa kết nối server!');
            return false;
        }

        const message = {
            type: 'message',
            sender: CONFIG.username,
            content: content
        };

        state.socket.send(JSON.stringify(message));
        return true;
    }

    // ============= MESSAGE PROCESSING =============

    /**
     * Xử lý tin nhắn từ server theo type
     */
    function processMessage(message) {
        switch (message.type) {
            case 'message':
                displayChatMessage(message);
                break;
            case 'join':
                displaySystemMessage(`${message.sender} đã tham gia phòng chat`);
                break;
            case 'leave':
                displaySystemMessage(`${message.sender} đã rời phòng chat`);
                break;
            case 'users':
                updateUserList(message.content);
                break;
            case 'error':
                showToast(`Lỗi: ${message.content}`);
                break;
            default:
                console.log('[ChatClient] Unknown message type:', message.type);
        }
    }

    /**
     * Hiển thị tin nhắn chat
     */
    function displayChatMessage(message) {
        // Kiểm tra xem có phải tin nhắn của mình không
        const isMyMessage = message.sender === CONFIG.username;

        // Kiểm tra có phải tin nhắn hệ thống không
        const isSystemMessage = message.sender === 'System';

        // Tạo element cho tin nhắn
        const messageEl = document.createElement('div');
        messageEl.className = 'message';

        if (isMyMessage) {
            messageEl.classList.add('my-message');
        } else if (isSystemMessage) {
            messageEl.classList.add('system-message');
        } else {
            messageEl.classList.add('other-message');
        }

        // Avatar color dựa trên username
        const avatarColor = generateAvatarColor(message.sender);

        messageEl.innerHTML = `
            <div class="message-content">
                <div class="message-header">
                    ${!isMyMessage && !isSystemMessage ? `
                        <div class="message-avatar" style="background: ${avatarColor}">
                            ${message.sender.charAt(0).toUpperCase()}
                        </div>
                    ` : ''}
                    <span class="message-sender">${escapeHtml(message.sender)}</span>
                </div>
                <div class="message-text">${escapeHtml(message.content)}</div>
                <div class="message-footer">
                    <span class="message-time">${message.timestamp || getCurrentTime()}</span>
                </div>
            </div>
        `;

        // Thêm vào container
        elements.messagesContainer.appendChild(messageEl);

        // Auto-scroll nếu user đang ở cuối
        autoScroll();

        // Cleanup nếu quá nhiều tin nhắn
        cleanupMessages();
    }

    /**
     * Hiển thị tin nhắn hệ thống
     */
    function displaySystemMessage(text) {
        const messageEl = document.createElement('div');
        messageEl.className = 'message system-message';

        messageEl.innerHTML = `
            <div class="message-content">
                <div class="message-text">${escapeHtml(text)}</div>
            </div>
        `;

        elements.messagesContainer.appendChild(messageEl);
        autoScroll();
    }

    /**
     * Cập nhật danh sách user online
     */
    function updateUserList(userString) {
        state.users = userString ? userString.split(',').filter(u => u.trim()) : [];

        // Clear current list
        elements.userList.innerHTML = '';

        // Thêm từng user
        state.users.forEach(username => {
            const isMe = username === CONFIG.username;
            const userItem = document.createElement('li');
            userItem.className = 'user-item' + (isMe ? ' you' : '');

            const avatarColor = generateAvatarColor(username);

            userItem.innerHTML = `
                <div class="user-item-avatar" style="background: ${avatarColor}">
                    ${username.charAt(0).toUpperCase()}
                </div>
                <span class="user-item-name">${escapeHtml(username)}</span>
            `;

            elements.userList.appendChild(userItem);
        });

        // Cập nhật số người online
        elements.onlineCount.textContent = state.users.length;
    }

    /**
     * Auto-scroll đến tin nhắn mới nhất
     */
    function autoScroll() {
        const container = elements.messagesContainer;
        const scrollBottom = container.scrollHeight - container.scrollTop - container.clientHeight;

        if (scrollBottom < CONFIG.scrollThreshold) {
            container.scrollTop = container.scrollHeight;
        }
    }

    /**
     * Cleanup tin nhắn cũ nếu quá nhiều
     */
    function cleanupMessages() {
        const messages = elements.messagesContainer.querySelectorAll('.message:not(.system-message)');
        if (messages.length > CONFIG.maxMessages) {
            const toRemove = messages.length - CONFIG.maxMessages;
            for (let i = 0; i < toRemove; i++) {
                messages[i].remove();
            }
        }
    }

    // ============= UI HELPERS =============

    /**
     * Cập nhật trạng thái kết nối
     */
    function updateConnectionStatus(status) {
        const statusEl = elements.connectionStatus;
        const textEl = statusEl.querySelector('.status-text');

        statusEl.className = 'connection-status ' + status;

        switch (status) {
            case 'connected':
                textEl.textContent = 'Đã kết nối';
                break;
            case 'disconnected':
                textEl.textContent = 'Mất kết nối';
                break;
            case 'connecting':
                textEl.textContent = 'Đang kết nối...';
                break;
        }
    }

    /**
     * Hiển thị toast notification
     */
    function showToast(message, duration = 3000) {
        // Remove existing toast
        const existingToast = document.querySelector('.toast');
        if (existingToast) {
            existingToast.remove();
        }

        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.textContent = message;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.remove();
        }, duration);
    }

    /**
     * Lấy thời gian hiện tại format HH:mm:ss
     */
    function getCurrentTime() {
        const now = new Date();
        return now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }

    // ============= UTILITIES =============

    /**
     * Escape HTML để prevent XSS
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Tạo màu avatar ngẫu nhiên từ username
     */
    function generateAvatarColor(username) {
        // Một số màu đẹp cho avatar
        const colors = [
            'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',  // Purple
            'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',  // Pink
            'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',  // Blue
            'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',  // Green
            'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',  // Orange
            'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',  // Pastel
            'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',  // Rose
            'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',  // Sky
        ];

        // Hash username để lấy index cố định
        let hash = 0;
        for (let i = 0; i < username.length; i++) {
            hash = username.charCodeAt(i) + ((hash << 5) - hash);
        }

        return colors[Math.abs(hash) % colors.length];
    }

    // ============= EVENT LISTENERS =============

    /**
     * Xử lý submit form tin nhắn
     */
    elements.messageForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const content = elements.messageInput.value.trim();

        if (content && sendMessage(content)) {
            elements.messageInput.value = '';
            elements.messageInput.focus();
        }
    });

    /**
     * Xử lý phím tắt (Enter để gửi)
     */
    elements.messageInput.addEventListener('keydown', function(e) {
        // Shift + Enter để xuống dòng
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            elements.messageForm.dispatchEvent(new Event('submit'));
        }
    });

    /**
     * Handle page unload - đóng WebSocket
     */
    window.addEventListener('beforeunload', function() {
        if (state.socket) {
            state.socket.close(1000, 'User leaving');
        }
    });

    // ============= INITIALIZATION =============

    /**
     * Khởi tạo ứng dụng
     */
    function init() {
        console.log('[ChatClient] Initializing chat client...');
        console.log('[ChatClient] Username:', CONFIG.username);
        console.log('[ChatClient] Room:', CONFIG.room);
        console.log('[ChatClient] WebSocket URL:', CONFIG.wsUrl);

        // Update room name display
        if (elements.roomName) {
            elements.roomName.textContent = CONFIG.room;
        }

        // Focus vào input
        if (elements.messageInput) {
            elements.messageInput.focus();
        }

        // Kết nối WebSocket
        initWebSocket();
    }

    // Start khi DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
