FROM openjdk:22-slim-bullseye

WORKDIR /app
RUN mkdir -p src/de/
COPY server.sh .
CMD ["./server.sh"]
COPY ./src/de/ ./src/de/
RUN javac -Xlint:unchecked -cp './src' -d bin $(find src/ -name *.java)
