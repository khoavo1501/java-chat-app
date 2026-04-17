(function () {
    "use strict";

    const config = window.adminDashboardConfig || {};
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");

    const refs = {
        stats: document.getElementById("adminStats"),
        onlineUsers: document.getElementById("adminOnlineUsers"),
        recentGroups: document.getElementById("adminRecentGroups")
    };

    const state = {
        stats: null,
        users: [],
        groups: []
    };

    function init() {
        Promise.all([loadStats(), loadUsers(), loadGroups()])
            .then(renderAll)
            .catch(handleError);
    }

    function loadStats() {
        return apiGet("/api/admin/stats").then(function (data) {
            state.stats = data || null;
            return data;
        });
    }

    function loadUsers() {
        return apiGet("/api/admin/users").then(function (data) {
            state.users = data || [];
            return data;
        });
    }

    function loadGroups() {
        return apiGet("/api/admin/groups").then(function (data) {
            state.groups = data || [];
            return data;
        });
    }

    function renderAll() {
        renderStats();
        renderOnlineUsers();
        renderRecentGroups();
    }

    function renderStats() {
        if (!refs.stats) {
            return;
        }

        refs.stats.innerHTML = "";

        if (!state.stats) {
            refs.stats.appendChild(createEmpty("Chưa có dữ liệu thống kê."));
            return;
        }

        const cards = [
            createStatCard("Tổng người dùng", state.stats.totalUsers),
            createStatCard("Đang online", state.stats.onlineUsers),
            createStatCard("Tài khoản admin", state.stats.adminUsers),
            createStatCard("Tổng nhóm", state.stats.totalGroups),
            createStatCard("Tin nhắn hệ thống", state.stats.totalMessages),
            createStatCard("Yêu cầu kết bạn chờ", state.stats.pendingFriendRequests),
            createStatCard("Yêu cầu vào nhóm chờ", state.stats.pendingGroupJoinRequests)
        ];

        cards.forEach(function (card) {
            refs.stats.appendChild(card);
        });
    }

    function renderOnlineUsers() {
        if (!refs.onlineUsers) {
            return;
        }

        const onlineUsers = state.users.filter(function (user) {
            return !!(user && user.online);
        });

        refs.onlineUsers.innerHTML = "";

        if (!onlineUsers.length) {
            refs.onlineUsers.appendChild(createEmpty("Hiện chưa có người dùng online."));
            return;
        }

        onlineUsers.slice(0, 8).forEach(function (user) {
            const row = document.createElement("li");
            row.className = "contact-card small";
            row.textContent = user.username + " • " + (user.theme || "aurora");
            refs.onlineUsers.appendChild(row);
        });
    }

    function renderRecentGroups() {
        if (!refs.recentGroups) {
            return;
        }

        refs.recentGroups.innerHTML = "";

        if (!state.groups.length) {
            refs.recentGroups.appendChild(createEmpty("Chưa có nhóm nào."));
            return;
        }

        state.groups.slice(0, 8).forEach(function (group) {
            const row = document.createElement("li");
            row.className = "contact-card small";
            row.textContent = (group.name || "Nhom khong ten") + " • " + (group.groupCode || "-")
                + " • " + ((group.memberUsernames || []).length) + " thanh vien";
            refs.recentGroups.appendChild(row);
        });
    }

    function createStatCard(label, value) {
        const card = document.createElement("article");
        card.className = "admin-stat-card";

        const title = document.createElement("p");
        title.className = "admin-stat-label";
        title.textContent = label;

        const number = document.createElement("p");
        number.className = "admin-stat-value";
        number.textContent = String(value == null ? 0 : value);

        card.appendChild(title);
        card.appendChild(number);
        return card;
    }

    function createEmpty(text) {
        const element = document.createElement("li");
        element.className = "empty-item";
        element.textContent = text;
        return element;
    }

    function apiGet(path) {
        return fetch(contextPath + path, {
            credentials: "same-origin"
        }).then(handleResponse);
    }

    function handleResponse(response) {
        if (!response.ok) {
            return response.text().then(function (text) {
                throw new Error(text || ("HTTP " + response.status));
            });
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        if (contentType.indexOf("application/json") !== -1) {
            return response.json();
        }

        return response.text();
    }

    function handleError(error) {
        alert((error && error.message) || "Khong the tai dashboard quan tri.");
    }

    init();
})();
