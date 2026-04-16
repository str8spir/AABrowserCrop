# <img src="https://github.com/user-attachments/assets/fa4252fa-b71e-4c87-9b93-d8ad832434cc" width="48" height="48" style="vertical-align: bottom;" /> AA Browser (Widescreen Edition)

[![Android](https://img.shields.io/badge/Android-15%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[<img src="https://github.com/user-attachments/assets/1551eaef-432d-4634-875c-f085870d00a1" alt="Get it on Obtainium" height="40">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.kododake.aabrowser%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fkododake%2FAABrowser%22%2C%22author%22%3A%22kododake%22%2C%22name%22%3A%22AABrowser%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D)

**The ultimate WebView browser experience for Android Auto, reimagined for ultra-widescreen dashboards.**
Transform your stationary time with a modern browser optimized for wide aspect ratios (32:9 / 21:9) found in modern systems like BMW iDrive 7/8/9 (X5 G05 45E) and Mercedes MBUX.

> [!NOTE]
> **Project Lineage**
> This is a specialized fork of **[kododake/AABrowser](https://github.com/kododake/AABrowser)**. It maintains all original features while adding specific widescreen ergonomics and video scaling tools.

---

## ✨ Widescreen Edition Exclusives

This fork addresses the "black bar" issue common on ultra-wide head units by introducing a context-aware control system.

### 🏝️ The Floating Control Island
Designed specifically for **32:9 aspect ratios**, this UI element only appears when a video enters fullscreen mode. It allows you to:
* **Zoom In/Out:** Incremental magnification to perfectly fit the video to your wide display.
* **Fill-to-Crop:** Utilize the full width of your dashboard, eliminating pillarboxing.
* **Granular Control:** Precise +/- adjustments to find the sweet spot between content visibility and screen coverage.
* **Overflow Lock:** Automatically locks document scrolling when zooming to keep the video perfectly centered.
  
<img width="1024" height="498" alt="image" src="https://github.com/user-attachments/assets/9bfa5d5b-4fb3-4c05-8be4-1d08baab6710" />
<img width="1024" height="588" alt="image" src="https://github.com/user-attachments/assets/cb4cb8b7-6657-462e-b32a-5aba711fcaba" />
<img width="1024" height="520" alt="image" src="https://github.com/user-attachments/assets/1879c6c0-72b8-4062-8f9e-e5f4ce236062" />


---

## 🌐 Core Features

* 🎯 **Native Automotive UI:** Designed for car displays, ensuring readability and touch-target accuracy.
* 🎬 **Immersive Media:** Fullscreen DRM-protected video support (Widevine L3).
* 🎨 **Light + AMOLED Themes:** True-black mode designed to blend perfectly with automotive interior panels.
* 🏠 **Enhanced Start Page:** Six configurable quick-link slots with local icon caching for a branded feel.
* 🗂️ **Tab Management:** Full tab manager with session restoration support.
* 🔎 **Global Display Scale:** Scale the entire UI and web content together via custom presets.
* 🔄 **Desktop Mode:** Toggle between mobile and desktop rendering with a single tap.

---

## 📱 Quick Start & Safety 🚦

> [!CAUTION]
> **Stationary Use Only:** This app is intended for use only while the vehicle is safely parked. **DO NOT** attempt to use this app while driving.

#### 🛠️ How to Enable Unknown Sources on Android Auto
1.  Open **Android Auto Settings** on your phone.
2.  Scroll to the bottom and **tap "Version" 10 times**.
3.  Tap the **three-dot menu (⋮)** -> **Developer settings**.
4.  Enable **Unknown sources**.

---

## 🤝 Credits & Contributors

A massive thanks to the developers who made this tool possible:

* **[kododake](https://github.com/kododake):** Original Project Lead.
* **[SaveEditors](https://github.com/SaveEditors):** Major architecture updates (Tabs, Themes, Start-page).
* **[cmacrowther](https://github.com/cmacrowther):** Design improvements.
* **[jigneshbhavani](https://github.com/jigneshbhavani):** Microphone support.

---

## 🛡️ Privacy
This app is designed with a **Transparency-First** policy. No URL tracking, no Big Tech trackers, and only anonymous, self-hosted analytics are used to improve app stability.
