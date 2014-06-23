---
    layout: default
    title: Release Checklist
---
# WikiBrain Release Checklist

 1. Run unit tests; make sure they pass.
 2. Release new version: 

    ```bash
    # for major releases (creates new branch)
    ./push-major-release.sh 0.x.0
    
    # for point releases
    ./push-minor-release.sh 0.x.0
    ```

 3. Bump snapshot release:
          
    ```bash
    ./bump-snapshot-version.sh 0.x.0
    ```

 4. Update documentation latest version in [ghpages template](https://github.com/shilad/wikibrain/edit/gh-pages/_config.yml).     