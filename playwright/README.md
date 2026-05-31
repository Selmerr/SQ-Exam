Before running the project you need to run these two commands

To run the commands you need to install node.js

Then run:
npm i

and then run

npm i playwright


then to run the test you need the docker compose file launched from the source code

the backend project running
the frontend project running

then run:

npx playwright test

This will run both the e2e tests and the api tests 

e2e tests are found in the e2e folder

api tests are found in the endpoint folder.