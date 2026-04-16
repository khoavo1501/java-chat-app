<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lỗi - Chat App</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
        }

        .error-container {
            text-align: center;
            padding: 40px;
            background: white;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
            max-width: 500px;
        }

        .error-code {
            font-size: 72px;
            font-weight: 700;
            color: #667eea;
            margin-bottom: 16px;
        }

        .error-title {
            font-size: 24px;
            color: #2d3748;
            margin-bottom: 16px;
        }

        .error-message {
            color: #718096;
            margin-bottom: 24px;
        }

        .btn-home {
            display: inline-block;
            padding: 12px 32px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 8px;
            font-weight: 600;
            transition: transform 0.2s;
        }

        .btn-home:hover {
            transform: translateY(-2px);
        }
    </style>
</head>
<body>
    <div class="error-container">
        <div class="error-code">
            <%= request.getAttribute("javax.servlet.error.status_code") != null
                ? request.getAttribute("javax.servlet.error.status_code")
                : "Error" %>
        </div>
        <h1 class="error-title">Oops! Đã xảy ra lỗi</h1>
        <p class="error-message">
            <%= exception != null ? exception.getMessage() : "Trang bạn đang tìm kiếm không tồn tại hoặc có lỗi xảy ra." %>
        </p>
        <a href="${pageContext.request.contextPath}/login" class="btn-home">
            Quay về trang chủ
        </a>
    </div>
</body>
</html>
