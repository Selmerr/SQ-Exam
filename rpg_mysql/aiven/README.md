Run these scripts against the Aiven `defaultdb` database in this order:

1. `01_create_schema_aiven.sql`
2. `03_procedures_aiven.sql`
3. `04_functions_aiven.sql`
4. `05_triggers_aiven.sql`
5. `06_seed_data_aiven.sql`
6. `06_security_roles_aiven.sql`
7. `06_events_aiven.sql` (optional; may fail on managed privileges)

These are adapted copies of the Docker init scripts and replace `choose_your_fate`
with `defaultdb` because Aiven free tier uses `defaultdb`.
