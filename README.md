# EcoUganda – Green Navigator

This is a complete Android Studio project scaffold with the screen layouts and basic screen navigation already connected.

## Open it
1. Extract `EcoUganda_Android_Studio_Project.zip`.
2. In Android Studio choose **Open**.
3. Select the extracted `EcoUganda` folder (the folder containing `settings.gradle`).
4. Let Android Studio sync the Gradle project. The first sync needs an internet connection to download the Android Gradle Plugin.
5. Press Run to launch on an emulator or connected Android device.

## Included
- Dashboard
- Renewable energy Projects
- Green Schools
- Districts / Reports
- Report detail for the Mabira Forest alert
- Settings
- Mobile hamburger drawer and bottom navigation
- Tablet layout (`layout-sw600dp`) with a permanent left navigation rail and wide content area
- Basic page switching, saved selected page on rotation, and report deep-link entry handling

## Project settings
- Application ID: `com.example.ecouganda`
- App name: EcoUganda
- Java launcher activity; page layouts are XML
- Minimum SDK: 23
- Compile SDK: 35
- Target SDK: 34

## Important scope and accuracy
The layouts are recreated from the screenshots supplied in the conversation. The map panel is a visual placeholder, not a live map. Search fields are visual/input controls but do not query a database. Assignment, offline sync, push-notification delivery, and live project/school/report data need backend and further implementation. The included `reportId` intent entry opens the report detail screen when a matching intent is sent; it does not itself register a push-notification service.
