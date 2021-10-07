<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<html>
<%@ include file="../fragments/header.jspf"%>

	<body>
		<%@ include file="../fragments/footer.jspf"%>
		
		<div id="poppupMessageContainer"></div>
		
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
		var poppupCounter = 0;
		
		document.body.style.backgroundImage = "url('/img/dark_bg1.png')";
		
		function showPoppupMessage(message, type) {
			var delayTime= 5000;
			var thisPoppup = poppupCounter;

			$("#poppupMessageContainer").append('<div class="poppupMessage" id="' + thisPoppup + '"></div>');
			
			$("#" + thisPoppup).text(message);

			if(type == "success") {
				$("#" + thisPoppup).css("background-color", "#28A745");
				$("#" + thisPoppup).css("border", "4px solid #218838");
				delayTime = 5000;
			}

			if(type == "fail") {
				$("#" + thisPoppup).css("background-color", "#DC3545");
				$("#" + thisPoppup).css("border", "4px solid #C92333");
				delayTime = 7000;
			}
			
			poppupCounter++;
			
			$("#" + thisPoppup).fadeIn();
			$("#" + thisPoppup).delay(delayTime).fadeOut("slow");
		}
	</script>
</html>

