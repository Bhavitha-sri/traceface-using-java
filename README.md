# TraceFace (Java Servlet/JSP)

AI face matching + public web discovery + blockchain verification.

## How it works
1. Upload a face photo.
2. OpenCV YuNet detects the face; SFace creates a 128-number embedding.
3. SerpApi (Google Lens) finds similar public web images.
4. Faces in those images are compared with cosine similarity (match >= 0.363).
5. An evidence record is created and its SHA-256 hash is written to Ethereum Sepolia.
6. The hash is read back from the chain to verify it.

## Tech stack
Java 17+, Servlet/JSP/JSTL, Maven, Tomcat 9, OpenCV, web3j, Gson, SerpApi, Ethereum Sepolia.

## Run locally
1. In TraceFaceHome, copy .env.example to .env and fill in your keys.
2. cd TraceFace-source, then: mvn clean package
3. Copy target/TraceFace.war to Tomcat 9 webapps.
4. Start Tomcat with -Dtraceface.home=<path to TraceFaceHome>
5. Open http://localhost:8080/TraceFace/

Never commit .env. Use a throw-away Sepolia wallet. Use only photos you have consent to use.
