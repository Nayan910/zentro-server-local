# Zentro Server Fork

**Local Client-Server Architecture for Zentro Rajkot**

A fork of the Zentro app that replaces Firebase with a local Java Spring Boot server running on your laptop. All data is stored locally, no internet required.

---

## Team

- **Chauhan Dhruv**
- **Nayan Chotaliya**
- **Yagna Desai**
- **Daksh Gondoliya**

**Mentor:** S. V. Ramani, A.V.P.T.I. Rajkot

---

## Architecture

```
┌─────────────────┐     HTTP/WebSocket     ┌─────────────────┐
│  Android App    │ ◄──────────────────► │  Laptop Server   │
│  (Client)       │                        │  (Spring Boot)  │
└─────────────────┘                        └─────────────────┘
                                                   │
                                           ┌───────┴───────┐
                                           │  H2 Database   │
                                           │  (Embedded)    │
                                           └───────────────┘
```

- **Server:** Java Spring Boot application running on your laptop
- **Database:** H2 embedded database (data stored in `server/data/` folder)
- **Communication:** REST API + WebSocket for real-time chat
- **Authentication:** JWT tokens

---

## Quick Start

### Step 1: Start the Server

**Option A: Using Maven (recommended)**
```bash
cd ZentroServerFork/server
mvn spring-boot:run
```

**Option B: Using the BAT file**
```
Double-click START_SERVER.bat
```

**Option C: Using the JAR file**
```bash
cd ZentroServerFork/server
mvn clean package -DskipTests
java -jar target\zentro-server-1.0.0.jar
```

The server will start on port 8080. Note the IP address shown in the console.

### Step 2: Connect Android App

1. Install the modified Android app on your phone
2. On the login screen, enter your **laptop's IP address** (e.g., `192.168.1.100`)
3. Tap **Connect** to test the connection
4. Register a new account or login with existing credentials

### Finding Your Laptop's IP

**Windows:**
```bash
ipconfig
```
Look for "IPv4 Address" under your WiFi/Ethernet adapter.

**Mac/Linux:**
```bash
ifconfig
```
Look for "inet" under your network interface.

---

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login user |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/{id}` | Get user profile |
| PUT | `/api/users/{id}` | Update user profile |
| GET | `/api/users` | Get all workers |
| GET | `/api/users/search?q=` | Search users |

### Gigs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/gigs` | Create new gig |
| GET | `/api/gigs` | Get all open gigs |
| GET | `/api/gigs/my` | Get my gigs |
| GET | `/api/gigs/{id}` | Get gig details |
| POST | `/api/gigs/{id}/apply` | Apply for gig |
| GET | `/api/gigs/{id}/applications` | Get gig applications |

### Chat
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat/send` | Send message |
| GET | `/api/chat/{userId}` | Get conversation |
| POST | `/api/chat/read` | Mark messages read |
| GET | `/api/chat/conversations` | Get conversations list |
| GET | `/api/chat/unread/count` | Get unread count |

### WebSocket
| Endpoint | Description |
|----------|-------------|
| `ws://SERVER:8080/ws/chat?token=JWT` | Real-time chat |

### Logs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/logs` | Get all activity logs |
| GET | `/api/logs/user/{id}` | Get user's logs |

---

## Database Models

### User
- `id` - Unique identifier
- `username` - Username (unique)
- `email` - Email (unique)
- `password` - Hashed password
- `phone` - Phone number
- `role` - worker, customer, admin
- `location` - City/location
- `tradeCategory` - Trade/skill
- `experienceYears` - Years of experience
- `rating` - User rating
- `isAvailable` - Available for work

### Gig
- `id` - Unique identifier
- `title` - Gig title
- `description` - Detailed description
- `budget` - Payment amount
- `location` - Work location
- `tradeCategory` - Required trade
- `user` - Posted by (FK)
- `status` - open, in_progress, completed, cancelled
- `acceptedBy` - Accepted worker (FK)

