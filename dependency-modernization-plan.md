# Dependency Modernization Plan

## Current Issues Identified

### Major Architecture Dependencies
- **RxJava2** (io.reactivex.rxjava2:rxjava:2.2.14) → Replace with Coroutines
- **RxAndroid** (io.reactivex.rxjava2:rxandroid:2.1.1) → Replace with Coroutines
- **Room RxJava2** (androidx.room:room-rxjava2:2.6.1) → Replace with room-ktx
- **Retrofit RxJava2 adapter** (com.squareup.retrofit2:adapter-rxjava2:2.9.0) → Remove

### Version Management Issues
- Navigation version hardcoded as "2.3.5" but actually using 2.8.5
- Duplicate preference dependency (line 75 and 85)
- Outdated Mockito versions (2.23.4 vs 4.0.0)

### Legacy Dependencies
- androidx.legacy:legacy-support-v4:1.0.0 → Remove or replace
- kotlin-stdlib-jdk7 → Replace with kotlin-stdlib

## Phase 1: Safe Updates (No Breaking Changes)

### Build System
```gradle
ext.kotlin_version = '2.0.21' // Update from 1.9.22
```

### AndroidX Libraries (Update to Latest)
```gradle
// Core
implementation 'androidx.core:core-ktx:1.15.0' // Keep current, already latest
implementation 'androidx.appcompat:appcompat:1.7.0' // Keep current, already latest

// Navigation (fix version variable)
def nav_version = "2.8.5" // Update from hardcoded 2.3.5
implementation "androidx.navigation:navigation-fragment-ktx:$nav_version"
implementation "androidx.navigation:navigation-ui-ktx:$nav_version"

// Material Design
implementation 'com.google.android.material:material:1.12.0' // Keep current

// Lifecycle (already latest)
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.8.7'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7'

// Room (update to latest)
implementation "androidx.room:room-runtime:2.6.1" // Keep current, but prepare for ktx
kapt "androidx.room:room-compiler:2.6.1"
```

### Dependency Injection
```gradle
// Dagger Hilt (update to latest)
id 'com.google.dagger.hilt.android' version '2.52' apply false
implementation "com.google.dagger:hilt-android:2.52"
kapt "com.google.dagger:hilt-compiler:2.52"
```

### Testing Libraries
```gradle
testImplementation 'junit:junit:4.13.2' // Keep current
testImplementation 'org.mockito:mockito-core:5.14.2' // Update from 2.23.4
androidTestImplementation 'org.mockito:mockito-android:5.14.2' // Update from 4.0.0
androidTestImplementation 'androidx.test:runner:1.6.2' // Keep current
androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1' // Keep current
```

## Phase 2: Architecture Migration (Breaking Changes)

### Replace RxJava2 with Coroutines
```gradle
// Remove RxJava dependencies
// implementation 'io.reactivex.rxjava2:rxjava:2.2.14' // REMOVE
// implementation 'io.reactivex.rxjava2:rxandroid:2.1.1' // REMOVE
// implementation 'com.squareup.retrofit2:adapter-rxjava2:2.9.0' // REMOVE
// implementation 'androidx.room:room-rxjava2:2.6.1' // REMOVE

// Add Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0'

// Room with Coroutines
implementation "androidx.room:room-ktx:2.6.1"

// Retrofit stays the same (has built-in suspend function support)
implementation 'com.squareup.retrofit2:retrofit:2.11.0' // Update version
implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
```

### Remove Legacy Dependencies
```gradle
// Remove legacy support
// implementation 'androidx.legacy:legacy-support-v4:1.0.0' // REMOVE

// Update Kotlin stdlib
implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version" // Replace jdk7 variant

// Remove duplicate
// implementation 'androidx.preference:preference:1.2.1' // REMOVE DUPLICATE (line 85)
```

## Phase 3: Future Preparation

### Add Compose BOM (for future UI migration)
```gradle
// Compose BOM for future migration
def compose_bom_version = '2024.12.01'
implementation platform("androidx.compose:compose-bom:$compose_bom_version")
```

### KAPT to KSP Migration (Future)
```gradle
// Add KSP plugin (future replacement for KAPT)
plugins {
    id 'com.google.devtools.ksp' version '2.0.21-1.0.28'
}
```

## Migration Risks & Considerations

### High Risk (Phase 2)
- RxJava → Coroutines migration will require code changes in:
  - ZoteroAPI.kt (Observable/Single → Flow/suspend functions)
  - Database DAOs (Room RxJava2 → Room KTX)
  - All Presenters/ViewModels using RxJava

### Medium Risk (Phase 1)
- Hilt version updates may require minor annotation changes
- Testing library updates may need test code updates

### Low Risk (Phase 1)
- AndroidX library updates
- Navigation version fix
- Removing duplicates

## Recommended Implementation Order

1. **Start with Phase 1 updates** (safe, no breaking changes)
2. **Fix immediate issues** (duplicate dependencies, version variables)
3. **Test thoroughly** after Phase 1
4. **Plan Phase 2 migration** (requires architectural code changes)
5. **Phase 3** only after MVVM + Compose migration