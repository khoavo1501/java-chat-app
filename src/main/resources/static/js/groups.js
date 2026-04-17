(function () {
    "use strict";

    const config = window.groupsConfig || {};
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");
    const csrfHeader = config.csrfHeader || "X-CSRF-TOKEN";
    const csrfToken = config.csrfToken || "";

    const refs = {
        toggleCreateGroup: document.getElementById("toggleCreateGroup"),
        toggleJoinGroup: document.getElementById("toggleJoinGroup"),
        createForm: document.getElementById("groupsCreateForm"),
        joinForm: document.getElementById("groupsJoinForm"),
        nameInput: document.getElementById("groupsNameInput"),
        autoApproveInput: document.getElementById("groupsAutoApproveInput"),
        codeInput: document.getElementById("groupsCodeInput"),
        groupsList: document.getElementById("groupsList")
    };

    const state = {
        groups: []
    };

    function init() {
        bindEvents();
        refreshGroups();
    }

    function bindEvents() {
        if (refs.toggleCreateGroup) {
            refs.toggleCreateGroup.addEventListener("click", function () {
                togglePanel(refs.createForm);
            });
        }

        if (refs.toggleJoinGroup) {
            refs.toggleJoinGroup.addEventListener("click", function () {
                togglePanel(refs.joinForm);
            });
        }

        if (refs.createForm) {
            refs.createForm.addEventListener("submit", submitCreateGroup);
        }

        if (refs.joinForm) {
            refs.joinForm.addEventListener("submit", submitJoinGroup);
        }
    }

    function togglePanel(panel) {
        if (!panel) {
            return;
        }

        panel.classList.toggle("hidden-panel");
    }

    function refreshGroups() {
        apiGet("/api/groups")
            .then(function (groups) {
                state.groups = groups || [];
                renderGroups();
            })
            .catch(handleApiError);
    }

    function renderGroups() {
        if (!refs.groupsList) {
            return;
        }

        refs.groupsList.innerHTML = "";

        if (!state.groups.length) {
            refs.groupsList.appendChild(createEmpty("Chưa có nhóm nào."));
            return;
        }

        state.groups.forEach(function (group) {
            const card = document.createElement("article");
            card.className = "contact-card";

            const header = document.createElement("div");
            header.className = "contact-card-header";

            const title = document.createElement("span");
            title.className = "contact-title static";
            title.textContent = group.name + " • " + group.groupCode;

            const status = document.createElement("span");
            status.className = "status-chip group";
            status.textContent = group.autoApproveJoin ? "Tự động" : "Thủ công";

            header.appendChild(title);
            header.appendChild(status);

            const meta = document.createElement("div");
            meta.className = "card-meta";
            meta.textContent = "Chủ nhóm: " + group.ownerUsername + " • " + group.memberUsernames.length + " thành viên";

            const members = document.createElement("div");
            members.className = "tag-list";
            (group.memberUsernames || []).forEach(function (member) {
                const tag = document.createElement("span");
                tag.className = "tag group-member";
                tag.textContent = member;
                members.appendChild(tag);
            });

            const actions = document.createElement("div");
            actions.className = "card-actions";

            actions.appendChild(createButton("Mở chat", function () {
                window.location.href = contextPath + "/chat?group=" + encodeURIComponent(group.groupCode);
            }));

            if (canManage(group)) {
                actions.appendChild(createButton(group.autoApproveJoin ? "Tắt duyệt tự động" : "Bật duyệt tự động", function () {
                    toggleAutoApprove(group.groupCode, !group.autoApproveJoin);
                }));

                actions.appendChild(createButton("Mời thành viên", function () {
                    const username = prompt("Nhập username cần mời vào nhóm:");
                    if (username) {
                        inviteMember(group.groupCode, username.trim());
                    }
                }));
            }

            card.appendChild(header);
            card.appendChild(meta);
            card.appendChild(members);

            if (canManage(group) && group.pendingUsernames && group.pendingUsernames.length) {
                const pending = document.createElement("div");
                pending.className = "tag-list";

                group.pendingUsernames.forEach(function (username) {
                    const wrap = document.createElement("div");
                    wrap.className = "tag-item";

                    const tag = document.createElement("span");
                    tag.className = "tag pending";
                    tag.textContent = username;

                    const approve = createButton("Duyệt", function () {
                        approveMember(group.groupCode, username);
                    });

                    wrap.appendChild(tag);
                    wrap.appendChild(approve);
                    pending.appendChild(wrap);
                });

                card.appendChild(pending);
            }

            card.appendChild(actions);
            refs.groupsList.appendChild(card);
        });
    }

    function submitCreateGroup(event) {
        event.preventDefault();

        const name = refs.nameInput ? refs.nameInput.value.trim() : "";
        if (!name) {
            return;
        }

        apiPost("/api/groups", {
            name: name,
            autoApproveJoin: !!(refs.autoApproveInput && refs.autoApproveInput.checked)
        })
            .then(function (group) {
                if (refs.nameInput) {
                    refs.nameInput.value = "";
                }
                if (refs.autoApproveInput) {
                    refs.autoApproveInput.checked = false;
                }
                refreshGroups();
                alert("Đã tạo nhóm với mã: " + group.groupCode);
            })
            .catch(handleApiError);
    }

    function submitJoinGroup(event) {
        event.preventDefault();

        const groupCode = refs.codeInput ? refs.codeInput.value.trim() : "";
        if (!groupCode) {
            return;
        }

        apiPost("/api/groups/join", { groupCode: groupCode })
            .then(function (group) {
                if (refs.codeInput) {
                    refs.codeInput.value = "";
                }

                refreshGroups();

                const isMember = (group.memberUsernames || []).indexOf(config.currentUser || "") !== -1;
                if (isMember) {
                    window.location.href = contextPath + "/chat?group=" + encodeURIComponent(group.groupCode);
                    return;
                }

                alert("Đã gửi yêu cầu vào nhóm. Vui lòng chờ duyệt.");
            })
            .catch(handleApiError);
    }

    function toggleAutoApprove(groupCode, autoApproveJoin) {
        apiPatch("/api/groups/" + encodeURIComponent(groupCode) + "/auto-approve", {
            autoApproveJoin: autoApproveJoin
        })
            .then(refreshGroups)
            .catch(handleApiError);
    }

    function inviteMember(groupCode, username) {
        if (!username) {
            return;
        }

        apiPost("/api/groups/" + encodeURIComponent(groupCode) + "/invite", {
            username: username
        })
            .then(refreshGroups)
            .catch(handleApiError);
    }

    function approveMember(groupCode, username) {
        apiPost("/api/groups/" + encodeURIComponent(groupCode) + "/approve/" + encodeURIComponent(username), {})
            .then(refreshGroups)
            .catch(handleApiError);
    }

    function canManage(group) {
        return group && (group.ownerUsername === (config.currentUser || "") || !!config.isAdmin);
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

    function jsonHeaders() {
        const headers = {};
        if (csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        headers["Content-Type"] = "application/json";
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
        item.className = "empty-item";
        item.textContent = text;
        return item;
    }

    init();
})();
