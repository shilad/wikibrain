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
	<li>Run the download script.</li>
 	<li>Run the dumploader.sh
		<ul>
			<li><b>Parameter Format:</b> [-c .conf] [-it] [file ...]</li>
			<br />
			<li>-c &nbsp;&nbsp;&nbsp; Sets configuration file. Include that file .conf</li>
			<li>-i &nbsp;&nbsp;&nbsp; Creates all indexes after loading</li>
			<li>-t &nbsp;&nbsp;&nbsp; Drops and recreates all tables</li>
		</ul>
	</li>
	
 	<li>Run the redirectloader.sh
 		<ul>
 			<li><b>Parameter Format:</b> [-c .conf] [-it] [-l lang ...]</li>
 			<br />
 			<li>-c &nbsp;&nbsp;&nbsp; Sets configuration file. Include that file .conf</li>
			<li>-i &nbsp;&nbsp;&nbsp; Creates all indexes after loading</li>
			<li>-l &nbsp;&nbsp;&nbsp; Set language for redirects to be resolved in. Enter as lang codes with spaces between them.</li>
			<li>-t &nbsp;&nbsp;&nbsp; Drops and recreates all tables</li>
		</ul>
 	</li>
 	
 	<li>Run the wikitextdumploader.sh
 		<ul>
 			<li><b>Parameter Format:</b> [-c .conf] [-it] [-l lang ...]</li>
 			<br />
 			<li>-c &nbsp;&nbsp;&nbsp; Sets configuration file. Include that file .conf</li>
			<li>-i &nbsp;&nbsp;&nbsp; Creates all indexes after loading</li>
			<li>-l &nbsp;&nbsp;&nbsp; Set languages to be processed. Also sets ills (Inter Language Links). Enter as lang codes with spaces between them.</li>
			<li>-t &nbsp;&nbsp;&nbsp; Drops and recreates all tables</li>
		</ul>
	</li>
	<li>Run concept mapper scripts.</li>
 </ol>
