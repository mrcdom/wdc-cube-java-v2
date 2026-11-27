# The WDC Store backend, from what Maven and the frontend builds made (./prepare.sh stages it in build/context): the
# backend's fat JAR and the web frontends. No JDK, Node, Flutter or Maven here.
FROM eclipse-temurin:25-jre-noble

# curl for the health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --home-dir /work store

COPY build/context/backend.jar /app/backend.jar
COPY build/context/frontend /work/frontend
COPY build/context/web-cache /work/data/web-cache
COPY logback.xml /work/config/logback.xml
COPY entrypoint.sh /app/entrypoint.sh

RUN mkdir -p /work/data /work/log /work/temp && chown -R store:store /work && chmod +x /app/entrypoint.sh

USER store
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
