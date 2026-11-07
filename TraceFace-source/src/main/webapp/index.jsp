<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TraceFace</title>
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body>
<div class="container">

    <jsp:include page="header.jsp"/>

    <form action="<c:url value='/analyze'/>" method="POST" enctype="multipart/form-data">
        <div class="dropzone">
            <div class="dropzone-icon"></div>
            <label for="photo">Select a face image</label>
        </div>
        <input type="file" id="photo" name="photo" accept="image/*" required>
        <button type="submit">Analyze Face</button>
    </form>

</div>
</body>
</html>
