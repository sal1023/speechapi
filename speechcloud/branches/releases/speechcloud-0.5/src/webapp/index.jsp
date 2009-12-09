<%@ include file="/inc/taglibs.jspf"
%><trim:body>

	<h1>Speech Upload Test Page</h1>

	<form action="<c:url value="/SpeechUploadServlet" />" method="post" enctype="multipart/form-data">
		Audio File:
		<input type="file" name="audio"><br />
		Grammar File:
		<input type="file" name="grammar"><br />
		Parameter1:
		<input type="text" name="p1"><br />
		Parameter2:
		<input type="text" name="p2"><br />
		<input type="submit" value="Upload">
	</form>

</trim:body>