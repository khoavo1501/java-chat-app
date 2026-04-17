(function () {
    "use strict";

    const config = window.adminGroupsConfig || {};
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");
    const csrfHeader = config.csrfHeader || "X-CSRF-TOKEN";
    const csrfToken = config.csrfToken || "";

    const refs = {
        search: document.getElementById("adminGroupSearch"),
        groups: document.getElementById("adminGroups")
    };

    const state = {
        groups: [],
        searchTerm: ""
    };

    function init() {
        bindEvents();
        loadGroups();
    }

    function bindEvents() {
        if (refs.search) {
            refs.search.addEventListener("input", function (event) {
                state.searchTerm = event.target.value || "";
                renderGroups();
            });
        }
    }

    function loadGroups() {
        apiGet("/api/admin/groups")
            .then(function (groups) {
                state.groups = groups || [];
                renderGroups();
            })
            .catch(handleError);
    }

    function renderGroups() {
        if (!refs.groups) {
            return;
        }

        const term = (state.searchTerm || "").trim().toLowerCase();
        const groups = state.groups.filter(function (group) {
            if (!term) {
                return true;
            }

            const name = (group.name || "").toLowerCase();
            const code = (group.groupCode || "").toLowerCase();
            const owner = (group.ownerUsername || "").toLowerCase();
            return name.indexOf(term) !== -1 || code.indexOf(term) !== -1 || owner.indexOf(term) !== -1;
        });

        refs.groups.innerHTML = "";

        if (!groups.length) {
            refs.groups.appendChild(createEmpty("Khong co nhom phu hop."));
            return;
        }

        groups.forEach(function (group) {
            refs.groups.appendChild(renderGroupCard(group));
        });
    }

    function renderGroupCard(group) {
        const card = document.createElement("article");
        card.className = "admin-card";

        const title = document.createElement("div");
        title.className = "admin-card-title";
        title.textContent = group.name + " • " + group.groupCode;

        const meta = document.createElement("div");
        meta.className = "admin-card-meta";
        meta.textContent = "Chu nhom: " + (group.ownerUsername || "-")
            + " • Thanh vien: " + ((group.memberUsernames || []).length)
            + " • Cho duyet: " + ((group.pendingUsernames || []).length);

        const mode = document.createElement("span");
        mode.className = "status-chip group";
        mode.textContent = group.autoApproveJoin ? "Tu dong duyet" : "Duyet thu cong";

        const members = document.createElement("div");
        members.className = "tag-list";
        (group.memberUsernames || []).slice(0, 10).forEach(function (member) {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = member;
            members.appendChild(tag);
        });

        const actions = document.createElement("div");
        actions.className = "card-actions wrap";

        const openChatButton = createButton("Mo chat", function () {
            window.location.href = contextPath + "/chat?group=" + encodeURIComponent(group.groupCode || "");
        });

        const toggleAutoApproveButton = createButton(
            group.autoApproveJoin ? "Tat duyet tu dong" : "Bat duyet tu dong",
            function () {
                updateGroupAutoApprove(group.groupCode, !group.autoApproveJoin);
            }
        );

        const deleteButton = createButton("Xoa nhom", function () {
            if (confirm("Ban chac chan muon xoa nhom " + group.name + "?")) {
                deleteGroup(group.groupCode);
            }
        });

        actions.appendChild(openChatButton);
        actions.appendChild(toggleAutoApproveButton);
        actions.appendChild(deleteButton);

        card.appendChild(title);
        card.appendChild(meta);
        card.appendChild(mode);
        card.appendChild(members);
        card.appendChild(actions);
        return card;
    }

    function updateGroupAutoApprove(groupCode, autoApproveJoin) {
        apiPatch("/api/admin/groups/" + encodeURIComponent(groupCode) + "/auto-approve", {
            autoApproveJoin: autoApproveJoin
        })
            .then(loadGroups)
            .catch(handleError);
    }

    function deleteGroup(groupCode) {
        apiDelete("/api/admin/groups/" + encodeURIComponent(groupCode))
            .then(loadGroups)
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

    function apiDelete(path) {
        return fetch(contextPath + path, {
            method: "DELETE",
            credentials: "same-origin",
            headers: jsonHeaders()
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

    function createButton(label, handler) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "ghost-mini";
        button.textContent = label;
        button.addEventListener("click", handler);
        return button;
    }

    function createEmpty(text) {
        const item = document.createElement("div");
        item.className = "empty-item wide";
        item.textContent = text;
        return item;
    }

    function handleError(error) {
        alert((error && error.message) || "Da xay ra loi.");
    }

    init();
})();
