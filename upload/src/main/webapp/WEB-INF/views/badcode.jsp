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
				
				<c:if test="${not tooManyRequests}">
				<h1 style="font-variant: small-caps;"><b>${code}</b> code is invalid!</h1>
				</c:if>
				
				<c:if test="${tooManyTries}">
					<br>
					<h3 style="font-variant: small-caps;">Too many failed attempts. <br> Please, wait for a minute and try again.</h3>
				    <br>
				</c:if>
				
				<c:if test="${tooManyRequests}">
					<br>
					<h3 style="font-variant: small-caps;">You are making too many download/upload requests! <br> Please, wait for a minute and try again.</h3>
				    <br>
				</c:if>
				
				<p style="font-variant: small-caps;">click anywhere to return to the main page</p>
			</div>
		</div>
	
	<script>
		document.body.style.backgroundImage = "url('/img/dark_bg1.png')";

		var redirectCase = $.parseJSON('${tooManyRequests}'.toLowerCase());

		if(redirectCase === null || redirectCase === 'undefined')
			redirectCase = false;
		
	    function pageRedirect() {
		    if(redirectCase)
	        	window.location.href = "/join/${code}";
		    else
		    	window.location.href = "/";
	      }      
	</script>
</html>

