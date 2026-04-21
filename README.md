# Airline Reservation System (ARS)

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Features](#3-features)
4. [Technology Stack](#4-technology-stack)
5. [External API Integrations](#5-external-api-integrations)
6. [Object-Oriented Design](#6-object-oriented-design)
7. [Project Structure](#7-project-structure)
8. [Team Contributions](#8-team-contributions)
9. [Setup and Installation](#9-setup-and-installation)
10. [API Reference](#10-api-reference)
11. [Booking Workflow](#11-booking-workflow)
12. [Loyalty Programme](#12-loyalty-programme)
13. [Available Routes](#13-available-routes)

---

## 1. Project Overview

The Airline Reservation System is a full-stack web application developed as part of the Object Oriented Analysis and Design course. The system enables users to search for domestic and international flights, select seats, manage passenger travel documents, process payments, track live flight status, and participate in a loyalty rewards programme — all through a responsive, modern web interface.

The backend is built on Spring Boot 3.3 with a layered architecture strictly following GRASP and SOLID principles. The frontend is a React single-page application communicating exclusively with the backend via a Vite development proxy. All external API calls — including Aviationstack for flight tracking, Google Gemini for AI-powered trip suggestions, and Razorpay for payment processing — are made server-side only. API keys are never exposed to the browser at any point.

Data persistence is handled entirely through Firebase Firestore, a NoSQL cloud database, which serves as the single source of truth for all application data including users, flights, bookings, payments, and loyalty accounts.

---

## 2. System Architecture

### High-Level Architecture

```
Browser (React + Vite — Port 5173)
            |
            |  All HTTP requests routed via Vite development proxy
            |  (eliminates CORS issues in development)
            v
Spring Boot Backend (Port 8080)
            |
            |-- Firebase Firestore       Primary data store for all entities
            |-- Aviationstack API        Live flight status and tracking
            |-- Google Gemini API        AI-powered personalised trip suggestions
            |-- Razorpay                 Payment gateway (demo mode)
            |-- AeroDataBox / Amadeus    Attempted scheduled flight data (see Section 5)
```

### Backend Layer Responsibilities

```
controller/     HTTP request routing and response mapping only.
                No business logic. Implements GRASP Controller principle.

service/        All business logic and orchestration.
                Each service class has a single, well-defined responsibility (SRP).

repository/     Firestore persistence layer.
                Pure Fabrication (GRASP) — no domain logic, only data access.

gateway/        Adapters for all external APIs.
                Implements the Adapter structural pattern.

factory/        Object creation logic.
                Implements the Factory Method creational pattern.

entity/         Plain Java domain model objects (POJOs).
                No framework annotations — clean domain layer.

dto/            Data Transfer Objects for request and response payloads.
                Decouples the API contract from the domain model.

security/       JWT authentication filter and user details service.
                Implements Template Method via OncePerRequestFilter.

config/         Spring bean configuration, property binding, and app startup.

exception/      Centralised error handling via GlobalExceptionHandler.
```

---

## 3. Features

### Core Features

**Flight Search**
Users can search for available flights between any two airports using IATA airport codes. The system supports both domestic Indian routes and major international routes. Search results display real airline flight numbers, departure and arrival times, flight duration, airline name, and base fare.

**Seat Selection**
An interactive seat map is presented for each selected flight. Seats are categorised into Business Class and Economy Class, each with distinct pricing. The system displays real-time seat availability — seats locked or booked by other users are shown as unavailable. Users may select multiple seats in a single booking.

**Booking Management**
Upon confirming a booking, the system creates a booking record in Firestore, locks the selected seats, and generates a unique booking reference. Users can view all their past and current bookings through the My Bookings page, with full status visibility including booking status and payment status.

**Payment Processing**
The payment page supports both Credit/Debit Card and UPI payment methods. Payment requests are routed through the backend to the Razorpay payment gateway adapter. In demo mode, any non-blank payment token is accepted, allowing full end-to-end testing without live credentials. When real Razorpay keys are configured, the backend performs server-side payment verification.

### Additional Features

**Passenger Details Management**
Between booking confirmation and payment, users are required to enter travel document details for each passenger. This includes first and last name, date of birth, nationality, passport number and expiry, contact email and phone number, and meal preference. Details are stored in a Firestore sub-collection under the booking document.

**Loyalty Programme**
Every registered user is automatically enrolled in the ARS Loyalty Programme upon registration. Users earn points for every completed booking at a rate of 1 point per INR 10 spent. Points can be redeemed for discounts on future bookings at a rate of 10 points per INR 1. The programme features four tiers — Bronze, Silver, Gold, and Platinum — based on lifetime points accumulated.

**Flight Tracking**
Users can track any flight by entering its IATA flight number. The system first attempts to retrieve live status data from the Aviationstack API. If live data is unavailable (for example, for future scheduled flights), the system derives a meaningful status from the scheduled departure and arrival times stored in Firestore. Possible derived statuses include Scheduled, Check-in Open, Boarding, In Flight (with percentage progress), Landed, and Arrived.

**AI Trip Suggestions**
The system integrates with the Google Gemini API to provide personalised trip destination suggestions. Users input their origin airport, total budget, travel interests, and trip duration. Gemini is prompted with the exact list of destinations reachable through the system's available routes, ensuring all suggestions correspond to bookable flights. The response is validated server-side to filter out any destinations not in the available route table.

---

## 4. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend Framework | React | 19 |
| Frontend Build Tool | Vite | 8 |
| Frontend Routing | React Router | v7 |
| HTTP Client | Axios | 1.15 |
| Backend Framework | Spring Boot | 3.3.2 |
| Backend Language | Java | 17 |
| Security | Spring Security + JJWT | 0.12.6 |
| Password Hashing | BCrypt | Spring Security built-in |
| Database | Firebase Firestore | Admin SDK 9.2.0 |
| Build Tool | Maven | 3.x |
| Node Runtime | Node.js | 18+ |

---

## 5. External API Integrations

### Aviationstack
- **Purpose:** Live flight status and tracking
- **Plan:** Free tier (500 requests per month)
- **Usage:** Called server-side only from `AviationstackClient.java`. The frontend calls `/api/tracking/{flightNumber}` on the backend, which in turn calls Aviationstack. The API key is stored in `application.yml` and never sent to the browser.
- **Endpoint used:** `GET /v1/flights?flight_iata={number}&access_key={key}`

### Google Gemini
- **Purpose:** AI-powered personalised trip destination suggestions
- **Plan:** Free tier
- **Usage:** Called from `GeminiService.java`. The prompt explicitly constrains Gemini to only suggest destinations available in the system's route table. Responses are validated server-side before being returned to the frontend.
- **Endpoint used:** `POST /v1beta/models/gemini-pro:generateContent`

### Razorpay
- **Purpose:** Payment gateway for card and UPI transactions
- **Plan:** Demo mode (no live credentials required for testing)
- **Usage:** Abstracted behind the `PaymentGatewayAdapter` interface, implemented by `RazorpayGatewayAdapter`. In demo mode, any non-blank payment token is accepted. Production verification logic is included as commented code.

### AeroDataBox and Amadeus (Attempted Integrations)

Two external APIs were evaluated for providing real scheduled flight data for the search feature:

**AeroDataBox (via RapidAPI)**
A complete implementation was developed targeting the `/flights/airports/iata/{iataCode}/{from}/{to}` endpoint. This API returns real departure data for any airport. However, the free RapidAPI plan restricts requests to a window of plus or minus 12 hours from the current time. Any request for a future date returns HTTP 400. A paid subscription is required for full schedule access.

**Amadeus Flight Offers Search API**
A complete OAuth2 token acquisition and flight offers search implementation was developed targeting the `/v2/shopping/flight-offers` endpoint. The free test environment functions correctly but returns synthetic mock data for a limited set of airports only. Production access with real data requires application approval and a paid subscription.

Both complete implementations are preserved as commented code in `AeroDataBoxClient.java` with detailed inline documentation explaining the request format, response structure, and the exact configuration changes required to activate them. The current implementation uses a curated static route table containing real IATA flight numbers and accurate schedules sourced from publicly available airline timetables, which provides full end-to-end functionality without API cost constraints.

---

## 6. Object-Oriented Design

### GRASP Principles

**Information Expert**
The `LoyaltyAccount` entity owns all tier calculation and point arithmetic logic. Methods such as `addPoints()`, `redeemPoints()`, and `recalculateTier()` are defined directly on the entity because it holds all the information required to perform these operations.

**Creator**
`DefaultBookingFactory` is responsible for creating `Booking` instances. `DefaultPaymentFactory` is responsible for creating `Payment` instances. Both implement their respective factory interfaces, centralising object creation and ensuring consistent initialisation logic.

**Controller**
All `*Controller` classes act purely as HTTP routing delegates. They extract request parameters, call the appropriate service method, and return the response. No business logic exists in any controller class.

**Pure Fabrication**
All `*Repository` classes are pure fabrications — they do not represent any concept in the problem domain. Their sole responsibility is Firestore data access, including serialisation to `Map<String, Object>` for writes and deserialisation from `DocumentSnapshot` for reads.

**Low Coupling and High Cohesion**
Service classes depend on repository interfaces and gateway interfaces rather than on Firestore or external API clients directly. This allows the persistence and integration layers to be swapped without modifying business logic.

### Design Patterns

**Factory Method (Creational)**
`BookingFactory` and `PaymentFactory` are interfaces defining the creation contract. `DefaultBookingFactory` and `DefaultPaymentFactory` provide the concrete implementations. New booking or payment types can be introduced by adding new implementations without modifying existing code, satisfying the Open/Closed Principle.

**Adapter (Structural)**
`PaymentGatewayAdapter` is an interface that abstracts the payment provider. `RazorpayGatewayAdapter` adapts the Razorpay API to this interface. Similarly, `FlightTrackingClient` is an interface implemented by `AviationstackClient`, adapting the Aviationstack REST API to the system's internal tracking contract.

**Strategy (Behavioral)**
`PaymentService` depends on the `PaymentGatewayAdapter` interface rather than any concrete implementation. The payment provider can be swapped at runtime or configuration time without any changes to `PaymentService`, demonstrating the Strategy pattern.

**Template Method (Behavioral)**
`JwtAuthenticationFilter` extends Spring Security's `OncePerRequestFilter` abstract class and overrides the `doFilterInternal` method. The parent class defines the template — ensuring the filter runs exactly once per request — while the subclass provides the JWT-specific authentication logic.

### SOLID Principles

| Principle | Application |
|---|---|
| Single Responsibility | Each service class has exactly one business responsibility. `LoyaltyService` handles only loyalty logic. `SeatService` handles only seat retrieval. |
| Open/Closed | New payment providers are added by implementing `PaymentGatewayAdapter`. No existing code requires modification. |
| Liskov Substitution | `RazorpayGatewayAdapter` is fully substitutable for `PaymentGatewayAdapter` without altering the correctness of `PaymentService`. |
| Interface Segregation | Gateway interfaces (`PaymentGatewayAdapter`, `FlightTrackingClient`) are narrow and focused, defining only the methods required by their consumers. |
| Dependency Inversion | `PaymentService` depends on the `PaymentGatewayAdapter` abstraction. `FlightTrackingService` depends on the `FlightTrackingClient` abstraction. Neither depends on concrete implementations. |

---

## 7. Project Structure

```
output/
|-- backend/
|   |-- pom.xml
|   `-- src/main/
|       |-- java/com/airline/reservation/
|       |   |-- AirlineReservationApplication.java
|       |   |-- config/
|       |   |   |-- AppProperties.java
|       |   |   |-- DataInitializer.java
|       |   |   |-- FirestoreConfig.java
|       |   |   |-- SecurityConfig.java
|       |   |   `-- WebConfig.java
|       |   |-- controller/
|       |   |   |-- AuthController.java
|       |   |   |-- BookingController.java
|       |   |   |-- FlightController.java
|       |   |   |-- FlightTrackingController.java
|       |   |   |-- LoyaltyController.java
|       |   |   |-- PassengerDetailsController.java
|       |   |   |-- PaymentController.java
|       |   |   |-- SeatController.java
|       |   |   `-- TripSuggestionController.java
|       |   |-- dto/
|       |   |   |-- auth/        AuthResponse, LoginRequest, RegisterRequest
|       |   |   |-- booking/     BookingResponse, CreateBookingRequest
|       |   |   |-- flight/      FlightResponse, FlightSearchRequest
|       |   |   |-- payment/     PaymentRequest, PaymentResponse
|       |   |   |-- seat/        SeatResponse
|       |   |   `-- tracking/    FlightTrackingResponse
|       |   |-- entity/
|       |   |   |-- Booking.java, BookingStatus.java
|       |   |   |-- Flight.java
|       |   |   |-- LoyaltyAccount.java, LoyaltyTransaction.java
|       |   |   |-- PassengerDetails.java
|       |   |   |-- Payment.java, PaymentStatus.java
|       |   |   |-- Role.java
|       |   |   |-- Seat.java, SeatClass.java, SeatStatus.java
|       |   |   `-- User.java
|       |   |-- exception/
|       |   |   |-- ApiErrorResponse.java
|       |   |   |-- BadRequestException.java
|       |   |   |-- GlobalExceptionHandler.java
|       |   |   |-- ResourceNotFoundException.java
|       |   |   `-- UnauthorizedException.java
|       |   |-- factory/
|       |   |   |-- BookingFactory.java
|       |   |   |-- DefaultBookingFactory.java
|       |   |   |-- DefaultPaymentFactory.java
|       |   |   `-- PaymentFactory.java
|       |   |-- gateway/
|       |   |   |-- AeroDataBoxClient.java   (includes commented AeroDataBox + Amadeus implementations)
|       |   |   |-- AviationstackClient.java
|       |   |   |-- FlightTrackingClient.java
|       |   |   |-- PaymentGatewayAdapter.java
|       |   |   `-- RazorpayGatewayAdapter.java
|       |   |-- repository/
|       |   |   |-- BookingRepository.java
|       |   |   |-- FlightRepository.java
|       |   |   |-- LoyaltyAccountRepository.java
|       |   |   |-- LoyaltyTransactionRepository.java
|       |   |   |-- PassengerDetailsRepository.java
|       |   |   |-- PaymentRepository.java
|       |   |   |-- SeatRepository.java
|       |   |   `-- UserRepository.java
|       |   |-- security/
|       |   |   |-- AppUserDetailsService.java
|       |   |   |-- JwtAuthenticationFilter.java
|       |   |   `-- JwtService.java
|       |   `-- service/
|       |       |-- AuthService.java
|       |       |-- BookingService.java
|       |       |-- FlightService.java
|       |       |-- FlightTrackingService.java
|       |       |-- GeminiService.java
|       |       |-- LoyaltyService.java
|       |       |-- PassengerDetailsService.java
|       |       |-- PaymentService.java
|       |       `-- SeatService.java
|       `-- resources/
|           |-- application.yml
|           `-- firebase-adminsdk.json
`-- frontend/
    |-- index.html
    |-- vite.config.js
    |-- package.json
    |-- .env.local
    `-- src/
        |-- main.jsx
        |-- App.jsx
        |-- index.css
        |-- components/
        |   |-- Navbar.jsx
        |   `-- ProtectedRoute.jsx
        |-- context/
        |   |-- AuthContext.jsx
        |   `-- BookingContext.jsx
        |-- firebase/
        |   `-- firebaseConfig.js
        |-- pages/
        |   |-- LoginPage.jsx
        |   |-- RegisterPage.jsx
        |   |-- HomePage.jsx
        |   |-- SeatPage.jsx
        |   |-- ConfirmPage.jsx
        |   |-- PassengerFormPage.jsx
        |   |-- PaymentPage.jsx
        |   |-- SuccessPage.jsx
        |   |-- MyBookingsPage.jsx
        |   |-- TrackPage.jsx
        |   `-- LoyaltyPage.jsx
        `-- services/
            `-- api.js
```

---

## 8. Team Contributions

| Member | Use Case Owned | Design Principle | Design Pattern |
| --- | --- | --- | --- |
| **Rohan** | Flight Search, Seat Selection & Booking + Entire Frontend + Database Layer | Pure Fabrication (GRASP) — `FlightRepository`, `SeatRepository`, and `BookingRepository` are invented purely to handle Firestore persistence, representing no real-world airline concept, keeping persistence completely separate from business logic | Factory Method (Creational) — `BookingFactory` interface with `DefaultBookingFactory` centralises `Booking` object creation with reference generation, PENDING status, and timestamp in one place |
| **Aditya** | Payment Processing & External API Integration | Low Coupling (GRASP) — `PaymentService` depends entirely on the `PaymentGatewayAdapter` interface, never on `RazorpayGatewayAdapter` directly. Swapping payment providers requires zero changes to existing service logic | **Adapter (Structural)** — `RazorpayGatewayAdapter` adapts Razorpay SDK to `PaymentGatewayAdapter`; `AviationstackClient` adapts Aviationstack REST API to `FlightTrackingClient` **Strategy (Behavioral)** — `PaymentService` holds a `PaymentGatewayAdapter` reference injected at runtime, provider fully swappable **Factory Method (Creational)** — `PaymentFactory` with `DefaultPaymentFactory` centralises `Payment` object construction |
| **Rithvik** | User Authentication & Security | Controller (GRASP) — `AuthController` and all 9 controllers are pure routing delegates. They extract request data, call one service method, and return the result with zero business logic in the controller layer | Template Method (Behavioral) — `JwtAuthenticationFilter` extends Spring's `OncePerRequestFilter`. The parent defines the template (run exactly once per request); the subclass fills in the JWT-specific authentication step |
| **Member 4** | Loyalty Programme, Flight Tracking & AI Trip Suggestions + UML Diagrams | Information Expert (GRASP) — `LoyaltyAccount` owns `currentPoints`, `lifetimePoints`, and `tier`, and therefore owns all logic operating on that data — `addPoints()`, `redeemPoints()`, `recalculateTier()`. No external class computes loyalty state | — |

---

## 9. Setup and Installation

### Prerequisites
 
Make sure the following are installed on your machine before proceeding:
 
| Tool | Version | Download |
| --- | --- | --- |
| Java JDK | 17 or higher | https://adoptium.net |
| Apache Maven | 3.x | https://maven.apache.org/download.cgi |
| Node.js + npm | 18 or higher | https://nodejs.org |
| Git | Any | https://git-scm.com |
 
---
 
### Step 1 — Clone the Repository
 
```bash
git clone https://github.com/OOAD-Airline-Reservation-system/Airline-Reservation.git
cd Airline-Reservation
```
 
---
 
### Step 2 — Set Up Firebase
 
Firebase Firestore is the primary database for this project. All users, flights, bookings, seats, payments, and loyalty data are stored here.
 
#### 2a. Create a Firebase Project
 
1. Go to [https://console.firebase.google.com](https://console.firebase.google.com) and click **Add project**.
2. Give your project a name (e.g. `airline-reservation-ars`) and click through to create it.
3. Once created, navigate to **Build → Firestore Database** in the left sidebar.
4. Click **Create database**, choose **Start in production mode** (the backend handles all writes), and select any region (e.g. `asia-south1` for India).
#### 2b. Generate a Service Account Key (Backend)
 
1. In the Firebase Console, go to **Project Settings** (gear icon) → **Service accounts**.
2. Click **Generate new private key** and confirm. A JSON file will be downloaded — this is your `firebase-adminsdk.json`.
3. Copy this file into `backend/src/main/resources/` and rename it `firebase-adminsdk.json`.
> **Security:** This file contains a private key. Never commit it to version control. It is already listed in `.gitignore`.
 
#### 2c. Get the Web SDK Config (Frontend)
 
1. In **Project Settings** → **General**, scroll to **Your apps** and click **Add app → Web**.
2. Register the app (any nickname). Firebase will show you a config object like this:
```javascript
const firebaseConfig = {
  apiKey: "AIza...",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "1234567890",
  appId: "1:1234567890:web:abc123"
};
```
 
3. Open `frontend/src/firebase/firebaseConfig.js` and paste these values in.
---
 
### Step 3 — Obtain API Keys
 
#### Aviationstack (Flight Tracking)
 
- Sign up at [https://aviationstack.com](https://aviationstack.com).
- The free plan provides **500 requests/month**, which is sufficient for development.
- After registration, copy your **Access Key** from the dashboard.
#### Google Gemini (AI Trip Suggestions)
 
- Go to [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) and sign in with a Google account.
- Click **Create API key** and copy it. The free tier has generous quota for development use.
#### Razorpay (Payment Gateway — Optional)
 
- The system runs in **demo mode by default**. No Razorpay credentials are required to test the full payment flow — any non-blank payment token is accepted.
- If you want to enable real payment verification, sign up at [https://razorpay.com](https://razorpay.com), go to **Settings → API Keys**, generate a key pair, and fill in `key-id` and `key-secret` in `application.yml`.
---
 
### Step 4 — Configure the Backend
 
Open `backend/src/main/resources/application.yml` and fill in all values:
 
```yaml
app:
  jwt:
    # Generate a secure random Base64-encoded 256-bit secret, e.g. via:
    #   openssl rand -base64 32
    secret: "your-base64-encoded-256-bit-secret"
    expiration-ms: 3600000   # 1 hour; adjust as needed
 
  firebase:
    service-account-key: "classpath:firebase-adminsdk.json"
    project-id: "your-firebase-project-id"   # From Firebase Console → Project Settings
 
  aviationstack:
    api-key: "your-aviationstack-access-key"
    base-url: "http://api.aviationstack.com/v1"
 
  gemini:
    api-key: "your-gemini-api-key"
 
  razorpay:
    key-id: ""       # Leave blank for demo mode
    key-secret: ""   # Leave blank for demo mode
 
  aerodatabox:
    api-key: ""      # Optional — only required if enabling AeroDataBox (see Section 5)
    api-host: "aerodatabox.p.rapidapi.com"
```
 
> **Tip:** To generate a JWT secret, run `openssl rand -base64 32` in your terminal and paste the output as the value.
 
---
 
### Step 5 — Start the Backend
 
```bash
cd backend
mvn spring-boot:run
```
 
Maven will download all dependencies on the first run. Once you see a log line like:
 
```
Started AirlineReservationApplication in X.XXX seconds
```
 
the backend is running at **http://localhost:8080**. On first startup, `DataInitializer` seeds the Firestore database with the static flight route table automatically.
 
---
 
### Step 6 — Configure and Start the Frontend
 
#### Install Dependencies
 
```bash
cd ../frontend
npm install
```
 
#### Environment File
 
Create a file called `.env.local` inside the `frontend/` directory with the following content. These values are only used for the Firebase client SDK (for any direct Firestore operations from the browser, if applicable):
 
```env
VITE_FIREBASE_API_KEY=your-firebase-api-key
VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your-firebase-project-id
VITE_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=your-sender-id
VITE_FIREBASE_APP_ID=your-app-id
```
 
These values come from the Firebase Web SDK config object you obtained in Step 2c.
 
#### Start the Dev Server
 
```bash
npm run dev
```
 
The frontend will start at **http://localhost:5173**.
 
The `vite.config.js` file already contains a proxy rule that forwards all `/api/*` and `/auth/*` requests to `http://localhost:8080`, so no CORS configuration is needed during development.
 
---
 
### Step 7 — Verify Everything is Working
 
1. Open [http://localhost:5173](http://localhost:5173) in your browser.
2. Click **Register** and create a new account — this creates a user document in Firestore and auto-enrols the user in the loyalty programme.
3. Log in and use the flight search with any route from the [Available Routes](#13-available-routes) table (e.g. `DEL → BOM`, date: any future date).
4. Complete a booking end-to-end: search → seat selection → confirm → passenger details → payment (enter any non-blank value as the payment token in demo mode) → success.
5. Check **My Bookings** and **Loyalty** pages to confirm data was persisted correctly.
---
 
### Troubleshooting
 
| Issue | Likely Cause | Fix |
| --- | --- | --- |
| Backend fails to start with Firebase error | `firebase-adminsdk.json` missing or wrong project ID in `application.yml` | Verify the file is in `src/main/resources/` and the `project-id` field matches your Firebase project |
| `401 Unauthorized` on all API calls | JWT secret not set or token expired | Set a valid secret in `application.yml`; re-login to get a fresh token |
| Flight tracking returns no data | Aviationstack key missing or free quota exhausted | Add your API key to `application.yml`; the system will fall back to schedule-derived status automatically |
| Gemini suggestions fail | Gemini API key missing or quota exceeded | Add your key to `application.yml`; check quota at [aistudio.google.com](https://aistudio.google.com) |
| Frontend cannot reach backend | Backend not running or wrong port | Ensure backend is running on port 8080 before starting the frontend |
| `npm install` fails | Node.js version below 18 | Upgrade Node.js at https://nodejs.org |
 
---

---

## 10. API Reference

### Authentication Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/auth/register` | Register a new user and auto-enrol in loyalty programme | No |
| POST | `/auth/login` | Authenticate and receive a JWT access token | No |

### Flight and Seat Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| GET | `/api/flights/search` | Search flights by source IATA, destination IATA, and date | Yes |
| GET | `/api/seats/flight/{flightId}` | Retrieve the seat map for a specific flight | Yes |

### Booking Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/bookings` | Create a new booking and lock selected seats | Yes |
| GET | `/api/bookings/me` | Retrieve all bookings for the authenticated user | Yes |
| GET | `/api/bookings/{id}` | Retrieve a specific booking by ID | Yes |
| POST | `/api/bookings/{id}/passengers` | Save passenger details for a booking | Yes |
| GET | `/api/bookings/{id}/passengers` | Retrieve passenger details for a booking | Yes |

### Payment Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/api/payments` | Process payment for a booking and award loyalty points | Yes |

### Loyalty Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| GET | `/api/loyalty/me` | Retrieve loyalty account, tier, balance, and transaction history | Yes |
| POST | `/api/loyalty/redeem` | Redeem loyalty points for a discount on a booking | Yes |

### Tracking and AI Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| GET | `/api/tracking/{flightNumber}` | Retrieve live or schedule-derived flight status | Yes |
| GET | `/api/trips/suggest` | Generate AI trip suggestions via Gemini | Yes |

#### Example Requests

```
GET /api/flights/search?source=DEL&destination=BOM&date=2025-05-10

GET /api/tracking/AI860

GET /api/trips/suggest?from=DEL&budget=20000&interests=beaches,culture&duration=5
```

---

## 11. Booking Workflow

The complete end-to-end booking process follows these steps:

```
Step 1 — Flight Search (HomePage)
         User enters origin and destination IATA codes and selects a travel date.
         The backend queries the route table and returns matching flights.

Step 2 — Seat Selection (SeatPage)
         User views the interactive seat map for the selected flight.
         Available, locked, and booked seats are displayed with distinct styling.
         User selects one or more seats.

Step 3 — Booking Confirmation (ConfirmPage)
         User reviews the flight details, selected seats, and price breakdown.
         On confirmation, the backend creates a booking record in Firestore,
         locks the selected seats, and returns a booking reference.

Step 4 — Passenger Details (PassengerFormPage)
         User enters travel document details for each passenger including
         passport information, contact details, and meal preference.
         Details are saved to a Firestore sub-collection under the booking.

Step 5 — Payment (PaymentPage)
         User selects Card or UPI payment method and enters payment details.
         The backend routes the payment through the Razorpay adapter.
         On success, the booking status is updated to CONFIRMED, seats are
         marked as BOOKED, and loyalty points are awarded.

Step 6 — Booking Success (SuccessPage)
         The confirmed booking reference is displayed to the user.

Step 7 — Flight Tracking (TrackPage)
         At any time, the user can track a flight by its IATA number.
         Live data is retrieved from Aviationstack where available.
         For future flights, status is derived from scheduled times.

Step 8 — Loyalty Dashboard (LoyaltyPage)
         User can view their tier status, points balance, transaction history,
         and redeem points for discounts on future bookings.
```

---

## 12. Loyalty Programme

The ARS Loyalty Programme automatically enrols every registered user. Points are earned on every completed payment and can be redeemed for discounts on future bookings.

| Tier | Lifetime Points Required | Benefits |
|---|---|---|
| Bronze | 0 to 4,999 | 1 point per INR 10 spent |
| Silver | 5,000 to 19,999 | 10% bonus points, priority check-in |
| Gold | 20,000 to 49,999 | 20% bonus points, lounge access |
| Platinum | 50,000 and above | 30% bonus points, unlimited lounge access, upgrade eligibility |

**Earning rate:** 1 point per INR 10 spent on completed bookings.
**Redemption rate:** 10 points = INR 1 discount on a booking.
**Tier progression:** Based on lifetime points accumulated, which never decrease.

---

## 13. Available Routes

The following routes are available for search and booking. All routes operate in both directions unless indicated otherwise.

### Domestic Routes

| Origin | Destination | Airlines |
|---|---|---|
| DEL | BOM | Air India, IndiGo, SpiceJet, Vistara |
| DEL | BLR | Air India, IndiGo, Vistara |
| DEL | MAA | Air India, IndiGo |
| DEL | HYD | Air India, IndiGo |
| DEL | CCU | Air India, IndiGo |
| BOM | BLR | Air India, IndiGo |
| BOM | MAA | Air India, IndiGo |
| BOM | GOI | Air India, IndiGo |

### International Routes

| Origin | Destination | Airlines |
|---|---|---|
| DEL, BOM | DXB | Emirates, Air India |
| DEL, BOM | LHR | British Airways, Air India |
| DEL, BOM | SIN | Singapore Airlines, Air India |
| DEL | JFK | Air India, United Airlines |
| DEL | CDG | Air France, Air India |
| DEL | FRA | Lufthansa, Air India |
| DEL | NRT | Japan Airlines, Air India |
| BOM | HKG | Cathay Pacific, Air India |
| DEL | SYD | Qantas, Air India |
| BOM | JFK | Air India |
