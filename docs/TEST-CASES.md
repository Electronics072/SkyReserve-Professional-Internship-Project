# SkyReserve Functional Test Record

| ID | Test | Expected Result |
|---|---|---|
| TC-01 | User login | Authenticated user reaches dashboard/home |
| TC-02 | Flight search | Matching active flights are displayed |
| TC-03 | Booking | Confirmed booking and reference are generated |
| TC-04 | E-ticket/PDF | Ticket is displayed and PDF can be downloaded |
| TC-05 | My Bookings | User sees only their bookings |
| TC-06 | Cancellation | Booking becomes CANCELLED and seat is released |
| TC-07 | Admin login | Admin reaches Control Center |
| TC-08 | Flight activation | Admin can activate/deactivate flights |
| TC-09 | Add flight | New flight is persisted and visible to admin |
| TC-10 | Create schedule | New schedule becomes searchable by users |
| TC-11 | Schedule status | Admin can update schedule status |
| TC-12 | Analytics | Booking/revenue figures reflect database records |
| TC-13 | Duplicate/concurrent seat booking | Only one request can confirm the same seat |
| TC-14 | Authorization | Non-admin users cannot access `/admin/**` |

All major tests above were manually verified during the project build session. Final screenshots/results should be captured for the project report.


## Professional build additions
| TC-15 | Search while logged out | User is sent to Login/Register before flight search continues |
| TC-16 | Multi-route search | Routes such as Visakhapatnam→Chennai, Hyderabad→Delhi, Mumbai→Delhi, Mumbai→Hyderabad and Bengaluru→Mumbai return matching flights |
| TC-17 | Payment gateway | Seat selection leads to simulated payment page before booking confirmation |
| TC-18 | Payment methods | UPI/apps, cards, net banking and wallets can be selected in demo mode |
| TC-19 | Admin validation | Empty/invalid/duplicate flight data is rejected server-side |
| TC-20 | Admin authorization | Direct `/admin` access is denied to non-admin users |
