<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Shared header, included in every page with <jsp:include page="header.jsp"/> --%>
<h1>TraceFace</h1>
<p class="subtitle"><c:out value="${empty param.subtitle ? 'AI-powered public content verification' : param.subtitle}"/></p>
