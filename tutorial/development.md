---
    layout: default
    title: Development Environment
---

# Setting up your WikiBrain Development Environment

This only applies if you actually want to make *changes* to Wikibrain. 
If you just want to use it in your own project, follow the installation guide.
These guidelines are for IntelliJ Version 15.

1. In IntelliJ, select "Checkout from Version Control" https://github.com/shilad/wikibrain.git.
2. Make sure you have a valid JDK (7+) specified in "Project Structure."
3. You may need to turn on some tool windows: View -> Tool Windows -> Project and Maven Projects.
4. In the "Maven Projects" window, run the maven "Test" target under -> wikibrain-core -> Lifecycle -> test
5. In the project window, find wikibrain-core -> src/generated/jooq-h2. Right click and select Mark directory -> Generated sources root.
