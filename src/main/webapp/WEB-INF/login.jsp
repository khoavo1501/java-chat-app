<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập Chat</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        /* Inline styles cho trang login - tập trung vào đơn giản và đẹp */
        body {
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
        }

        .login-container {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
            padding: 48px 40px;
            width: 100%;
            max-width: 400px;
            text-align: center;
        }

        .login-header {
            margin-bottom: 32px;
        }

        .login-header h1 {
            font-size: 28px;
            font-weight: 700;
            color: #1a1a2e;
            margin: 0 0 8px 0;
        }

        .login-header p {
            color: #666;
            font-size: 14px;
            margin: 0;
        }

        .chat-icon {
            width: 64px;
            height: 64px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 16px;
            margin: 0 auto 20px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .chat-icon svg {
            width: 32px;
            height: 32px;
            fill: white;
        }

        .form-group {
            margin-bottom: 20px;
            text-align: left;
        }

        .form-group label {
            display: block;
            font-size: 14px;
            font-weight: 600;
            color: #333;
            margin-bottom: 8px;
        }

        .form-group input {
            width: 100%;
            padding: 14px 16px;
            border: 2px solid #e1e1e1;
            border-radius: 10px;
            font-size: 16px;
            transition: all 0.3s ease;
            box-sizing: border-box;
        }

        .form-group input:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
        }

        .btn-submit {
            width: 100%;
            padding: 16px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
            border-radius: 10px;
            color: white;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .btn-submit:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
        }

        .btn-submit:active {
            transform: translateY(0);
        }

        .error-message {
            background: #fee;
            border: 1px solid #fcc;
            color: #c33;
            padding: 12px 16px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
        }

        .login-footer {
            margin-top: 24px;
            padding-top: 24px;
            border-top: 1px solid #eee;
            color: #999;
            font-size: 12px;
        }

        .login-footer span {
            color: #667eea;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="login-header">
            <div class="chat-icon">
                <!-- Chat Icon SVG -->
                <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
                </svg>
            </div>
            <h1>Web Chat</h1>
            <p>Nhập nickname để tham gia trò chuyện</p>
        </div>

        <%-- Hiển thị thông báo lỗi nếu có --%>
        <c:if test="${not empty errorMessage}">
            <div class="error-message">
                ${errorMessage}
            </div>
        </c:if>

        <%-- Form đăng nhập --%>
        <form action="${pageContext.request.contextPath}/login" method="POST">
            <div class="form-group">
                <label for="username">Nickname</label>
                <input type="text"
                       id="username"
                       name="username"
                       placeholder="Nhập nickname của bạn..."
                       maxlength="20"
                       required
                       autofocus>
            </div>

            <button type="submit" class="btn-submit">
                Tham gia Chat
            </button>
        </form>

        <div class="login-footer">
            Chat realtime với <span>Java WebSocket</span>
        </div>
    </div>
</body>
</html>
