#!/bin/bash

DL=../dat/gold/dl
SRC=../dat/gold/src  # src datasets
CLEANED=../dat/gold/cleaned  # cleaned source datasets


rm -rf $DL
mkdir -p $DL
rm -rf $SRC
mkdir -p $SRC
rm -rf $CLEANED
mkdir -p $CLEANED

mkdir $SRC/titles   # wikipedia titles to similarity scores
mkdir $SRC/phrases  # phrases similarity scores

# Downloads datasets and combines them into a single gold standard


# Gabrilovich et al, 2002
# see http://www.cs.technion.ac.il/~gabr/resources/data/wordsim353/
wget -P $DL http://www.cs.technion.ac.il/~gabr/resources/data/wordsim353/wordsim353.zip &&
mkdir $DL/wordsim353 &&
unzip -d $DL/wordsim353 $DL/wordsim353.zip &&
tail -n '+2' $DL/wordsim353/combined.csv > $SRC/phrases/wordsim353.csv || 
{ echo "ERROR: preparing wordsim353 failed" >&2; exit 1;}

# A version of wordsim353 manually matched to wikipedia articles
wget -P $DL http://www.nzdl.org/wikipediaSimilarity/wikipediaSimilarity353.csv &&
mkdir $DL/wikisim353 &&
tail -n '+2' $DL/wikipediaSimilarity353.csv |
cut -f 3,6,7 -d ',' |
tr -d '\r' >$SRC/titles/wikisim353.csv ||
{ echo "ERROR: preparing wikisim353 dataset failed" >&2; exit 1;}

# MTurk, Radinsky et al, 2011
# see http://www.technion.ac.il/~kirar/Datasets.html
wget -P $DL http://www.technion.ac.il/~kirar/files/Mtruk.csv &&
cp -p $DL/Mtruk.csv $SRC/phrases/radinsky.csv || 
{ echo "ERROR: preparing radinsky dataset failed" >&2; exit 1;}

# Concept sim, Miller et al, 1991
# http://www.seas.upenn.edu/~hansens/conceptSim/
wget -P $DL http://www.seas.upenn.edu/~hansens/conceptSim/ConceptSim.tar.gz &&
tar -C $DL -xzvf $DL/ConceptSim.tar.gz &&
sed -e 's/	[	]*/,/g' < $DL/ConceptSim/MC_word.txt  > $SRC/phrases/MC.csv &&
sed -e 's/	[	]*/,/g' < $DL/ConceptSim/RG_word.txt  > $SRC/phrases/RG.csv ||
{ echo "ERROR: preparing conceptsim dataset failed" >&2; exit 1;}

# Atlasify: Hecht et al, 2012
#
wget -P $DL http://www.cs.northwestern.edu/~ddowney/data/atlasify240.csv &&
cp -p $DL/atlasify240.csv $SRC/phrases/atlasify240.csv ||
{ echo "ERROR: preparing atlasify dataset failed" >&2; exit 1;}

# WikiSimi
#
wget -P $DL http://sigwp.org/wikisimi/WikiSimi3000_1.csv &&
cp -p $DL/WikiSimi3000_1.csv  $SRC/titles/WikiSimi3000.tab ||
{ echo "ERROR: preparing wikisimi dataset failed" >&2; exit 1;}


for d in titles phrases; do
    python src/main/python/combine_gold.py $SRC/$d/*.* |
    python src/main/python/filter_gold.py 10 >../dat/gold/gold.$d.similarity.txt ||
        { echo "ERROR: combining $d datasets failed" >&2; exit 1;}

    python src/main/python/combine_gold.py $SRC/$d/*.* |
    python src/main/python/filter_gold.py 4 0.6 >../dat/gold/gold.$d.mostSimilar.txt ||
        { echo "ERROR: combining $d datasets failed" >&2; exit 1;}
done

# Create individual datasets
for d in $SRC/phrases $SRC/titles; do
    for file in `ls $d`; do
        txt=`echo $file | sed 's/\..*$/.txt/'`
        python src/main/python/combine_gold.py $d/$file >$CLEANED/$txt
    done
done


echo "SUCCESS!"
