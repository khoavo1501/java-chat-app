(function () {
    "use strict";

    const config = window.chatConfig || {};
    const currentUser = config.currentUser || "";
    const wsEndpoint = config.wsEndpoint || "/ws";
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");

    const userListEl = document.getElementById("userList");
    const activePeerEl = document.getElementById("activePeer");
    const peerStatusEl = document.getElementById("peerStatus");
    const messagesEl = document.getElementById("messages");
    const messageFormEl = document.getElementById("messageForm");
    const messageInputEl = document.getElementById("messageInput");

    let stompClient = null;
    let users = [];
    let activePeer = null;

    function init() {
        connectSocket();
        loadUsers();
        renderEmptyState("Chon mot nguoi dung de xem lich su chat.");

        messageFormEl.addEventListener("submit", function (event) {
            event.preventDefault();
            sendMessage();
        });
    }

    function connectSocket() {
        const socket = new SockJS(wsEndpoint);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function () {
            stompClient.subscribe("/topic/presence", function (payload) {
                users = JSON.parse(payload.body || "[]");
                renderUsers();
                refreshActivePeerStatus();
            });

            stompClient.subscribe("/user/queue/messages", function (payload) {
                const message = JSON.parse(payload.body);
                handleIncomingMessage(message);
            });

            stompClient.subscribe("/user/queue/errors", function (payload) {
                alert(payload.body || "Gui tin nhan that bai.");
            });

            loadUsers();
        });

        window.addEventListener("beforeunload", function () {
            if (stompClient && stompClient.connected) {
                stompClient.disconnect();
            }
        });
    }

    function loadUsers() {
        fetch(contextPath + "/api/users")
                .then(handleJson)
                .then(function (data) {
                    users = data || [];
                    renderUsers();
                    refreshActivePeerStatus();
                })
                .catch(function () {
                    renderEmptyUserList("Khong the tai danh sach user.");
                });
    }

    function loadConversation(username) {
        fetch(contextPath + "/api/messages/" + encodeURIComponent(username))
                .then(handleJson)
                .then(function (messages) {
                    renderConversation(messages || []);
                })
                .catch(function () {
                    renderEmptyState("Khong the tai lich su chat.");
                });
    }

    function renderUsers() {
        const peers = users.filter(function (user) {
            return user.username !== currentUser;
        });

        if (peers.length === 0) {
            renderEmptyUserList("Chua co tai khoan nao khac.");
            return;
        }

        userListEl.innerHTML = "";

        peers.forEach(function (user) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "user-item" + (activePeer === user.username ? " active" : "");
            button.innerHTML = ""
                    + "<span>" + escapeHtml(user.username) + "</span>"
                    + "<span class='user-state " + (user.online ? "online" : "offline") + "'>"
                    + (user.online ? "Online" : "Offline")
                    + "</span>";

            button.addEventListener("click", function () {
                activePeer = user.username;
                activePeerEl.textContent = user.username;
                refreshActivePeerStatus();
                renderUsers();
                loadConversation(user.username);
                messageInputEl.focus();
            });

            const item = document.createElement("li");
            item.appendChild(button);
            userListEl.appendChild(item);
        });
    }

    function renderEmptyUserList(text) {
        userListEl.innerHTML = "<li class='empty-state'>" + escapeHtml(text) + "</li>";
    }

    function refreshActivePeerStatus() {
        if (!activePeer) {
            peerStatusEl.textContent = "Offline";
            peerStatusEl.className = "badge";
            return;
        }

        const user = users.find(function (it) {
            return it.username === activePeer;
        });

        if (user && user.online) {
            peerStatusEl.textContent = "Online";
            peerStatusEl.className = "badge online";
        } else {
            peerStatusEl.textContent = "Offline";
            peerStatusEl.className = "badge";
        }
    }

    function sendMessage() {
        const content = messageInputEl.value.trim();
        if (!activePeer) {
            alert("Ban can chon nguoi dung de chat.");
            return;
        }

        if (!content) {
            return;
        }

        if (!stompClient || !stompClient.connected) {
            alert("WebSocket chua ket noi.");
            return;
        }

        stompClient.send("/app/chat.private", {}, JSON.stringify({
            recipient: activePeer,
            content: content
        }));

        messageInputEl.value = "";
    }

    function handleIncomingMessage(message) {
        const peer = message.sender === currentUser ? message.recipient : message.sender;

        if (activePeer && peer === activePeer) {
            appendMessage(message);
        }
    }

    function renderConversation(messages) {
        messagesEl.innerHTML = "";

        if (!messages.length) {
            renderEmptyState("Chua co tin nhan. Hay gui tin nhan dau tien.");
            return;
        }

        messages.forEach(appendMessage);
    }

    function renderEmptyState(text) {
        messagesEl.innerHTML = "<p class='empty-state'>" + escapeHtml(text) + "</p>";
    }

    function appendMessage(message) {
        if (messagesEl.querySelector(".empty-state")) {
            messagesEl.innerHTML = "";
        }

        const mine = message.sender === currentUser;
        const wrapper = document.createElement("article");
        wrapper.className = "bubble" + (mine ? " mine" : "");

        const time = formatTime(message.sentAt);
        wrapper.innerHTML = ""
                + "<div class='bubble-header'>"
                + escapeHtml(message.sender)
                + " • "
                + escapeHtml(time)
                + "</div>"
                + "<div class='bubble-text'>"
                + escapeHtml(message.content)
                + "</div>";

        messagesEl.appendChild(wrapper);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function handleJson(response) {
        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }
        return response.json();
    }

    function formatTime(iso) {
        if (!iso) {
            return "";
        }

        const date = new Date(iso);
        if (Number.isNaN(date.getTime())) {
            return iso;
        }

        return date.toLocaleTimeString("vi-VN", {
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function escapeHtml(value) {
        return String(value || "")
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/\"/g, "&quot;")
                .replace(/'/g, "&#39;");
    }

    init();
})();
