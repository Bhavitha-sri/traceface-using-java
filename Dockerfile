FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY TraceFace-source/pom.xml .
RUN mvn -q dependency:go-offline
COPY TraceFace-source/src ./src
RUN mvn -q clean package -DskipTests

FROM tomcat:9.0-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/TraceFace.war /usr/local/tomcat/webapps/ROOT.war
COPY TraceFaceHome /opt/traceface
COPY start.sh /start.sh
RUN sed -i 's/\r$//' /start.sh && chmod +x /start.sh
ENV TRACEFACE_HOME=/opt/traceface
ENV CATALINA_OPTS="-Xmx350m"
CMD ["/start.sh"]
