(function () {
    "use strict";

    const config = window.profileConfig || {};
    const currentUser = config.currentUser || "";
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");
    const csrfHeader = config.csrfHeader || "X-CSRF-TOKEN";
    const csrfToken = config.csrfToken || "";

    const refs = {
        profileInfo: document.getElementById("profileInfo"),
        themeSelector: document.getElementById("profileThemeSelector"),
        friendRequests: document.getElementById("profileFriendRequests"),
        friendList: document.getElementById("profileFriendList"),
        userSearch: document.getElementById("profileUserSearch"),
        userList: document.getElementById("profileUserList")
    };

    const state = {
        profile: null,
        users: [],
        searchTerm: ""
    };

    function init() {
        bindEvents();
        refreshData();
    }

    function bindEvents() {
        if (refs.themeSelector) {
            refs.themeSelector.addEventListener("change", function (event) {
                applyTheme(event.target.value || "aurora", true);
            });
        }

        if (refs.userSearch) {
            refs.userSearch.addEventListener("input", function (event) {
                state.searchTerm = event.target.value || "";
                renderUsers();
            });
        }
    }

    function refreshData() {
        Promise.all([loadProfile(), loadUsers()])
            .then(function () {
                renderAll();
                applyTheme((state.profile && state.profile.theme) || config.currentTheme || "aurora", false);
            })
            .catch(handleApiError);
    }

    function loadProfile() {
        return apiGet("/api/users/profile").then(function (profile) {
            state.profile = profile || null;
            return profile;
        });
    }

    function loadUsers() {
        return apiGet("/api/users").then(function (users) {
            state.users = users || [];
            return users;
        });
    }

    function renderAll() {
        renderProfileInfo();
        renderFriendRequests();
        renderFriendList();
        renderUsers();
    }

    function renderProfileInfo() {
        if (!refs.profileInfo || !state.profile) {
            return;
        }

        refs.profileInfo.innerHTML = "";

        refs.profileInfo.appendChild(createInfoLine("Tên đăng nhập", state.profile.username || "-"));
        refs.profileInfo.appendChild(createInfoLine("Giao diện", state.profile.theme || "aurora"));
        refs.profileInfo.appendChild(createInfoLine("Vai trò", (state.profile.roles || []).join(", ") || "USER"));
        refs.profileInfo.appendChild(createInfoLine("Bạn bè", String((state.profile.friends || []).length)));

        if (refs.themeSelector) {
            refs.themeSelector.value = state.profile.theme || "aurora";
        }
    }

    function renderFriendRequests() {
        if (!refs.friendRequests) {
            return;
        }

        const requests = (state.profile && state.profile.incomingFriendRequests) || [];
        refs.friendRequests.innerHTML = "";

        if (!requests.length) {
            refs.friendRequests.appendChild(createEmptyItem("Không có yêu cầu kết bạn."));
            return;
        }

        requests.forEach(function (username) {
            const row = createListRow(username, "Yêu cầu");
            row.appendChild(createActionGroup([
                createButton("Duyệt", function () {
                    acceptFriendRequest(username);
                }),
                createButton("Từ chối", function () {
                    rejectFriendRequest(username);
                })
            ]));
            refs.friendRequests.appendChild(row);
        });
    }

    function renderFriendList() {
        if (!refs.friendList) {
            return;
        }

        const friends = (state.profile && state.profile.friends) || [];
        refs.friendList.innerHTML = "";

        if (!friends.length) {
            refs.friendList.appendChild(createEmptyItem("Chưa có bạn bè nào."));
            return;
        }

        friends.forEach(function (username) {
            const row = createListRow(username, "Bạn bè");
            row.appendChild(createActionGroup([
                createButton("Nhắn tin", function () {
                    window.location.href = contextPath + "/chat?user=" + encodeURIComponent(username);
                }),
                createButton("Xóa", function () {
                    removeFriend(username);
                })
            ]));
            refs.friendList.appendChild(row);
        });
    }

    function renderUsers() {
        if (!refs.userList) {
            return;
        }

        const friends = new Set((state.profile && state.profile.friends) || []);
        const term = (state.searchTerm || "").trim().toLowerCase();

        const users = state.users.filter(function (user) {
            if (!user || user.username === currentUser) {
                return false;
            }

            if (term && user.username.toLowerCase().indexOf(term) === -1) {
                return false;
            }

            return true;
        });

        refs.userList.innerHTML = "";

        if (!users.length) {
            refs.userList.appendChild(createEmptyItem("Không có người dùng phù hợp."));
            return;
        }

        users.forEach(function (user) {
            const row = createListRow(user.username, user.online ? "Đang hoạt động" : "Ngoại tuyến");
            const actions = [
                createButton("Nhắn tin", function () {
                    window.location.href = contextPath + "/chat?user=" + encodeURIComponent(user.username);
                })
            ];

            if (!friends.has(user.username)) {
                actions.push(createButton("Kết bạn", function () {
                    sendFriendRequest(user.username);
                }));
            }

            row.appendChild(createActionGroup(actions));
            refs.userList.appendChild(row);
        });
    }

    function applyTheme(theme, persist) {
        const normalized = theme || "aurora";
        document.body.setAttribute("data-theme", normalized);

        if (refs.themeSelector) {
            refs.themeSelector.value = normalized;
        }

        if (persist) {
            apiPatch("/api/users/theme", { theme: normalized })
                .then(function (profile) {
                    state.profile = profile;
                    renderProfileInfo();
                })
                .catch(handleApiError);
        }
    }

    function sendFriendRequest(username) {
        apiPost("/api/users/friends/request", { username: username })
            .then(refreshData)
            .catch(handleApiError);
    }

    function acceptFriendRequest(username) {
        apiPost("/api/users/friends/accept", { username: username })
            .then(refreshData)
            .catch(handleApiError);
    }

    function rejectFriendRequest(username) {
        apiPost("/api/users/friends/reject", { username: username })
            .then(refreshData)
            .catch(handleApiError);
    }

    function removeFriend(username) {
        apiDelete("/api/users/friends/" + encodeURIComponent(username))
            .then(refreshData)
            .catch(handleApiError);
    }

    function apiGet(path) {
        return fetch(contextPath + path, {
            credentials: "same-origin"
        }).then(handleResponse);
    }

    function apiPost(path, body) {
        return fetch(contextPath + path, {
            method: "POST",
            credentials: "same-origin",
            headers: jsonHeaders(),
            body: JSON.stringify(body || {})
        }).then(handleResponse);
    }

    function apiPatch(path, body) {
        return fetch(contextPath + path, {
            method: "PATCH",
            credentials: "same-origin",
            headers: jsonHeaders(),
            body: JSON.stringify(body || {})
        }).then(handleResponse);
    }

    function apiDelete(path) {
        return fetch(contextPath + path, {
            method: "DELETE",
            credentials: "same-origin",
            headers: csrfHeaders()
        }).then(handleResponse);
    }

    function jsonHeaders() {
        const headers = csrfHeaders();
        headers["Content-Type"] = "application/json";
        return headers;
    }

    function csrfHeaders() {
        const headers = {};
        if (csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
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

    function handleApiError(error) {
        alert((error && error.message) || "Đã xảy ra lỗi.");
    }

    function createInfoLine(label, value) {
        const row = document.createElement("div");
        row.className = "profile-line";
        row.innerHTML = "<strong>" + escapeHtml(label) + ":</strong> " + escapeHtml(value);
        return row;
    }

    function createEmptyItem(text) {
        const item = document.createElement("li");
        item.className = "empty-item";
        item.textContent = text;
        return item;
    }

    function createListRow(title, subtitle) {
        const item = document.createElement("li");
        item.className = "contact-card small";

        const header = document.createElement("div");
        header.className = "contact-card-header";

        const titleNode = document.createElement("span");
        titleNode.className = "contact-title static";
        titleNode.textContent = title;

        const subtitleNode = document.createElement("span");
        subtitleNode.className = "status-chip muted-chip";
        subtitleNode.textContent = subtitle;

        header.appendChild(titleNode);
        header.appendChild(subtitleNode);
        item.appendChild(header);
        return item;
    }

    function createButton(label, handler) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "ghost-mini";
        button.textContent = label;
        button.addEventListener("click", handler);
        return button;
    }

    function createActionGroup(buttons) {
        const group = document.createElement("div");
        group.className = "card-actions";
        buttons.forEach(function (button) {
            group.appendChild(button);
        });
        return group;
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
