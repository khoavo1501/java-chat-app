# Huong Dan Chay Local 2 May Trong Cung Mang LAN

Tai lieu nay huong dan chay ung dung chat de 2 may tinh trong cung mang LAN co the nhan tin voi nhau.

## 1. Dieu kien

- Ca 2 may cung ket noi cung mot mang LAN (wifi hoac day).
- May Host (May A) da cai Java va Maven.
- Project da co day du source code.

## 2. Tong quan

- May A: chay Tomcat 9 va deploy ung dung.
- May B: mo trinh duyet truy cap vao IP cua May A.

Luot truy cap se co dang:

http://IP_MAY_A:8080/chat/login

## 3. Buoc cho May A (May chay server)

### Buoc 1: Mo terminal tai thu muc project

Vi du:

```bash
cd /d/WorkSpace/225/225_TLTM/project/ChatRealtime/java-chat-app
```

### Buoc 2: Build file WAR

```bash
mvn clean package -DskipTests
```

Sau buoc nay se co file:

- target/chat-app.war

### Buoc 3: Tai Tomcat 9 local (neu chua co)

```bash
curl -fL "https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.80/bin/apache-tomcat-9.0.80.zip" -o target/apache-tomcat-9.0.80.zip
powershell -NoProfile -Command "Expand-Archive -Path 'target/apache-tomcat-9.0.80.zip' -DestinationPath 'target' -Force"
```

### Buoc 4: Deploy WAR vao Tomcat 9

```bash
cp target/chat-app.war target/apache-tomcat-9.0.80/webapps/chat.war
```

### Buoc 5: Chay Tomcat 9

```bash
target/apache-tomcat-9.0.80/bin/catalina.sh run
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

- http://192.168.1.25:8080/chat/login

Thay 192.168.1.25 bang IP thuc te cua May A.

Dang nhap voi nickname bat ky de vao phong chat.

## 6. Kiem tra nhanh ket noi

Tu May B, co the test truoc bang:

```bash
curl -I http://IP_MAY_A:8080/chat/login
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

Doi cong trong file:

- target/apache-tomcat-9.0.80/conf/server.xml

Sau do doi URL truy cap theo cong moi.

### Loi 500 khi vao /chat

Build lai va deploy lai WAR:

```bash
mvn package -DskipTests
cp target/chat-app.war target/apache-tomcat-9.0.80/webapps/chat.war
```

Neu can, dung Tomcat roi chay lai.

## 9. Dung server

Tai terminal dang chay Tomcat, nhan:

- Ctrl + C
