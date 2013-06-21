wikAPIdia
=====
<br />
Here is the basic outline of how this program runs:
<ol>
	<li>Download scripts... </li>
	<li>Load the Dump as XML <br />
			<ol>
			<li>Convert Dump into RawPages </li>
			<li>Convert RawPages into LocalPages
					<ol>
					<li>Mark Redirects to be dealt with after this process</li> 
					</ol></li>
			</ol></li>
	<li>Resolve Redirects 
			<ol>
			<li>Load into Redirect Table, fully resolved </li>
			</ol></li>
	<li>WikiTextParser does the following 
			<ol>
			<li>load links into table with src/dest IDs </li>
			<li>load categories with the source article as a category member</li>
			</ol></li>
	<li>Load Concepts</li>
	<li>Load Concept Links</li>
</ol>

<br />
Here is what you, the user, need to do:
<ol>
 	<li>Script-y stuff</li>
 </ol>
