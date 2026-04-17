# Huong Dan Chay Du An Java Chat App

Tai lieu nay huong dan chay du an theo 2 cach:
- Cach 1: Chay local bang Maven (can MongoDB local)
- Cach 2: Chay bang Docker Compose (khuyen nghi de nhanh)

## 1. Yeu cau moi truong

- Java JDK 11+
- Maven 3.6+
- Docker Desktop (neu chay bang Docker Compose)

Kiem tra nhanh:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## 2. Cau hinh quan trong

Ung dung doc MongoDB URI tu bien moi truong `MONGODB_URI`.
Neu khong co bien nay, app dung mac dinh:

`mongodb://localhost:27017/java_chat_app`

Gia tri nay dang duoc dat trong `src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/java_chat_app}
```

## 3. Cach 1 - Chay local bang Maven

### Buoc 1: Chay MongoDB local

Neu ban da co MongoDB local, bo qua buoc nay.
Neu chua co, co the chay nhanh bang Docker:

```bash
docker run --name java-chat-mongo -p 27017:27017 -d mongo:6.0
```

### Buoc 2: Chay ung dung

Tai thu muc goc du an:

```bash
mvn clean spring-boot:run
```

### Buoc 3: Truy cap ung dung

- Login: http://localhost:8080/login
- Dang ky: http://localhost:8080/register
- Admin: http://localhost:8080/admin

Tai khoan admin mac dinh:
- Username: admin
- Password: admin123

## 4. Cach 2 - Chay bang Docker Compose (khuyen nghi)

Cach nay se chay ca app va MongoDB cung luc.

### Buoc 1: Build va chay

```bash
docker compose up -d --build
```

### Buoc 2: Kiem tra log

```bash
docker compose logs -f chat-app
```

### Buoc 3: Truy cap ung dung

- Login: http://localhost:8080/login
- Admin: http://localhost:8080/admin

### Buoc 4: Dung he thong

```bash
docker compose down
```

## 5. Build JAR va chay thu cong

Build:

```bash
mvn clean package
```

Chay:

```bash
java -jar target/java-chat-app-2.0.0.jar
```

## 6. Bien moi truong co the tuy chinh

- `MONGODB_URI` (VD: mongodb://localhost:27017/java_chat_app)
- `APP_ADMIN_USERNAME` (mac dinh: admin)
- `APP_ADMIN_PASSWORD` (mac dinh: admin123)
- `SERVER_PORT` (neu muon doi cong)

## 7. Loi thuong gap

### Port 8080 bi chiem

Chay app voi cong khac:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Khong ket noi duoc MongoDB

- Kiem tra MongoDB da chay chua.
- Kiem tra dung URI chua.
- Neu chay Docker Compose, dam bao service `mongo` dang running:

```bash
docker compose ps
```

### Docker Compose build loi

Thu build lai khong dung cache:

```bash
docker compose build --no-cache
docker compose up -d
```

## 8. Deploy len Google Cloud Run

### Luu y quan trong truoc khi deploy

- Khong duoc de `MONGODB_URI` la `localhost` tren Cloud Run.
- Can dung MongoDB public/managed (thuong la MongoDB Atlas), vi Cloud Run khong chay kem MongoDB nhu local Docker Compose.

### Buoc 1: Chuan bi project GCP

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
```

### Buoc 2: Deploy service

```bash
gcloud run deploy java-chat-app \
	--source . \
	--region asia-southeast1 \
	--allow-unauthenticated \
	--memory 1Gi \
	--cpu 1 \
	--max-instances 1 \
	--set-env-vars MONGODB_URI="mongodb+srv://<user>:<pass>@<cluster>/<db>?retryWrites=true&w=majority",APP_ADMIN_USERNAME="admin",APP_ADMIN_PASSWORD="admin123"
```

Sau khi deploy thanh cong, Cloud Run se tra ve URL public.

### Buoc 3: Xem log neu deploy/startup loi

```bash
gcloud run services logs read java-chat-app --region asia-southeast1 --limit 100
```

### Cac loi hay gap khi deploy Cloud Run

- Loi startup do khong ket noi duoc MongoDB:
	- Nguyen nhan thuong gap: app dang dung URI mac dinh `mongodb://localhost:27017/java_chat_app`.
	- Cach xu ly: set bien moi truong `MONGODB_URI` bang URI MongoDB Atlas hoac MongoDB public.

- Loi timeout/start failed:
	- Kiem tra service co nghe cong 8080 hay khong (project hien tai da set `server.port=8080`, phu hop Cloud Run).

- Loi quyen truy cap GCP API:
	- Chay lai lenh enable API o Buoc 1 va kiem tra dung `project`.

### Goi y bao mat

- Khong de thang password trong command khi deploy that.
- Nen luu `MONGODB_URI` va `APP_ADMIN_PASSWORD` trong Secret Manager, sau do gan vao Cloud Run environment variables.
