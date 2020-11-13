mvn clean package shade:shade
docker build -t namelessmc/discord-link .
docker build -t namelessmc/discord-link-postgres postgres-docker
