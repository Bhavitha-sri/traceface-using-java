# TraceFace (Java Servlet / JSP version)

Java conversion of the Python/Flask TraceFace project.

| Python (Flask)                | Java (Servlet/JSP)                         |
|-------------------------------|--------------------------------------------|
| app.py `/analyze`             | `servlet/AnalyzeServlet`                   |
| app.py face functions         | `service/FaceService`, `WebFaceMatcher`    |
| app.py `download_web_image`   | `service/ImageDownloader`                  |
| web_search.py                 | `service/WebSearchService`                 |
| evidence.py                   | `service/EvidenceService`                  |
| blockchain/blockchain.py      | `service/BlockchainService` (web3j)        |
| templates/*.html (Jinja2)     | `index.jsp`, `result.jsp`, `header.jsp` (JSTL) |
| static/style.css              | `css/style.css`                            |
| .env                          | `.env` read by `config/AppConfig`          |

## Requirements
* JDK 17+, Maven 3.8+, **Apache Tomcat 9** (javax.* servlet API)
* The two ONNX models from your Python project:
  `face_detection_yunet.onnx` and `face_recognition_sface.onnx`

## Setup
1. Create a TraceFace home folder, e.g. `C:\TraceFaceHome`, containing
   `models/` (the two .onnx files) and a `.env` (see `.env.example`).
2. Tell Tomcat where it is: add `-Dtraceface.home=C:\TraceFaceHome`
   to `CATALINA_OPTS` (or set the `TRACEFACE_HOME` environment variable).
3. `mvn clean package` -> copy `target/TraceFace.war` to Tomcat `webapps/`.
4. Open http://localhost:8080/TraceFace/

`uploads/` and `data/` (web_results.json, evidence_records.json) are created automatically.

## Security notes
* Keep `.env` out of Git and out of any zip you share. Rotate keys that were shared before.
* Use a throw-away Sepolia wallet that holds only test ETH.
