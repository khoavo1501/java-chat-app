# Java Chat Realtime - Huong Dan Chay

Ung dung hien tai da duoc nang cap sang Spring Boot, Spring Security, MongoDB va STOMP WebSocket.

## 1) Yeu cau moi truong

- JDK 11 tro len
- Maven 3.6 tro len
- MongoDB 6 tro len
- Trinh duyet bat ky (Chrome, Edge, Firefox)

Kiem tra nhanh:

	java -version
	mvn -version

## 2) Chay du an trong VS Code

- Mo terminal tai thu muc goc du an
- Chay lenh:

	mvn clean spring-boot:run

Sau khi app khoi dong, truy cap:

- http://localhost:8080/login

## 3) Dang nhap va test nhanh

- Vao trang Dang ky: http://localhost:8080/register
- Tao 2 tai khoan khac nhau
- Dang nhap 2 tai khoan tren 2 tab trinh duyet
- Chon user o danh sach ben trai de chat 1-1

## 4) Database va du lieu

- App dung MongoDB, mac dinh ket noi toi `mongodb://localhost:27017/java_chat_app`
- Neu chay Docker Compose, MongoDB duoc khoi tao cung container app
- Lich su tin nhan, ban be, theme va group duoc luu trong MongoDB
- Tai khoan admin mac dinh:
	- Username: `admin`
	- Password: `admin123`

## 5) Build file jar de deploy

	mvn clean package

File jar tao ra tai:

- target/java-chat-app-2.0.0.jar

Chay file jar:

	java -jar target/java-chat-app-2.0.0.jar

## 5.1) Deploy bang Docker Compose

Build va chay container:

	docker compose up -d --build

Xem log:

	docker compose logs -f

Dung va xoa container:

	docker compose down

Ghi chu:

- Docker Compose cung cap MongoDB trong service `mongo`
- Truy cap app tai http://localhost:8080/login
- Admin portal o http://localhost:8080/admin

## 5.2) Deploy len Linux server bang systemd

Buoc 1: Build jar tren local

	mvn clean package

Buoc 2: Copy file jar len server (vi du)

	scp target/java-chat-app-2.0.0.jar user@your-server:/opt/java-chat-app/app.jar

Buoc 3: Tao service file /etc/systemd/system/java-chat-app.service

	[Unit]
	Description=Java Chat App
	After=network.target

	[Service]
	User=www-data
	WorkingDirectory=/opt/java-chat-app
	ExecStart=/usr/bin/java -jar /opt/java-chat-app/app.jar
	SuccessExitStatus=143
	Restart=always
	RestartSec=5

	[Install]
	WantedBy=multi-user.target

Buoc 4: Enable va start service

	sudo systemctl daemon-reload
	sudo systemctl enable java-chat-app
	sudo systemctl start java-chat-app
	sudo systemctl status java-chat-app

Buoc 5: Neu dung Nginx reverse proxy

- Forward ve http://127.0.0.1:8080
- Bat websocket headers:
  - Upgrade $http_upgrade
  - Connection "upgrade"

## 6) Cac lenh Maven hay dung

	mvn clean
	mvn compile
	mvn test
	mvn package
	mvn spring-boot:run

## 7) Loi thuong gap

Port 8080 dang bi chiem:

- Dung tien trinh dang chiem cong 8080
- Hoac doi server.port trong src/main/resources/application.properties

Maven khong tai duoc dependency:

	mvn -U clean package

Khong ket noi duoc MongoDB:

- Kiem tra MongoDB da chay chua
- Neu chay local, dam bao `mongodb://localhost:27017/java_chat_app` truy cap duoc
- Neu chay Docker, kiem tra service `mongo` trong `docker-compose.yml`

Sai version Java:

- Dam bao Maven dang chay voi JDK 11+
