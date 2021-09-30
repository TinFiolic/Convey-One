<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<html>
<%@ include file="../fragments/header.jspf"%>

	<body>
		<%@ include file="../fragments/footer.jspf"%>
		
		<c:if test="${fn:length(code) ne 5}">
			<div id="startScreenFragment">
				<%@ include file="../fragments/startScreen.jspf"%>
			</div>
		</c:if>
		
		<c:if test="${fn:length(code) eq 5}">
			<div id="sessionFragment">
				<%@ include file="../fragments/sessionFragment.jspf"%>
			</div>
		</c:if>
	</body>
	
	<script>

	</script>
</html>

