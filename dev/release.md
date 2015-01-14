---
    layout: default
    title: Release Checklist
---
# WikiBrain Release Checklist

 1. Run unit tests; make sure they pass.

 2. Release new version: 

    ```bash
    
    # for major releases
    git checkout develop
    mvn jgitflow:release-start -DskipTests=true
    mvn jgitflow:release-finish -DskipTests=true
    git checkout <release-version>
    mvn -P release-profile deploy -DskipTests
    ./push-major-release.sh
    git checkout develop
    
    ```

 3. Update documentation latest version in [ghpages template](https://github.com/shilad/wikibrain/edit/gh-pages/_config.yml).     
