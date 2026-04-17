# Huong Dan Chay Local 2 May Trong Cung Mang LAN

Tai lieu nay huong dan chay ung dung chat de 2 may tinh trong cung mang LAN co the nhan tin voi nhau.

## 1. Dieu kien

- Ca 2 may cung ket noi cung mot mang LAN (wifi hoac day).
- May Host (May A) da cai Java va Maven.
- Project da co day du source code.

## 2. Tong quan

- May A: chay ung dung Spring Boot va MongoDB.
- May B: mo trinh duyet truy cap vao IP cua May A.

Luot truy cap se co dang:

http://IP_MAY_A:8080/chat/login

## 3. Buoc cho May A (May chay server)

### Buoc 1: Mo terminal tai thu muc project

Vi du:

```bash
cd /d/WorkSpace/225/225_TLTM/project/ChatRealtime/java-chat-app
```

### Buoc 2: Build file JAR

```bash
mvn clean package -DskipTests
```

Sau buoc nay se co file:

- target/java-chat-app-2.0.0.jar

### Buoc 3: Chay MongoDB local

```bash
docker run --name java-chat-mongo -p 27017:27017 -d mongo:6.0
```

Neu da cai MongoDB local, co the bo qua buoc nay.

### Buoc 4: Chay ung dung

```bash
java -jar target/java-chat-app-2.0.0.jar
```

Neu thay log startup thanh cong, server dang chay o cong 8080.

## 4. Tim IP LAN cua May A

Tren Windows, chay:

```powershell
ipconfig
```

Tim dong IPv4 Address, vi du:

- 192.168.1.25

## 5. Buoc cho May B (May truy cap)

Mo trinh duyet va vao:

- http://192.168.1.25:8080/login

Thay 192.168.1.25 bang IP thuc te cua May A.

Dang nhap voi nickname bat ky de vao phong chat.

## 6. Kiem tra nhanh ket noi

Tu May B, co the test truoc bang:

```bash
curl -I http://IP_MAY_A:8080/login
```

Neu tra ve HTTP 200 la truy cap duoc.

## 7. Mo cong firewall (neu May B khong vao duoc)

Tren May A (Windows), mo cong 8080:

```powershell
netsh advfirewall firewall add rule name="Tomcat 8080" dir=in action=allow protocol=TCP localport=8080
```

Sau do thu truy cap lai tu May B.

## 8. Loi thuong gap

### Loi khong vao duoc tu May B

- Kiem tra 2 may co cung mang LAN khong.
- Kiem tra IP May A co doi khong.
- Kiem tra firewall da mo cong 8080 chua.
- Kiem tra Tomcat tren May A con dang chay khong.

### Loi cong 8080 da duoc su dung

Doi cong trong file `src/main/resources/application.properties` hoac truyen `server.port` khi chay jar.

### Loi 500 khi vao /chat

Kiem tra da chay MongoDB chua va app co ket noi duoc toi `mongodb://localhost:27017/java_chat_app` khong.

### Loi dang nhap admin portal

- Dam bao dang nhap bang tai khoan `admin` / `admin123`
- Chi role ADMIN moi vao duoc /admin

## 9. Dung server

Tai terminal dang chay Tomcat, nhan:

- Ctrl + C
