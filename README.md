# SkyReserve Professional Internship Project — Final Build
## Web-Based Airline Reservation and Management System

SkyReserve Professional Internship Project is a Spring Boot + Thymeleaf + JPA airline reservation application developed as a final-year internship project. The original Swing application supplied with the project is retained under `legacy-source` for reference.

### Main features
- Passenger registration/login with BCrypt password hashing
- Login/signup required before flight search; Spring Security returns the passenger to the requested search after login
- Professional payment-gateway simulation after seat selection (UPI, Google Pay, PhonePe, Paytm, BHIM, cards, net banking and wallets)
- Role-based access control (`USER`, `ADMIN`)
- Flight search by source, destination and date
- Dynamic seat map with occupied-seat highlighting
- Transactional booking and database uniqueness protection
- Pessimistic schedule locking for stronger concurrent booking protection
- My Bookings, cancellation and seat release
- Printable e-ticket and PDF ticket download
- Admin dashboard with flight, schedule, user and booking management
- Server-side admin authorization: only ROLE_ADMIN can access `/admin/**`
- Server-side flight validation: required fields, valid fares/seats, time format, different source/destination and unique flight number
- Revenue and route/flight booking analytics
- H2 file database for zero-setup demonstration
- Optional MySQL profile for deployment/database demonstrations

## Recommended software
- Java 17 or newer
- IntelliJ IDEA
- Maven 3.6.3+
- Chrome/Edge
- MySQL 8.x (optional; H2 is the default)

## Run in IntelliJ
1. Extract the ZIP and open the folder containing `pom.xml`.
2. Set the project SDK to Java 17 or newer.
3. Let IntelliJ import Maven dependencies.
4. Run `SkyReserveApplication`.
5. Open `http://localhost:8080`.

## Run from terminal
```bash
mvn clean package
java -jar target/skyreserve-2.0.0.jar
```

## Demo accounts
Passenger:
- Email: `user@skyreserve.local`
- Password: `user123`

Administrator:
- Email: `admin@skyreserve.local`
- Password: `admin123`

## H2 database
The default profile stores the H2 database under `./data/skyreserve`. No MySQL installation is required for the basic demo.

H2 console (development only): `http://localhost:8080/h2-console`

JDBC URL: `jdbc:h2:file:./data/skyreserve`
User: `sa`
Password: blank

## MySQL
1. Install MySQL and create the database with `database/schema.sql`.
2. Copy `application-mysql.properties` values into your environment or edit the password.
3. Start with the MySQL profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Do not commit real database passwords to Git.

## Booking workflow
Home → Login/Sign up → Search Flights → Select Flight → Passenger details + seat → Secure Demo Payment → E-Ticket → My Bookings → Cancel if required.

## Advanced implementation points
- Layered architecture: Controller → Service → Repository → Database
- Role-based authorization for `/admin/**`
- `@Transactional` booking/cancellation operations
- Pessimistic lock on the flight schedule during seat allocation
- Unique database constraint for confirmed seat allocation
- BCrypt password hashing
- Server-side ownership check for tickets and cancellations

## Final cleanup notes
- A fresh demo database seeds 3 flights and 3 upcoming schedules (one day).
- The checked-in H2 database is intentionally not included in the ZIP; it is created locally under `./data`.
- Keep the development H2 console for local demonstration only. Do not expose it on a public deployment.
- Replace demo passwords before public deployment.

## Project structure
```text
src/main/java/com/skyreserve
├── config
├── controller
├── model
├── repository
├── security
└── service
```

## Original source
The provided legacy Swing project is preserved in `legacy-source`. Its sample destinations, classes and visual assets informed the web redesign.
