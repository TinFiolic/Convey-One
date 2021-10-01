<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<html>
<%@ include file="../fragments/header.jspf"%>

	<body onclick="pageRedirect()">
		<%@ include file="../fragments/footer.jspf"%>
		
		<div class="wrapper">
			<div class="formContent">
				<div class="logo"></div>
				<h1 style="font-variant: small-caps;">404 - page not found</h1>
				<p>click anywhere to return to the main page</p>
			</div>
		</div>
	
	<script>
		document.body.style.backgroundImage = "url('/img/dark_bg1.png')";
		
	    function pageRedirect() {
	        window.location.href = "/";
	      }      
	</script>
</html>

