# VideoPlayer

A simple and flexible Android project to **play videos** in a **list** with smooth **scroll-to-play** and **fullscreen transition** — similar to chat app experiences.  
Also includes a separate branch for **manually setting up FFmpeg with JNI** for low-level video decoding.

---

## 📂 Branches

### 1. `test/ffmpeg_clib`

- **Manual FFmpeg Setup**:
    
    - Integrates **FFmpeg** using **JNI and C++**.
        
    - Low-level access to video decoding functions.
        
    - Useful for learning or testing how FFmpeg interacts directly with Android native layers.
        
    - Setup involves writing native C++ code and connecting with Android via JNI.
        

### 2. `test_exoplayer`

- **Video List with Auto Play**:
    
    - Displays a **vertical list of videos**.
        
    - When you **scroll**, the **bottom-most visible video** **auto-plays** smoothly.
        
    - **Click on a video** to **open fullscreen mode** with a **smooth transition animation**.
        
    - Designed to **simulate chat-like video interaction behavior** (like Zalo, Messenger, etc.).
        
    - Built on top of **ExoPlayer** for efficient video playback.
        

---

## ✨ Features

### `test_exoplayer` branch

- Smooth **auto-play** when scrolling (bottom-most video auto starts).
    
- Click to **enter fullscreen** with elegant transition animation.
    
- Shared **ExoPlayer** instance managed via a **VideoPlayerManager** for better performance.
    
- Full **lifecycle management** to handle playback correctly across scrolling and navigation.
    

### `test/ffmpeg_clib` branch

- Native **FFmpeg decoding** setup using **NDK** and **JNI**.
    
- Test environment to:
    
    - Decode video frames manually.
        
    - Prepare for custom video rendering pipelines (e.g., TextureView, SurfaceView).
        
- Good starting point if you want full control over video decoding.
    

---

## 🛠️ Tech Stack

- **Kotlin** (main app code)
    
- **C++ / JNI** (for FFmpeg integration)
    
- **ExoPlayer** (test_exoplayer branch)
    
- **TextureView** for custom video rendering
    
- **RecyclerView** for video list
    
- **MVVM architecture** (ViewModel, Repository)
    
- **Hilt** for Dependency Injection
    
- **Flow** and **StateFlow** for reactive state handling
    
- **MotionLayout** and **TransitionManager** (for smooth fullscreen transitions)
    
---

## Demo with ExoPlayer

https://github.com/user-attachments/assets/3332b621-93cc-4f7a-bbf0-c6a7a1698a34

---

## 📜 Notes

- The two branches are **independent**:
    
    - `test/ffmpeg_clib` is mainly for **low-level FFmpeg testing**.
        
    - `test_exoplayer` focuses on **modern Android UX for video lists**.
        
- Full FFmpeg integration (including audio decoding and complete playback pipeline) is not finalized yet — this branch mainly **tests the decoding layer**.
    

---
