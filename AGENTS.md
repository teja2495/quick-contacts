# Quick Contacts - AI Agent Guide

- Follow the existing code architecture, folder structure, and UI design patterns. When in doubt, inspect the surrounding implementation before changing it.
- Do not run automated UI tests unless the user explicitly asks. The user will normally perform manual UI testing and report the behavior.
- Do not run `git add` or `git commit` without permission.
- After every coding task, run `run` from this repository. It builds the debug APK, installs it on every connected Android device, stops the existing debug app, and launches `com.tk.quickcontacts.debug/com.tk.quickcontacts.MainActivity`. Rerun it until it succeeds; if no device is connected, report that runtime validation is blocked.
