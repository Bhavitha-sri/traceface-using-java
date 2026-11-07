<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TraceFace Result</title>
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body>
<div class="container">

    <jsp:include page="header.jsp">
        <jsp:param name="subtitle" value="AI Face Matching + Public Web Discovery + Blockchain Verification"/>
    </jsp:include>

    <c:choose>

        <%-- ================= FAILURE ================= --%>
        <c:when test="${not success}">
            <div class="error">
                <h2>&#10007; Analysis Failed</h2>
                <p><c:out value="${empty message ? 'Unexpected error.' : message}"/></p>
                <c:if test="${not empty webResultsCount}">
                    <p>Web results found: <strong><c:out value="${webResultsCount}"/></strong></p>
                </c:if>
            </div>
        </c:when>

        <%-- ================= SUCCESS ================= --%>
        <c:otherwise>

            <div class="success">
                <h2>&#10003; Face Detected</h2>
                <p><c:out value="${message}"/></p>
                <p>Public web image results: <strong><c:out value="${webResultsCount}"/></strong></p>
            </div>

            <c:choose>
                <c:when test="${matchFound}">

                    <div class="match">
                        <h2>&#10003; Public Web Content Matched</h2>
                        <p><strong>Match ID:</strong> <c:out value="${bestMatch.id}"/></p>
                        <p><strong>Title:</strong> <c:out value="${bestMatch.title}"/></p>
                        <p><strong>Source:</strong> <c:out value="${bestMatch.source}"/></p>
                        <p><strong>Face Similarity:</strong>
                            <fmt:formatNumber value="${bestMatch.similarityPercent}" pattern="0.00"/>%</p>
                        <p><strong>Image Match Type:</strong>
                            <c:out value="${bestMatch.exactMatch ? 'Exact web image match' : 'Visual web match'}"/></p>
                        <p><strong>Public Page:</strong></p>
                        <p><a href="<c:out value='${bestMatch.url}'/>" target="_blank" rel="noopener noreferrer">
                            <c:out value="${bestMatch.url}"/></a></p>
                    </div>

                    <c:if test="${not empty evidenceRecord}">
                        <div class="evidence">
                            <h2>&#128274; Evidence Record</h2>

                            <div class="evidence-row"><strong>Evidence ID:</strong>
                                <span><c:out value="${evidenceRecord.evidence.evidenceId}"/></span></div>
                            <div class="evidence-row"><strong>Uploaded File:</strong>
                                <span><c:out value="${evidenceRecord.evidence.uploadedFile}"/></span></div>
                            <div class="evidence-row"><strong>Matched Web Page:</strong>
                                <span><c:out value="${evidenceRecord.evidence.matchedPostId}"/></span></div>
                            <div class="evidence-row"><strong>Created:</strong>
                                <span><c:out value="${evidenceRecord.evidence.createdAt}"/></span></div>

                            <h3>SHA-256 Fingerprint</h3>
                            <div class="hash-box"><c:out value="${evidenceRecord.sha256}"/></div>

                            <div class="blockchain-section">
                                <h2>&#9939; Blockchain Verification</h2>

                                <div class="blockchain-row"><strong>Status:</strong>
                                    <span><c:out value="${evidenceRecord.blockchain.status}"/></span></div>

                                <c:if test="${not empty evidenceRecord.blockchain.transactionHash}">
                                    <div class="blockchain-row"><strong>Transaction Hash:</strong>
                                        <span class="hash-box"><c:out value="${evidenceRecord.blockchain.transactionHash}"/></span></div>
                                    <p><a target="_blank" rel="noopener noreferrer"
                                          href="https://sepolia.etherscan.io/tx/<c:out value='${evidenceRecord.blockchain.transactionHash}'/>">
                                        View transaction on Sepolia Etherscan</a></p>
                                </c:if>

                                <c:if test="${not empty evidenceRecord.blockchain.blockNumber}">
                                    <div class="blockchain-row"><strong>Block:</strong>
                                        <span><c:out value="${evidenceRecord.blockchain.blockNumber}"/></span></div>
                                </c:if>

                                <c:if test="${not empty evidenceRecord.blockchain.onChainSha256}">
                                    <h3>On-Chain SHA-256</h3>
                                    <div class="hash-box"><c:out value="${evidenceRecord.blockchain.onChainSha256}"/></div>
                                </c:if>

                                <c:choose>
                                    <c:when test="${evidenceRecord.blockchain.verified}">
                                        <div class="verification-success">
                                            <h2>&#9989; VERIFICATION SUCCESS</h2>
                                            <p>The SHA-256 fingerprint read from the blockchain matches the evidence record.</p>
                                        </div>
                                    </c:when>
                                    <c:when test="${evidenceRecord.blockchain.status == 'FAILED'}">
                                        <div class="verification-failed">
                                            <h2>&#10060; BLOCKCHAIN TRANSACTION FAILED</h2>
                                            <c:if test="${not empty evidenceRecord.blockchain.error}">
                                                <p><c:out value="${evidenceRecord.blockchain.error}"/></p>
                                            </c:if>
                                        </div>
                                    </c:when>
                                    <c:when test="${evidenceRecord.blockchain.status == 'VERIFICATION_FAILED'}">
                                        <div class="verification-failed">
                                            <h2>&#10060; VERIFICATION FAILED</h2>
                                        </div>
                                    </c:when>
                                </c:choose>
                            </div>
                        </div>
                    </c:if>

                </c:when>

                <c:otherwise>
                    <div class="error">
                        <h2>&#10007; No Strong Face Match</h2>
                        <p>Public web images were found, but none reached the face-matching threshold.</p>
                    </div>
                </c:otherwise>
            </c:choose>

            <h2 class="section-title">Public Web Face Comparisons</h2>

            <div class="results">
                <c:forEach var="match" items="${matches}" varStatus="loop">
                    <div class="candidate">
                        <c:if test="${match.match}"><span class="badge">MATCH</span></c:if>
                        <h3>${loop.count}. <c:out value="${match.title}"/></h3>
                        <p><strong>Source:</strong> <c:out value="${match.source}"/></p>
                        <p><strong>Face Similarity:</strong>
                            <fmt:formatNumber value="${match.similarityPercent}" pattern="0.00"/>%</p>
                        <c:if test="${match.exactMatch}">
                            <p>Exact image match returned by Lens</p>
                        </c:if>
                        <p><a href="<c:out value='${match.url}'/>" target="_blank" rel="noopener noreferrer">Open public page</a></p>
                    </div>
                </c:forEach>
            </div>

        </c:otherwise>
    </c:choose>

    <a href="<c:url value='/index.jsp'/>" class="back-button">Analyze another image</a>

</div>
</body>
</html>
