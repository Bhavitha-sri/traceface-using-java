# TraceFace (Java Servlet/JSP)

**AI face matching + public web discovery + blockchain verification**

TraceFace is a Java web application. A user uploads a face photo, and the app detects the face with AI, finds similar public images on the web, compares the faces, and stores a tamper-proof SHA-256 fingerprint of the evidence on the Ethereum Sepolia test blockchain. The fingerprint can then be read back from the chain to verify that the record was not changed.

**Live demo:** https://traceface-using-java.onrender.com/
> Hosted on a free Render plan, so the first load after inactivity can take 50 seconds or more. The blockchain step can take up to 3 minutes.

---

## Screenshots

| Upload page | Choose a photo |
|---|---|
| ![Upload page](screenshots/01-upload.png) | ![Choose a photo](screenshots/02-choose-file.png) |

| Face detected and best match | Evidence record |
|---|---|
| ![Match result](screenshots/03-match.png) | ![Evidence record](screenshots/04-evidence.png) |

| Blockchain verification | Face comparisons |
|---|---|
| ![Blockchain verification](screenshots/05-blockchain.png) | ![Comparisons](screenshots/06-comparisons.png) |

---

## How it works

1. The user uploads a face photo.
2. **OpenCV YuNet** detects the face and **SFace** turns it into a 128-number embedding.
3. **SerpApi (Google Lens)** finds similar public web images.
4. Faces in those images are compared with **cosine similarity** (a match is a score of 0.363 or higher).
5. An **evidence record** is created and its **SHA-256** fingerprint is calculated.
6. The fingerprint is written to an **Ethereum Sepolia** transaction using web3j.
7. The fingerprint is read back from the chain and compared, which gives the "Verification Success" result.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17+ |
| Web | Servlets, JSP, JSTL (MVC) |
| Build / Server | Maven, Apache Tomcat 9 |
| Face AI | OpenCV 4.9, YuNet (detection), SFace (recognition) |
| Web search | SerpApi, Google Lens |
| Blockchain | Ethereum Sepolia, web3j, Alchemy RPC |
| Data | JSON files (Gson), SHA-256 |
| Deployment | Docker, Render |

## Project structure

```
TraceFace-source/        Maven project
  src/main/java/com/traceface/
    servlet/             AnalyzeServlet (controller)
    listener/            AppContextListener (loads OpenCV + models)
    service/             Face, WebSearch, WebFaceMatcher, Evidence, Blockchain
    model/               Data classes
    config/              AppConfig (reads keys from .env / environment)
  src/main/webapp/       index.jsp, result.jsp, header.jsp, css/style.css
TraceFaceHome/           models (.onnx), data/, uploads/, .env.example
Dockerfile, start.sh     Render deployment
```

## Run locally

**Requirements:** JDK 17+, Maven 3.8+, Apache Tomcat 9 (not 10 or 11).

1. In `TraceFaceHome`, copy `.env.example` to `.env` and fill in:
   ```
   SERPAPI_API_KEY=your_key
   SEPOLIA_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/your_key
   SEPOLIA_PRIVATE_KEY=your_test_wallet_key
   SEPOLIA_CHAIN_ID=11155111
   ```
2. Build the WAR:
   ```
   cd TraceFace-source
   mvn clean package
   ```
3. Copy `target/TraceFace.war` into Tomcat 9's `webapps` folder.
4. Start Tomcat with `-Dtraceface.home=<full path to TraceFaceHome>`.
5. Open http://localhost:8080/TraceFace/

## Deploy on Render

The repository includes a `Dockerfile` and `start.sh`. On Render, create a **Web Service** from this repo, choose the **Docker** language, and add the four environment variables above. Do not upload a `.env` file.

## Security and ethics

- Keep `.env` out of Git. Use a **throw-away Sepolia wallet** that holds only test ETH.
- Use this tool only with photos **you own or have consent to use**.
- A similarity score is a **hint, not proof of identity**. The blockchain proves only that a record existed unchanged at a point in time. It does not prove that the match is true.
- Uploaded photos and results are stored temporarily on the server and are not meant to be kept long term.

## Limitations

- Depends on SerpApi and Google Lens results and quota.
- Uses the Sepolia test network, not a production blockchain.
- No user login and no database (results are stored in JSON files).
- Free hosting uses temporary disk and sleeps when idle.

## Future work

- User login and a database for evidence history.
- A page to verify any stored transaction by hash.
- Deployment on a low-cost layer-2 network.

## Author

Bhavitha Sri
