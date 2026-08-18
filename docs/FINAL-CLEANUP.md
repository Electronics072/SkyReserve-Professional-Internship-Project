# SkyReserve Final Cleanup Notes

## Verified fixes
- Fresh demo data now creates one upcoming day (3 schedules for the 3 seeded flights), rather than 14 days/42 schedules.
- `HomeController` uses `0L` for the seat-availability calculation to avoid Java `int`/`Long` ambiguity.
- `admin.html` uses `100L` and `5L` in `Math.min` to avoid Thymeleaf/SpEL overloaded-method ambiguity.
- Existing H2 data is not deleted automatically; the final demo database should be kept as-is after testing.

## Clean demo database state
The application uses a file-based H2 database at `./data/skyreserve`. The `data/` directory is intentionally ignored by Git so a fresh clone/ZIP does not carry a machine-specific database.

If an old local database contains unwanted schedules, back it up before cleanup. The project should be started once with a clean database to generate the seeded demo data.

## Demo accounts
Passenger: `user@skyreserve.local` / `user123`
Administrator: `admin@skyreserve.local` / `admin123`

Change these credentials before any public deployment.
