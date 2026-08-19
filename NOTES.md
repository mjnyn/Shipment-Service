# Notes

## Time Spent
Built over several evening sessions from 17-08-2026 - 20-08-2026. Focused implementation time was approximately 6 hours.

## Clarifications and Limitations
- For `INVALID` responses, I treat `eventId` and `shipmentId` as the identifying fields to echo. Unknown extra fields in the JSON are not echoed.
- Parse failures, such as invalid enum values or malformed timestamps, return `INVALID` with a reason but do not echo identifiers because the request DTO could not be constructed.
- Concurrent duplicate inserts are guarded by the database primary key and handled as duplicates, but I did not add a dedicated concurrent test.

## If I Had More Time
- Add a dedicated concurrent duplicate ingestion test.