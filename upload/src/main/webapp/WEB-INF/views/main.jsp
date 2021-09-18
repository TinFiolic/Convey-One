<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<%@ include file="../fragments/header.jspf"%>

<body>
	<div class="bg-image">
		<button id="generateCode" class="buttonFont generateButton">Generate
			code</button>

		<br>

		<div id="existingFiles" hidden="true">
			<c:if test="${not empty '${files}'}">
				<c:forEach items="${files}" var="file" varStatus="loop">
					<a href="download/${code}/${loop.index}"> <i
						class="fa fa-file file-download" data-id="${loop.index}"></i>

						<p data-id="${loop.index}">
							<script>
								var fileName = '${file.getName()}';
								fileName = fileName.slice(0, -4);
								document.write(fileName);
							</script>
						</p>
					</a>

					<p class="deleteFile" data-id="${loop.index}">Delete file</p>
				</c:forEach>
			</c:if>

			<c:if test="${not empty '${code}'}">
				<i class="fa fa-close endSession" style="font-size: 25px"></i>
				<br>
				<form action="/upload/${code}" class="dropzone" id="files"
					hidden="true"></form>
			</c:if>
		</div>

		<div class="codeSection" hidden="true">
			<h3 class="textFont">
				Enter the code below on another device to access uploaded files
				(case-sensitive)
				</h2>
				<br>
				<h1 id="code">CODE</h1>
				<br>
				<h3 class="textFont">
					...or scan the following QR code if you're using a camera-enabled
					device
					</h2>
					<br>
					<div id="qrCode"></div>
		</div>
	</div>

	<script>
		// 		If someone joins a session
		var joinedFromCode = ('${fromLink}' === 'true');
		console.log(joinedFromCode);

		//		QR Generator inicialization
		var qrcode = new QRCode("qrCode", {
			width : 128,
			height : 128,
			colorDark : "#000000",
			colorLight : "#ffffff",
			correctLevel : QRCode.CorrectLevel.H
		});

		$(document).ready(function() {
			// 		    Check if user already has an active file sharing session
			if ("${code}".length > 1 && "${code}" != "NULL") {
				$("#files").show();
				$("#generateCode").hide();

				$(".codeSection").show();

				$("#code").html("${code}");
				$("#files").attr("action", "/upload/${code}");

				$("#existingFiles").show();

				// 			    Generate new QR Code
				qrcode.makeCode(window.location.host + "/join/{code}");
			}
		});

		//		Dropzone initialization
		Dropzone.options.dropzoneForm = {
			addRemoveLinks : true,
			maxFiles : 10,
			maxFilesize : 5,
			init : function() {
				this.on("error", function(file, message) {
					alert(message);
					this.removeFile(file);
				});
			},
			accept : function(file, done) {
				if (file.name.length > 30) {
					done("Filename exceeds 30 characters!");
				} else {
					done();
				}
			},
		};

		//		Ajax call for code generation
		$("#generateCode").click(function() {
			$.ajax("/code")

			.done(function(data) {
				console.log("Success generated a new code " + data);

				location.reload();
			})

			.fail(function() {
				console.log("Failed at generating a new code!");
			})
		});

		//		Ajax call for file deletion
		$(".deleteFile").click(function() {
			var id = $(this).data("id");

			$.ajax("/delete/${code}/" + id)

			.done(function(data) {
				console.log("Success deleted a file!");

				$('[data-id]').each(function() {
					if ($(this).data("id") == id) {
						$(this).hide();
					}
				});
			})

			.fail(function() {
				console.log("Failed at deleting a file!");
			})
		});

		//		Ajax call for session deletion
		$(".endSession").click(function() {

			$.ajax("/endSession/${code}/")

			.done(function(data) {
				console.log("Success ended a session!");

				location.reload();
			})

			.fail(function() {
				console.log("Failed at ending a session!");
			})
		});
	</script>
</body>
</html>

