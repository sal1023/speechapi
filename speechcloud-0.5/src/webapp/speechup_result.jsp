<%@ include file="/inc/taglibs.jspf"
%><trim:body>

	<h1>Speech Upload Result Page</h1>
	
	<c:choose>
		<c:when test="${!empty fileUploadList}">
			<h2>Files Successfully uploaded:</h2>
			<ul>
				<c:forEach var="filename" items="${fileUploadList}">
					<li><c:out value="${filename}" /></li>
				</c:forEach>
			</ul>
		</c:when>
		<c:otherwise>
			<p>No files uploaded!</p>
		</c:otherwise>
	</c:choose>

</trim:body>