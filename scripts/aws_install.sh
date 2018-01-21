ssh -i ~/Documents/feb2016-keypair.pem ec2-user@ec2-34-192-219-149.compute-1.amazonaws.com

----- Ubuntu -----

WB_LANG=en

sudo su - 

# Update, etc.
apt-get update
apt-get upgrade
apt-get install unzip zip pigz python3-pip
sudo pip3 install cython gensim

# Setup postgres
apt-get install postgresql postgresql-contrib
vim /etc/postgresql/9.5/main/pg_hba.conf # change dd

cat >> /etc/postgresql/9.5/main/postgresql.conf  << HERE
listen_addresses = '*'
max_connections = 1000         # Must be at least 300
shared_buffers = 48GB         # Should be 1/4 of system memory
effective_cache_size = 96GB   # Should be 1/2 of system memory
fsync = off                 
synchronous_commit = off    
max_wal_size = 10GB
checkpoint_completion_target = 0.9
autovacuum = off
HERE

systemctl enable postgresql
systemctl start postgresql

su - postgres
createuser -s -r -d -P wikibrain
createdb wikibrain_${WB_LANG}

exit    # will be root
exit    # will be ec2-user

# Setup java
curl -s "https://get.sdkman.io" | bash
source "/home/ubuntu/.sdkman/bin/sdkman-init.sh"
sdk install java 8u161-oracle
sdk install maven

# Wikibrain setup
git clone https://github.com/shilad/wikibrain.git
cd wikibrain/
git checkout develop
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller
export JAVA_OPTS="-d64 -Xmx120000M -server"
cat >./wmf_${WB_LANG}.conf <<HERE
    baseDir : /home/ec2-user/wikibrain/base_${WB_LANG}
    dao.dataSource.default : psql
    dao.dataSource.psql {
        username : wikibrain
        password : wikibrain
        connectionsPerPartition : 5
        url : "jdbc:postgresql://localhost/wikibrain_${WB_LANG}"
    }
sr.metric.local.word2vec2 : \${sr.densevectorbase} {
        generator : {
            type : word2vec
            corpus : wikified
            modelDir : \${baseDir}"/dat/word2vec2"
        }
        reliesOn : [ "prebuiltword2vec" ]
    }

sr.wikifier.websail2 : {
            type : websail
            phraseAnalyzer : anchortext
            sr : word2vec2
            identityWikifier : identity
            localLinkDao : matrix
            useLinkProbabilityCache : true
            desiredWikifiedFraction : 0.25
        }

sr.corpus.wikified2 : {
            path : \${baseDir}"/dat/corpus/wikified2/"
            wikifier : websail2
            rawPageDao : default
            localPageDao : default
            phraseAnalyzer : anchortext
}
HERE
mkdir -p /home/ec2-user/wikibrain/base_${WB_LANG}

# Import data 
export JAVA_OPTS="-d64 -Xmx120000M -server"
./wb-java.sh org.wikibrain.Loader -l ${WB_LANG} -c wmf_${WB_LANG}.conf

# Make corpus
./wb-java.sh org.wikibrain.sr.wikify.CorpusCreatorMain -p wikified2 -o corpus_en -f 0.3 -c wmf_${WB_LANG}.conf

