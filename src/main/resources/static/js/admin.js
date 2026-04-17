(function () {
    "use strict";

    const config = window.adminConfig || {};
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");
    const csrfHeader = config.csrfHeader || "X-CSRF-TOKEN";
    const csrfToken = config.csrfToken || "";

    const refs = {
        search: document.getElementById("adminSearch"),
        users: document.getElementById("adminUsers")
    };

    const state = {
        users: [],
        searchTerm: ""
    };

    function init() {
        bindEvents();
        loadUsers();
    }

    function bindEvents() {
        if (refs.search) {
            refs.search.addEventListener("input", function (event) {
                state.searchTerm = event.target.value || "";
                renderUsers();
            });
        }
    }

    function loadUsers() {
        apiGet("/api/admin/users")
            .then(function (users) {
                state.users = users || [];
                renderUsers();
            })
            .catch(handleError);
    }

    function renderUsers() {
        if (!refs.users) {
            return;
        }

        const term = state.searchTerm.trim().toLowerCase();
        const users = state.users.filter(function (user) {
            if (!term) {
                return true;
            }

            return user.username.toLowerCase().indexOf(term) !== -1;
        });

        refs.users.innerHTML = "";

        if (!users.length) {
            refs.users.appendChild(createEmpty("Không có người dùng phù hợp."));
            return;
        }

        users.forEach(function (user) {
            refs.users.appendChild(renderUserCard(user));
        });
    }

    function renderUserCard(user) {
        const card = document.createElement("article");
        card.className = "admin-card";

        const title = document.createElement("div");
        title.className = "admin-card-title";
        title.textContent = user.username;

        const meta = document.createElement("div");
        meta.className = "admin-card-meta";
        meta.textContent = (user.online ? "Đang hoạt động" : "Ngoại tuyến") + " • giao diện: " + (user.theme || "aurora");

        const roles = document.createElement("div");
        roles.className = "tag-list";
        (user.roles || []).forEach(function (role) {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = role;
            roles.appendChild(tag);
        });

        const actions = document.createElement("div");
        actions.className = "card-actions wrap";

        const adminToggle = document.createElement("button");
        adminToggle.type = "button";
        adminToggle.className = "ghost-mini";
        adminToggle.textContent = hasRole(user, "ADMIN") ? "Gỡ ADMIN" : "Cấp ADMIN";
        adminToggle.addEventListener("click", function () {
            toggleRole(user.username, "ADMIN", !hasRole(user, "ADMIN"));
        });

        const themeReset = document.createElement("button");
        themeReset.type = "button";
        themeReset.className = "ghost-mini";
        themeReset.textContent = "Đặt lại giao diện";
        themeReset.addEventListener("click", function () {
            updateTheme(user.username, "aurora");
        });

        const forceOffline = document.createElement("button");
        forceOffline.type = "button";
        forceOffline.className = "ghost-mini";
        forceOffline.textContent = "Buộc ngoại tuyến";
        forceOffline.addEventListener("click", function () {
            apiPost("/api/admin/users/" + encodeURIComponent(user.username) + "/force-offline", {})
                .then(loadUsers)
                .catch(handleError);
        });

        actions.appendChild(adminToggle);
        actions.appendChild(themeReset);
        actions.appendChild(forceOffline);

        card.appendChild(title);
        card.appendChild(meta);
        card.appendChild(roles);
        card.appendChild(actions);
        return card;
    }

    function toggleRole(username, role, enabled) {
        apiPatch("/api/admin/users/" + encodeURIComponent(username) + "/role/" + encodeURIComponent(role), {
            enabled: enabled
        })
            .then(loadUsers)
            .catch(handleError);
    }

    function updateTheme(username, theme) {
        apiPatch("/api/admin/users/" + encodeURIComponent(username) + "/theme", {
            theme: theme
        })
            .then(loadUsers)
            .catch(handleError);
    }

    function apiGet(path) {
        return fetch(contextPath + path, {
            credentials: "same-origin"
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

    function apiPost(path, body) {
        return fetch(contextPath + path, {
            method: "POST",
            credentials: "same-origin",
            headers: jsonHeaders(),
            body: JSON.stringify(body || {})
        }).then(handleResponse);
    }

    function jsonHeaders() {
        const headers = {
            "Content-Type": "application/json"
        };

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

    function handleError(error) {
        alert((error && error.message) || "Đã xảy ra lỗi.");
    }

    function createEmpty(text) {
        const item = document.createElement("div");
        item.className = "empty-item wide";
        item.textContent = text;
        return item;
    }

    function hasRole(user, role) {
        return (user.roles || []).indexOf(role) !== -1;
    }

    init();
})();