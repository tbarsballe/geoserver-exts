<html>
<head>
<title> GeoGig Web API </title>
<meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>
<h2>Geogig repositories</h2>
<#if repositories?size != 0>
<ul>
<#foreach repo in repositories>
<li><a href="${page.pageURI(repo.id)}">${repo.id}</a> (${repo.name})</li>
</#foreach>
</ul>
<#else>
<p>There are no Geogig DataStores configured and enabled.</p>
</#if>
</body>
</html>
