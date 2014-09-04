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
    mvn jgitflow:release-start
    mvn jgitflow:release-finish
    
    # for point releases (TODO)
    ```

 3. Bump snapshot release:
          
    ```bash
    ./bump-snapshot-version.sh 0.x.0
    ```

 4. Update documentation latest version in [ghpages template](https://github.com/shilad/wikibrain/edit/gh-pages/_config.yml).     
