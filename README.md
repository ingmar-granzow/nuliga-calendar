# nuliga-calendar

Generate an ICS Calendar file from a team's season schedule in nuLiga.

# Docker Execution

Use the following command to call the script from the Groovy Docker container:

```bash
docker run --rm -v "$PWD":/home/groovy/scripts -w /home/groovy/scripts groovy groovy -Duser.timezone=Europe/Berlin calendar.groovy
```
