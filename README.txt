TraceFace - Java (Servlet/JSP) full project
===========================================
TraceFace-source/   Maven project (src/main/java, src/main/webapp, pom.xml)
TraceFaceHome/      Runtime folder: models/ (YuNet + SFace .onnx already included),
                    data/ and uploads/ (filled by the app), .env.example

Steps
1. In TraceFaceHome, copy .env.example to .env and fill in SERPAPI_API_KEY,
   SEPOLIA_RPC_URL, SEPOLIA_PRIVATE_KEY (throw-away test wallet), SEPOLIA_CHAIN_ID.
   Never share or commit .env.
2. cd TraceFace-source  then  mvn clean package   (JDK 17+, Maven 3.8+; internet needed once)
3. Copy target/TraceFace.war into Apache Tomcat 9 /webapps
4. Start Tomcat with  -Dtraceface.home=<full path to TraceFaceHome>
   (e.g. CATALINA_OPTS=-Dtraceface.home=C:\TraceFace_Java_Full\TraceFaceHome)
5. Open http://localhost:8080/TraceFace/

Use Tomcat 9 (javax.*). Tomcat 10+ needs the jakarta.* namespace.
