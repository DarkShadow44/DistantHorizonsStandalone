This directory contains the native files of libraries that were modified for relocation. They will be copied from here during the normal build steps.

Before adding/updating a library, make sure you have Python 3.8+ installed and check the instructions below.

How to add a library's natives:

1. In `build.gradle`:

- Make sure the target package is the same length or shorter (untested) than the source package. Underscores in native methods will be mapped to `_1` so account for that as well.
- Exclude the native files and add them as `relocateNative` (see example).

Example:

```groovy
// Relocate the namespace (Java side)
relocate "org.sqlite", "dh_sqlite", {
    // (Specific to SQLite's relocation)
    // Make sure that native paths are not changed before steps below
    exclude "org/sqlite/native/**"
}
// Shadow also replaces strings inside the Java code
// See the library's source code to find strings used to call into the native code
// This also includes native library paths, if you use mapPaths {} below they will likely need adjustment as well
relocate "jdbc:sqlite", "jdbc:dh_sqlite"

transform(NativeTransformer) {
    // NativeTransformer configuration
    rootDir = project.rootDir

    // Match native libraries
    matchFiles { it.startsWith("org/sqlite") }
    // Replace paths with ones that won't overlap with other mods
    // Libraries are the ones choosing the path to use for natives; check the source code to see which paths are acceptable.
    mapPaths { it.replace("org/sqlite", "dh_sqlite") }

    // Replace native strings, e.g. used in calls back to Java
    // They must be of the same length or shorter!
    relocateNative "org/sqlite", "dh_sqlite"
    // Rename native methods used when calling from Java
    relocateNative "org_sqlite", "dh_1sqlite"
}
```

How to update a library's natives:

1. Delete the library's folder in cache/.
2. It will repopulate during the next build.

Why does this step exist?

- Native files are not handled by the shadow plugin correctly.
- I preferred it as a more streamlined approach, although a bit hacky.
- Possible alternatives:
    - Use edited libraries' source code: although more straightforward, it requires maintaining and updating the repositories for the libraries being added
    - Interfacing with the necessary libraries directly: an absolute mess for technical reasons

What are libraries used?

- LIEF: for fixing binaries after replacing strings
- apple-codesign: for re-signing Mac binaries, since their signatures get invalidated after previous steps
