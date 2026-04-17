(function () {
    "use strict";

    const config = window.chatConfig || {};
    const currentUser = config.currentUser || "";
    const contextPath = (config.contextPath || "/").replace(/\/$/, "");
    const wsEndpoint = config.wsEndpoint || "/ws";
    const csrfHeader = config.csrfHeader || "X-CSRF-TOKEN";
    const csrfToken = config.csrfToken || "";

    const refs = {
        layout: document.querySelector(".chat-layout"),
        sidebar: document.getElementById("chatSidebar"),
        sidebarToggle: document.getElementById("sidebarToggle"),
        sidebarBackdrop: document.getElementById("sidebarBackdrop"),
        userSearch: document.getElementById("userSearch"),
        themeSelector: document.getElementById("themeSelector"),
        themeSelectorHeader: document.getElementById("themeSelectorHeader"),
        userList: document.getElementById("userList"),
        friendRequests: document.getElementById("friendRequests"),
        friendList: document.getElementById("friendList"),
        groupList: document.getElementById("groupList"),
        groupCreateForm: document.getElementById("groupCreateForm"),
        groupJoinForm: document.getElementById("groupJoinForm"),
        groupPanel: document.getElementById("groupPanel"),
        groupSettingsToggle: document.getElementById("groupSettingsToggle"),
        groupNameInput: document.getElementById("groupNameInput"),
        groupCodeInput: document.getElementById("groupCodeInput"),
        groupAutoApproveInput: document.getElementById("groupAutoApproveInput"),
        chatScopeLabel: document.getElementById("chatScopeLabel"),
        activePeer: document.getElementById("activePeer"),
        activeThreadMeta: document.getElementById("activeThreadMeta"),
        peerStatus: document.getElementById("peerStatus"),
        messages: document.getElementById("messages"),
        messageForm: document.getElementById("messageForm"),
        messageInput: document.getElementById("messageInput"),
        attachmentInput: document.getElementById("attachmentInput"),
        attachmentPreview: document.getElementById("attachmentPreview"),
        emojiStrip: document.getElementById("emojiStrip")
    };

    const state = {
        presenceUsers: [],
        profile: null,
        groups: [],
        activeThread: null,
        initialThread: null,
        groupSubscription: null,
        groupSettingsOpen: false,
        pendingAttachment: null,
        searchTerm: ""
    };

    let stompClient = null;

    function init() {
        parseInitialThreadFromQuery();
        setSidebarOpen(false);
        applyTheme(config.currentTheme || "aurora", false);
        bindEvents();
        connectSocket();
        refreshData();
    }

    function bindEvents() {
        if (refs.messageForm) {
            refs.messageForm.addEventListener("submit", function (event) {
                event.preventDefault();
                sendMessage();
            });
        }

        if (refs.messageInput) {
            autoResizeComposer();
            refs.messageInput.addEventListener("input", autoResizeComposer);
        }

        if (refs.userSearch) {
            refs.userSearch.addEventListener("input", function (event) {
                state.searchTerm = event.target.value || "";
                renderUsers();
                renderGroups();
            });
        }

        if (refs.themeSelector) {
            refs.themeSelector.addEventListener("change", handleThemeChange);
        }

        if (refs.themeSelectorHeader) {
            refs.themeSelectorHeader.addEventListener("change", handleThemeChange);
        }

        if (refs.groupCreateForm) {
            refs.groupCreateForm.addEventListener("submit", submitGroupCreate);
        }

        if (refs.groupJoinForm) {
            refs.groupJoinForm.addEventListener("submit", submitGroupJoin);
        }

        if (refs.attachmentInput) {
            refs.attachmentInput.addEventListener("change", handleAttachmentChange);
        }

        if (refs.emojiStrip) {
            refs.emojiStrip.addEventListener("click", function (event) {
                const button = event.target.closest("button[data-emoji]");
                if (!button || !refs.messageInput) {
                    return;
                }

                const emoji = button.getAttribute("data-emoji") || "";
                refs.messageInput.value = (refs.messageInput.value || "") + emoji;
                autoResizeComposer();
                refs.messageInput.focus();
            });
        }

        if (refs.sidebarToggle) {
            refs.sidebarToggle.addEventListener("click", function () {
                setSidebarOpen(!isSidebarOpen());
            });
        }

        if (refs.sidebarBackdrop) {
            refs.sidebarBackdrop.addEventListener("click", function () {
                setSidebarOpen(false);
            });
        }

        if (refs.groupSettingsToggle) {
            refs.groupSettingsToggle.addEventListener("click", function () {
                if (!state.activeThread || state.activeThread.type !== "group") {
                    return;
                }

                state.groupSettingsOpen = !state.groupSettingsOpen;
                if (state.groupSettingsOpen) {
                    renderGroupPanel(getGroupByCode(state.activeThread.id));
                } else {
                    hideGroupPanel();
                }

                updateGroupSettingsToggle();
            });
        }

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                setSidebarOpen(false);
            }
        });

        window.addEventListener("beforeunload", function () {
            disconnectSocket();
        });
    }

    function refreshData() {
        Promise.all([loadProfile(), loadUsers(), loadGroups()])
            .then(function () {
                renderAll();
                syncThemeSelectors((state.profile && state.profile.theme) || config.currentTheme || "aurora");
                applyInitialThreadIfNeeded();
                if (state.activeThread) {
                    renderActiveThread();
                }
            })
            .catch(function () {
                renderEmptyState("Không thể tải dữ liệu khởi tạo.");
            });
    }

    function connectSocket() {
        const socket = new SockJS(wsEndpoint);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function () {
            stompClient.subscribe("/topic/presence", function (payload) {
                state.presenceUsers = JSON.parse(payload.body || "[]");
                renderUsers();
                renderFriends();
                renderGroups();
                refreshThreadBadge();
            });

            stompClient.subscribe("/user/queue/messages", function (payload) {
                const message = JSON.parse(payload.body || "{}");
                handlePrivateIncoming(message);
            });

            stompClient.subscribe("/user/queue/errors", function (payload) {
                alert(payload.body || "Gửi tin nhắn thất bại.");
            });

            if (state.activeThread && state.activeThread.type === "group") {
                subscribeToGroup(state.activeThread.id);
            }
        });
    }

    function disconnectSocket() {
        if (stompClient && stompClient.connected) {
            stompClient.disconnect();
        }

        if (state.groupSubscription) {
            state.groupSubscription.unsubscribe();
            state.groupSubscription = null;
        }
    }

    function loadProfile() {
        return apiGet("/api/users/profile")
            .then(function (profile) {
                state.profile = profile || null;
                return profile;
            });
    }

    function loadUsers() {
        return apiGet("/api/users")
            .then(function (users) {
                state.presenceUsers = users || [];
                return users;
            });
    }

    function loadGroups() {
        return apiGet("/api/groups")
            .then(function (groups) {
                state.groups = groups || [];
                return groups;
            });
    }

    function renderAll() {
        renderUsers();
        renderFriends();
        renderGroups();
        renderActiveThread();
    }

    function renderUsers() {
        if (!refs.userList) {
            return;
        }

        const currentFriends = new Set((state.profile && state.profile.friends) || []);
        const searchTerm = (state.searchTerm || "").trim().toLowerCase();
        const peers = state.presenceUsers.filter(function (user) {
            if (!user || user.username === currentUser) {
                return false;
            }

            if (searchTerm && user.username.toLowerCase().indexOf(searchTerm) === -1) {
                return false;
            }

            return true;
        });

        refs.userList.innerHTML = "";

        if (peers.length === 0) {
            refs.userList.appendChild(createEmptyItem("Không có người dùng phù hợp."));
            return;
        }

        peers.forEach(function (user) {
            const item = document.createElement("li");
            item.className = "contact-card" + (isActiveDirect(user.username) ? " active" : "");

            const header = document.createElement("div");
            header.className = "contact-card-header";

            const title = document.createElement("button");
            title.type = "button";
            title.className = "contact-title";
            title.textContent = user.username;
            title.addEventListener("click", function () {
                selectDirect(user.username);
            });

            const status = document.createElement("span");
            status.className = "status-chip " + (user.online ? "online" : "offline");
            status.textContent = user.online ? "Đang hoạt động" : "Ngoại tuyến";

            header.appendChild(title);
            header.appendChild(status);

            const actions = document.createElement("div");
            actions.className = "card-actions";

            const chatButton = document.createElement("button");
            chatButton.type = "button";
            chatButton.className = "ghost-mini";
            chatButton.textContent = "Nhắn tin";
            chatButton.addEventListener("click", function () {
                selectDirect(user.username);
            });

            actions.appendChild(chatButton);

            if (!currentFriends.has(user.username)) {
                const addFriendButton = document.createElement("button");
                addFriendButton.type = "button";
                addFriendButton.className = "ghost-mini";
                addFriendButton.textContent = "Kết bạn";
                addFriendButton.addEventListener("click", function () {
                    sendFriendRequest(user.username);
                });
                actions.appendChild(addFriendButton);
            } else {
                const friendLabel = document.createElement("span");
                friendLabel.className = "status-chip friend";
                friendLabel.textContent = "Bạn bè";
                actions.appendChild(friendLabel);
            }

            item.appendChild(header);
            item.appendChild(actions);
            refs.userList.appendChild(item);
        });
    }

    function renderFriends() {
        renderFriendRequests();
        renderFriendList();
    }

    function renderFriendRequests() {
        if (!refs.friendRequests) {
            return;
        }

        const requests = (state.profile && state.profile.incomingFriendRequests) || [];
        refs.friendRequests.innerHTML = "";

        if (requests.length === 0) {
            refs.friendRequests.appendChild(createEmptyItem("Không có yêu cầu kết bạn."));
            return;
        }

        requests.forEach(function (username) {
            const item = createListRow(username, "Yêu cầu kết bạn");

            const accept = createButton("Duyệt", function () {
                acceptFriendRequest(username);
            });

            const reject = createButton("Từ chối", function () {
                rejectFriendRequest(username);
            });

            item.appendChild(createActionGroup([accept, reject]));
            refs.friendRequests.appendChild(item);
        });
    }

    function renderFriendList() {
        if (!refs.friendList) {
            return;
        }

        const friends = (state.profile && state.profile.friends) || [];
        refs.friendList.innerHTML = "";

        if (friends.length === 0) {
            refs.friendList.appendChild(createEmptyItem("Chưa có bạn bè nào."));
            return;
        }

        friends.forEach(function (username) {
            const item = createListRow(username, "Bạn bè");

            const chat = createButton("Nhắn tin", function () {
                selectDirect(username);
            });

            const remove = createButton("Xóa", function () {
                removeFriend(username);
            });

            item.appendChild(createActionGroup([chat, remove]));
            refs.friendList.appendChild(item);
        });
    }

    function renderGroups() {
        if (!refs.groupList) {
            return;
        }

        refs.groupList.innerHTML = "";

        const searchTerm = (state.searchTerm || "").trim().toLowerCase();
        const groups = state.groups.filter(function (group) {
            if (!searchTerm) {
                return true;
            }

            const name = (group.name || "").toLowerCase();
            const code = (group.groupCode || "").toLowerCase();
            return name.indexOf(searchTerm) !== -1 || code.indexOf(searchTerm) !== -1;
        });

        if (!groups.length) {
            refs.groupList.appendChild(createEmptyItem("Không có nhóm phù hợp."));
            return;
        }

        groups.forEach(function (group) {
            const item = document.createElement("li");
            item.className = "contact-card" + (isActiveGroup(group.groupCode) ? " active" : "");

            const header = document.createElement("div");
            header.className = "contact-card-header";

            const title = document.createElement("button");
            title.type = "button";
            title.className = "contact-title";
            title.textContent = group.name;
            title.addEventListener("click", function () {
                selectGroup(group.groupCode);
            });

            const status = document.createElement("span");
            status.className = "status-chip group";
            status.textContent = group.autoApproveJoin ? "Tự động" : "Thủ công";

            header.appendChild(title);
            header.appendChild(status);

            const meta = document.createElement("div");
            meta.className = "card-meta";
            meta.textContent = group.groupCode + " • " + group.memberUsernames.length + " thành viên";

            const actions = document.createElement("div");
            actions.className = "card-actions";

            const open = createButton("Mở", function () {
                selectGroup(group.groupCode);
            });
            actions.appendChild(open);

            item.appendChild(header);
            item.appendChild(meta);
            item.appendChild(actions);
            refs.groupList.appendChild(item);
        });
    }

    function renderActiveThread() {
        if (!state.activeThread) {
            refs.chatScopeLabel.textContent = "Sẵn sàng";
            refs.activePeer.textContent = "Chọn một người dùng hoặc nhóm";
            refs.activeThreadMeta.textContent = "Không có cuộc trò chuyện nào được mở.";
            refs.peerStatus.textContent = "Offline";
            refs.peerStatus.className = "badge";
            state.groupSettingsOpen = false;
            updateGroupSettingsToggle();
            hideGroupPanel();
            renderEmptyState("Chọn một người dùng để chat riêng hoặc mở một nhóm.");
            return;
        }

        if (state.activeThread.type === "direct") {
            refs.chatScopeLabel.textContent = "Trò chuyện riêng";
            refs.activePeer.textContent = state.activeThread.label;
            refs.activeThreadMeta.textContent = "Tin nhắn sẽ được lưu vào MongoDB theo cặp người dùng.";
            updateDirectStatus(state.activeThread.id);
            state.groupSettingsOpen = false;
            updateGroupSettingsToggle();
            hideGroupPanel();
            loadConversation();
            return;
        }

        const group = getGroupByCode(state.activeThread.id);
        refs.chatScopeLabel.textContent = "Nhóm chat";
        refs.activePeer.textContent = group ? group.name : state.activeThread.label;
        refs.activeThreadMeta.textContent = group ? (group.groupCode + " • " + group.memberUsernames.length + " thành viên") : "";
        refs.peerStatus.textContent = group && group.autoApproveJoin ? "Tự động" : "Thủ công";
        refs.peerStatus.className = "badge group";
        updateGroupSettingsToggle();
        if (state.groupSettingsOpen) {
            renderGroupPanel(group);
        } else {
            hideGroupPanel();
        }
        subscribeToGroup(state.activeThread.id);
        loadConversation();
    }

    function updateGroupSettingsToggle() {
        if (!refs.groupSettingsToggle) {
            return;
        }

        if (!state.activeThread || state.activeThread.type !== "group") {
            refs.groupSettingsToggle.classList.add("hidden-panel");
            refs.groupSettingsToggle.textContent = "Cài đặt nhóm";
            return;
        }

        refs.groupSettingsToggle.classList.remove("hidden-panel");
        refs.groupSettingsToggle.textContent = state.groupSettingsOpen ? "Ẩn cài đặt" : "Cài đặt nhóm";
    }

    function updateDirectStatus(username) {
        const user = state.presenceUsers.find(function (candidate) {
            return candidate.username === username;
        });

        if (user && user.online) {
            refs.peerStatus.textContent = "Online";
            refs.peerStatus.className = "badge online";
        } else {
            refs.peerStatus.textContent = "Offline";
            refs.peerStatus.className = "badge";
        }
    }

    function refreshThreadBadge() {
        if (!state.activeThread) {
            return;
        }

        if (state.activeThread.type === "direct") {
            updateDirectStatus(state.activeThread.id);
            return;
        }

        const group = getGroupByCode(state.activeThread.id);
        if (group) {
            refs.peerStatus.textContent = group.autoApproveJoin ? "Tự động" : "Thủ công";
            refs.peerStatus.className = "badge group";
        }
    }

    function renderGroupPanel(group) {
        if (!refs.groupPanel) {
            return;
        }

        if (!group) {
            hideGroupPanel();
            return;
        }

        const canManage = canManageGroup(group);
        refs.groupPanel.classList.remove("hidden");
        refs.groupPanel.innerHTML = "";

        const heading = document.createElement("p");
        heading.className = "group-panel-heading";
        heading.textContent = "Cài đặt nhóm";
        refs.groupPanel.appendChild(heading);

        const modeHint = document.createElement("p");
        modeHint.className = "group-mode-hint";
        modeHint.textContent = group.autoApproveJoin
            ? "Chế độ hiện tại: duyệt tự động"
            : "Chế độ hiện tại: duyệt thủ công";
        refs.groupPanel.appendChild(modeHint);

        if (canManage) {
            const manageForm = document.createElement("form");
            manageForm.className = "inline-form group-manage-form";

            const inviteInput = document.createElement("input");
            inviteInput.type = "text";
            inviteInput.maxLength = 50;
            inviteInput.placeholder = "Mời bạn vào nhóm";

            const inviteButton = document.createElement("button");
            inviteButton.type = "submit";
            inviteButton.textContent = "Mời";

            manageForm.appendChild(inviteInput);
            manageForm.appendChild(inviteButton);
            manageForm.addEventListener("submit", function (event) {
                event.preventDefault();
                inviteToGroup(group.groupCode, inviteInput.value);
                inviteInput.value = "";
            });

            const autoApproveToggle = document.createElement("button");
            autoApproveToggle.type = "button";
            autoApproveToggle.className = "ghost-mini";
            autoApproveToggle.textContent = group.autoApproveJoin ? "Tắt duyệt tự động" : "Bật duyệt tự động";
            autoApproveToggle.addEventListener("click", function () {
                updateGroupAutoApprove(group.groupCode, !group.autoApproveJoin);
            });

            refs.groupPanel.appendChild(manageForm);
            refs.groupPanel.appendChild(autoApproveToggle);
        }

        const membersTitle = document.createElement("p");
        membersTitle.className = "group-section-title";
        membersTitle.textContent = "Thành viên";
        refs.groupPanel.appendChild(membersTitle);

        refs.groupPanel.appendChild(createTagList(group.memberUsernames, "group-member"));

        if (canManage && group.pendingUsernames.length > 0) {
            const pendingTitle = document.createElement("p");
            pendingTitle.className = "group-section-title";
            pendingTitle.textContent = "Đang chờ duyệt";
            refs.groupPanel.appendChild(pendingTitle);

            const pendingList = document.createElement("div");
            pendingList.className = "tag-list";

            group.pendingUsernames.forEach(function (username) {
                const tag = document.createElement("span");
                tag.className = "tag pending";
                tag.textContent = username;

                const approve = createButton("Duyệt", function () {
                    approveGroupMember(group.groupCode, username);
                });

                const wrapper = document.createElement("div");
                wrapper.className = "tag-item";
                wrapper.appendChild(tag);
                wrapper.appendChild(approve);
                pendingList.appendChild(wrapper);
            });

            refs.groupPanel.appendChild(pendingList);
        }
    }

    function hideGroupPanel() {
        if (refs.groupPanel) {
            refs.groupPanel.classList.add("hidden");
            refs.groupPanel.innerHTML = "";
        }
    }

    function loadConversation() {
        if (!state.activeThread) {
            return;
        }

        const endpoint = state.activeThread.type === "direct"
            ? "/api/messages/" + encodeURIComponent(state.activeThread.id)
            : "/api/messages/groups/" + encodeURIComponent(state.activeThread.id);

        apiGet(endpoint)
            .then(function (messages) {
                renderConversation(messages || []);
            })
            .catch(function () {
                renderEmptyState("Không thể tải lịch sử chat.");
            });
    }

    function renderConversation(messages) {
        if (!refs.messages) {
            return;
        }

        refs.messages.innerHTML = "";

        if (!messages.length) {
            renderEmptyState("Chưa có tin nhắn. Hãy gửi tin nhắn đầu tiên.");
            return;
        }

        messages.forEach(function (message) {
            appendMessage(message);
        });
    }

    function renderEmptyState(text) {
        if (!refs.messages) {
            return;
        }

        refs.messages.innerHTML = "";
        const stateItem = document.createElement("p");
        stateItem.className = "empty-state";
        stateItem.textContent = text;
        refs.messages.appendChild(stateItem);
    }

    function appendMessage(message) {
        if (!refs.messages) {
            return;
        }

        const emptyState = refs.messages.querySelector(".empty-state");
        if (emptyState) {
            emptyState.remove();
        }

        const mine = message.sender === currentUser;
        const bubble = document.createElement("article");
        bubble.className = "bubble" + (mine ? " mine" : "");

        const header = document.createElement("div");
        header.className = "bubble-header";
        header.textContent = [message.sender || "", formatTime(message.sentAt)].filter(Boolean).join(" • ");
        bubble.appendChild(header);

        if (message.content) {
            const text = document.createElement("div");
            text.className = "bubble-text";
            text.textContent = message.content;
            bubble.appendChild(text);
        }

        if (message.attachmentUrl) {
            const media = document.createElement("div");
            media.className = "bubble-media";
            const image = document.createElement("img");
            image.alt = message.contentType === "IMAGE" ? "Ảnh" : "Tệp đính kèm";
            image.src = message.attachmentUrl;
            media.appendChild(image);
            bubble.appendChild(media);
        }

        refs.messages.appendChild(bubble);
        refs.messages.scrollTop = refs.messages.scrollHeight;
    }

    function autoResizeComposer() {
        if (!refs.messageInput) {
            return;
        }

        refs.messageInput.style.height = "auto";
        const nextHeight = Math.min(refs.messageInput.scrollHeight, 150);
        refs.messageInput.style.height = Math.max(54, nextHeight) + "px";
    }

    function sendMessage() {
        if (!state.activeThread) {
            alert("Bạn cần chọn người dùng hoặc nhóm để gửi tin nhắn.");
            return;
        }

        const content = refs.messageInput ? refs.messageInput.value.trim() : "";
        if (!content && !state.pendingAttachment) {
            return;
        }

        if (!stompClient || !stompClient.connected) {
            alert("WebSocket chưa kết nối.");
            return;
        }

        const payload = {
            content: content,
            contentType: state.pendingAttachment ? "IMAGE" : "TEXT",
            attachmentUrl: state.pendingAttachment ? state.pendingAttachment.dataUrl : null,
            attachmentMimeType: state.pendingAttachment ? state.pendingAttachment.mimeType : null
        };

        if (state.activeThread.type === "direct") {
            payload.recipient = state.activeThread.id;
            stompClient.send("/app/chat.private", {}, JSON.stringify(payload));
        } else {
            payload.groupCode = state.activeThread.id;
            stompClient.send("/app/chat.group", {}, JSON.stringify(payload));
        }

        refs.messageInput.value = "";
        autoResizeComposer();
        clearAttachment();
    }

    function handlePrivateIncoming(message) {
        if (!state.activeThread || state.activeThread.type !== "direct") {
            return;
        }

        const peer = message.sender === currentUser ? message.recipient : message.sender;
        if (peer === state.activeThread.id) {
            appendMessage(message);
        }
    }

    function subscribeToGroup(groupCode) {
        if (!stompClient || !stompClient.connected) {
            return;
        }

        if (state.groupSubscription) {
            state.groupSubscription.unsubscribe();
            state.groupSubscription = null;
        }

        state.groupSubscription = stompClient.subscribe("/topic/groups/" + groupCode, function (payload) {
            const message = JSON.parse(payload.body || "{}");
            if (state.activeThread && state.activeThread.type === "group" && state.activeThread.id === groupCode) {
                appendMessage(message);
            }
        });
    }

    function selectDirect(username) {
        state.activeThread = {
            type: "direct",
            id: username,
            label: username
        };
        state.groupSettingsOpen = false;

        if (state.groupSubscription) {
            state.groupSubscription.unsubscribe();
            state.groupSubscription = null;
        }

        setSidebarOpen(false);
        renderUsers();
        renderGroups();
        renderActiveThread();
    }

    function selectGroup(groupCode) {
        const group = getGroupByCode(groupCode);
        state.activeThread = {
            type: "group",
            id: groupCode,
            label: group ? group.name : groupCode
        };
        state.groupSettingsOpen = false;

        setSidebarOpen(false);
        renderUsers();
        renderGroups();
        renderActiveThread();
    }

    function isSidebarOpen() {
        return !!(refs.layout && refs.layout.classList.contains("sidebar-open"));
    }

    function setSidebarOpen(open) {
        if (!refs.layout) {
            return;
        }

        refs.layout.classList.toggle("sidebar-open", !!open);
    }

    function parseInitialThreadFromQuery() {
        try {
            const params = new URLSearchParams(window.location.search || "");
            const directUser = (params.get("user") || "").trim();
            const groupCode = (params.get("group") || "").trim().toUpperCase();

            if (directUser) {
                state.initialThread = {
                    type: "direct",
                    id: directUser
                };
                return;
            }

            if (groupCode) {
                state.initialThread = {
                    type: "group",
                    id: groupCode
                };
            }
        } catch (error) {
            state.initialThread = null;
        }
    }

    function applyInitialThreadIfNeeded() {
        if (!state.initialThread || state.activeThread) {
            return;
        }

        if (state.initialThread.type === "direct") {
            selectDirect(state.initialThread.id);
            state.initialThread = null;
            return;
        }

        const group = getGroupByCode(state.initialThread.id);
        if (group) {
            selectGroup(group.groupCode);
        }
        state.initialThread = null;
    }

    function sendFriendRequest(username) {
        apiPost("/api/users/friends/request", { username: username })
            .then(refreshProfileAndUsers)
            .catch(handleApiError);
    }

    function acceptFriendRequest(username) {
        apiPost("/api/users/friends/accept", { username: username })
            .then(refreshProfileAndUsers)
            .catch(handleApiError);
    }

    function rejectFriendRequest(username) {
        apiPost("/api/users/friends/reject", { username: username })
            .then(refreshProfileAndUsers)
            .catch(handleApiError);
    }

    function removeFriend(username) {
        apiDelete("/api/users/friends/" + encodeURIComponent(username))
            .then(refreshProfileAndUsers)
            .catch(handleApiError);
    }

    function submitGroupCreate(event) {
        event.preventDefault();

        const name = refs.groupNameInput ? refs.groupNameInput.value.trim() : "";
        if (!name) {
            return;
        }

        apiPost("/api/groups", {
            name: name,
            autoApproveJoin: !!(refs.groupAutoApproveInput && refs.groupAutoApproveInput.checked)
        })
            .then(function (group) {
                if (refs.groupNameInput) {
                    refs.groupNameInput.value = "";
                }
                refreshData();
                selectGroup(group.groupCode);
                alert("Đã tạo nhóm với mã: " + group.groupCode);
            })
            .catch(handleApiError);
    }

    function submitGroupJoin(event) {
        event.preventDefault();

        const groupCode = refs.groupCodeInput ? refs.groupCodeInput.value.trim() : "";
        if (!groupCode) {
            return;
        }

        apiPost("/api/groups/join", { groupCode: groupCode })
            .then(function (group) {
                if (refs.groupCodeInput) {
                    refs.groupCodeInput.value = "";
                }

                refreshData();

                if (group.memberUsernames && group.memberUsernames.indexOf(currentUser) !== -1) {
                    selectGroup(group.groupCode);
                } else {
                    alert("Đã gửi yêu cầu vào nhóm. Vui lòng chờ duyệt.");
                }
            })
            .catch(handleApiError);
    }

    function inviteToGroup(groupCode, username) {
        const clean = (username || "").trim();
        if (!clean) {
            return;
        }

        apiPost("/api/groups/" + encodeURIComponent(groupCode) + "/invite", { username: clean })
            .then(function () {
                refreshData();
            })
            .catch(handleApiError);
    }

    function approveGroupMember(groupCode, username) {
        apiPostRaw("/api/groups/" + encodeURIComponent(groupCode) + "/approve/" + encodeURIComponent(username), {})
            .then(refreshData)
            .catch(handleApiError);
    }

    function updateGroupAutoApprove(groupCode, autoApproveJoin) {
        apiPatch("/api/groups/" + encodeURIComponent(groupCode) + "/auto-approve", {
            autoApproveJoin: autoApproveJoin
        })
            .then(function (group) {
                refreshData();
                renderGroupPanel(group);
            })
            .catch(handleApiError);
    }

    function handleAttachmentChange(event) {
        const file = event.target.files && event.target.files[0];
        if (!file) {
            clearAttachment();
            return;
        }

        if (!file.type || file.type.indexOf("image/") !== 0) {
            alert("Chỉ hỗ trợ tệp ảnh.");
            clearAttachment();
            return;
        }

        const reader = new FileReader();
        reader.onload = function () {
            state.pendingAttachment = {
                dataUrl: String(reader.result || ""),
                mimeType: file.type,
                fileName: file.name
            };
            renderAttachmentPreview();
        };
        reader.readAsDataURL(file);
    }

    function renderAttachmentPreview() {
        if (!refs.attachmentPreview) {
            return;
        }

        if (!state.pendingAttachment) {
            refs.attachmentPreview.textContent = "";
            return;
        }

        refs.attachmentPreview.textContent = "Đã chọn: " + state.pendingAttachment.fileName;
    }

    function clearAttachment() {
        state.pendingAttachment = null;
        if (refs.attachmentInput) {
            refs.attachmentInput.value = "";
        }
        renderAttachmentPreview();
    }

    function handleThemeChange(event) {
        const theme = event.target.value || "aurora";
        applyTheme(theme, true);
    }

    function applyTheme(theme, persist) {
        const normalized = theme || "aurora";
        document.body.setAttribute("data-theme", normalized);
        syncThemeSelectors(normalized);

        if (persist && normalized && normalized !== (state.profile && state.profile.theme)) {
            apiPatch("/api/users/theme", { theme: normalized })
                .then(function (profile) {
                    state.profile = profile;
                    syncThemeSelectors(profile.theme || normalized);
                })
                .catch(handleApiError);
        }
    }

    function syncThemeSelectors(theme) {
        if (refs.themeSelector) {
            refs.themeSelector.value = theme;
        }

        if (refs.themeSelectorHeader) {
            refs.themeSelectorHeader.value = theme;
        }
    }

    function refreshProfileAndUsers() {
        return Promise.all([loadProfile(), loadUsers()]).then(function () {
            renderAll();
            refreshThreadBadge();
        });
    }

    function getGroupByCode(groupCode) {
        return state.groups.find(function (group) {
            return group.groupCode === groupCode;
        }) || null;
    }

    function isActiveDirect(username) {
        return state.activeThread && state.activeThread.type === "direct" && state.activeThread.id === username;
    }

    function isActiveGroup(groupCode) {
        return state.activeThread && state.activeThread.type === "group" && state.activeThread.id === groupCode;
    }

    function canManageGroup(group) {
        if (!group || !state.profile) {
            return false;
        }

        return group.ownerUsername === currentUser || (state.profile.roles || []).indexOf("ADMIN") !== -1;
    }

    function apiGet(path) {
        return fetch(contextPath + path, {
            credentials: "same-origin"
        }).then(handleResponse);
    }

    function apiPost(path, body) {
        return apiPostRaw(path, body);
    }

    function apiPostRaw(path, body) {
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

    function createTagList(items, className) {
        const container = document.createElement("div");
        container.className = "tag-list";

        (items || []).forEach(function (itemValue) {
            const tag = document.createElement("span");
            tag.className = "tag " + (className || "");
            tag.textContent = itemValue;
            container.appendChild(tag);
        });

        return container;
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