ConceptSim README:
------------------
Sense (concept) annotated lists of similarity scores between nominal concepts.
WordNet version: 3.0


Author:
-------
 Hansen Andrew Schwartz, 2011 -- Reference:
   Hansen A. Schwartz, Fernando Gomez. 2011. Evaluating Semantic Metrics on 
   Tasks of Concept Similarity. In FLAIRS-24. Palm Beach, Florida. 


Description:
------------
 ConceptSim contains sense-annotated versions of three standard similarity 
 datasets:
    MC
    RG
    WordSim-Sim
 Each pair of words was annotated by two humans with WordNet 3.0 senses. The 
 inter-annotator agreement ranged from 86% - 93%. The similarity scores 
 themselves are maintained from the original datasets (motivated by past 
 research showing greatest correlations with human judgments coming from the 
 maximum similarity over all pairs of senses). The final version of each 
 sense-annotated dataset was the result of annotators coming to an agreement 
 on disagreed senses. 


Attribution:
------------
 *RG: All files prefixed with "RG" were derived from the Rubenstein 
 and Goodenough dataset:
   Rubenstein, H., and Goodenough, J. 1965. Contextual correlates of synonymy. 
   Communications of the ACM 8:627-633.

 *MC: All files prefixed with "MC" were derived from the Miller and 
 Charles dataset:
   Miller, G., and Charles, W. 1991. Contextual correlates of semantic 
  s imilarity. Language and Cognitive Processes 6(1):1-28.

 *WordSim: All files prefixed with "WordSim" were derived from the "similarity" 
 subset of the WordSim dataset:
 -original dataset:
   Finkelstein, L.; Gabrilovich, E.; Matias, Y.; Rivlin, E.; Solan, Z.; 
   Wolfman, G.; and Ruppin, E. 2001. Placing search in context: The concept 
   revisited. In ACM Trans. on Information Systems.
 -similarity subset:
   Agirre, E.; Alfonseca, E.; Hall, K.; Kravalova, J.; Pasca, M.; and Soroa, A. 
   2009. A study on similarity and relatedness using distributional and 
   wordnet-based approaches. In The Annual Conference of the NAACL, 19-27.


License:
--------
ConceptSim by Hansen Andrew Schwartz is licensed under a 
Creative Commons Attribution-NonCommercial 3.0 Unported License. 
http://creativecommons.org/licenses/by-nc/3.0/


Format:
-------
All data files contain are of the format:

   <concept1><tab><concept2><tab><similarity_score>

where each concept is presented as

   <word>#<pos>#<WordNet_sense>


Main Sense Annotated Files:
---------------------------
MC_sense.txt - The annotator agreed upon MC dataset
RG_sense.txt - The annotator agreed upon RG dataset
WordSim_sense.txt - The annotator agreed upon WordSim dataset


Other files:
------------
MC_sense.annotator1.txt
MC_sense.annotator2.txt
MC_word.txt
README.txt
RG_sense.annotator1.txt
RG_sense.annotator2.txt
RG_word.txt
WordSim_sense.annotator1.txt
WordSim_sense.annotator2.txt
WordSim_with_unrelated.txt


contact: hansens@upenn.edu

