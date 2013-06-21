wikAPIdia
=====

Here is the basic outline of how this program runs:
	1. Download scripts... 
	2. Load the Dump as XML
			a. Convert Dump into RawPages
			b. Convert RawPages into LocalPages
					1) Mark Redirects to be dealt with after this process
	3. Resolve Redirects
			a. Load into Redirect Table, fully resolved
	4. WikiTextParser does the following
			a. load links into table with src/dest IDs
			b. load categories with the source article as a category member
	5. Load Concepts
	6. Load Concept Links

Here is what you, the user, need to do:
 	1. script-y stuff
