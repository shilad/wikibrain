
# Main components
The WikiBrain Configurator offers a set of components that you can use as building blocks in your application.
To get one of these components, use the Configurator.get() method:

* *RawPageDao* provides detailed information about an article, include the raw WIkiMarkup pagetext.
* *LocalPageDao* provides basic metadata about an article, including title, namespace, and Wikipedia id.
* *LocalLinkDao* provides access to the Wikilink structure between pages.
* **LocalCategoryMemberDao** provides access to Wikipedia's category graph.
* **UniversalArticleDao** provides access to the multilingual concept mapping.
* **UniversalLinkDao** exposes the link structure imposed by the multilingual mapping.
* **WikidataDao** stores factual statements, aliases, and descriptions about to multilingual concepts.
* **LuceneSearcher** searches arbitrary fields (e.g. title or plain text) in an arbitrary language.
* **SparseMatrix** represents a sparse matrix of ints (ids) to floats (values) that is persisted using memory mapping to disk.
* **PhraseAnalyzer** returns the most likely Wikipedia articles for a textual phrase, and the most common textual phrases that represent a particular Wikipedia article.
* **MonolingualSRMetric** returns the strength of relationship between two Wikipedia pages or phrases in a particular language, and the most closely related pages to a particular phrase or page.
* **UniversalSRMetric** (not yet tested) returns the same information as the MonolingualSRMetric, but for universal concepts that span multiple languages.