### Message
- `id` - Unique identifier
- `sender` - Sender user (FK)
- `receiver` - Receiver user (FK)
- `content` - Message text
- `isRead` - Read status
- `timestamp` - Send time

### ActivityLog
- `id` - Unique identifier
- `user` - User who performed action (FK)
- `action` - Action type (LOGIN, GIG_CREATE, etc.)
- `details` - Action details
- `ipAddress` - Client IP
- `timestamp` - Action time

---

## Project Structure

```
ZentroServerFork/
├── server/                          # Java Spring Boot server
│   ├── src/main/java/com/zentro/server/
│   │   ├── ZentroServerApplication.java    # Main entry point
│   │   ├── config/
│   │   │   ├── CorsConfig.java             # CORS configuration
│   │   │   ├── SecurityConfig.java         # Spring Security + JWT
│   │   │   └── WebSocketConfig.java        # WebSocket configuration
│   │   ├── controller/
│   │   │   ├── AuthController.java         # /api/auth/*
│   │   │   ├── UserController.java         # /api/users/*
│   │   │   ├── GigController.java          # /api/gigs/*
│   │   │   ├── ChatController.java         # /api/chat/*
│   │   │   └── LogController.java          # /api/logs/*
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Gig.java
│   │   │   ├── GigApplication.java
│   │   │   ├── Message.java
│   │   │   └── ActivityLog.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── GigRepository.java
│   │   │   ├── GigApplicationRepository.java
│   │   │   ├── MessageRepository.java
│   │   │   └── ActivityLogRepository.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── UserService.java
│   │   │   ├── GigService.java
│   │   │   ├── ChatService.java
│   │   │   └── ActivityLogService.java
│   │   ├── security/
│   │   │   ├── JwtUtil.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   └── CustomUserDetailsService.java
│   │   └── websocket/
│   │       └── ChatWebSocketHandler.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── banner.txt
│   └── pom.xml
├── android/                         # Modified Android app
│   └── app/src/main/java/com/skillmatch/rajkot/
│       ├── activities/
│       │   ├── LoginActivity.java         # Modified with IP input
│       │   └── RegisterActivity.java      # Server registration
│       ├── api/
│       │   ├── ApiClient.java             # REST API client
│       │   └── ChatWebSocketClient.java   # WebSocket client
│       └── res/layout/
│           ├── activity_login_server.xml  # Login with IP field
│           └── activity_register_server.xml
├── START_SERVER.bat                 # One-click server start
├── QUICK_START.bat                  # Build + start
└── README.md                        # This file
```

---

## Requirements

### Server
- Java 21 or higher
- Maven 3.6+ (or use Maven wrapper)
- Port 8080 available

### Android
- Android SDK 26+ (minSdk)
- Android SDK 35 (targetSdk)
- Internet permission for local network

---

## Configuration

### Server Configuration (`application.properties`)
```properties
server.port=8080
spring.datasource.url=jdbc:h2:file:./data/zentrodb
zentro.jwt.secret=YourSecretKeyHere
zentro.jwt.expiration=86400000
```

### Android Configuration
The server IP is stored in the app's SharedPreferences. Users can change it from the login screen.

---

## Development

### Building the Server
```bash
cd server
mvn clean install
```

### Running Tests
```bash
cd server
mvn test
```

### Database Console
Access H2 console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/zentrodb`
- Username: `sa`
- Password: `zentro2026`

---

## Troubleshooting

### "Connection refused" on Android
1. Make sure the server is running
2. Check that your phone and laptop are on the same WiFi network
3. Verify the IP address is correct
4. Check firewall settings (allow port 8080)

### Server won't start
1. Check if Java 21 is installed: `java -version`
2. Check if port 8080 is in use
3. Check the `server/data/` folder permissions

### "Already applied" error
Each user can only apply once per gig. This is by design.

---

## License

College minor project - A.V.P.T.I. Rajkot
