# This is my backport of Distant Horizons to 1.7.10 - see the [official Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons).

# GTNH Distant Horizons wrapper

Staging repo for quick fixes to [Distant Horizons][dh] and [DH core][core] until they land
upstream. It carries no code of its own: DH sits in `dh/` as a git subtree, DH core sits in
`dh/coreSubProjects/` as a second subtree — exactly where DH's own build expects its submodule —
and a thin Gradle wrapper drives that build so GTNH Actions can build it like any other repo.

# What is Distant Horizons?

Distant Horizons is a mod which implements a [Level of Detail](https://en.wikipedia.org/wiki/Level_of_detail_(computer_graphics)) system to Minecraft.\
This allows for far greater render distances without harming performance by gradually lowering the quality of distant terrain.

Below is a video demonstrating the system:

<a href="https://youtu.be/SxQdbtjGEsc" target="_blank">![Distant Horizons - Alpha 2.0](https://i.ytimg.com/vi/SxQdbtjGEsc/hqdefault.jpg)</a>

# Installation

- Download the latest version from [DistantHorizonsStandalone Releases](https://github.com/DarkShadow44/DistantHorizonsStandalone/releases) and put it into the mods folder

Make sure the latest versions of each of the dependencies are installed:

- [lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify) - Use 3.0.15 or higher
- [GTNHLib](https://github.com/GTNewHorizons/GTNHLib)
- [UniMixins](https://github.com/LegacyModdingMC/UniMixins)

Now supports shaders when used with Angelica 2.1.12 or higher. Tested with [Complementary 5.7.1](https://modrinth.com/shader/complementary-reimagined/version/r5.7.1) with [Euphoria patches 1.8.6](https://modrinth.com/mod/euphoria-patches/version/1.8.6-r5.7.1-forge1.7.10)
If it works with modern DH+Iris, but not with latest Angelica + DH, this should be reported as bug.

# Known Issues

- Memory usage might creep up over time and crash the server
- Server side not fully stable, use with caution
- Sometimes LODs don't update properly, change rendering distance and then back to fix that (upstream issue)

# GTNH 2.8.4

Here detailed instructions how to get DH + latest Angelica working in GTNH 2.8.4 (you'll need to upgrade a few things) in Prism Launcher:

Download the following mods:
- Latest DH - tested with [alpha18](https://github.com/DarkShadow44/DistantHorizonsStandalone/releases/tag/alpha18) - get `distanthorizons-alpha18.jar`
- Latest Angelica - tested with [2.1.16](https://github.com/GTNewHorizons/Angelica/releases/tag/2.1.16) - get `angelica-2.1.16.jar`
- Latest lwjgl3ify - tested with [3.0.15](https://github.com/GTNewHorizons/lwjgl3ify/releases/tag/3.0.15) - get `lwjgl3ify-3.0.15.jar` and `lwjgl3ify-3.0.15-multimc.zip`
- Latest GTNHLib - tested with [0.9.47](https://github.com/GTNewHorizons/GTNHLib/releases/tag/0.9.47) - get `gtnhlib-0.9.47.jar`

From your mods folder delete
- angelica-1.0.0-beta66b.jar
- lwjgl3ify-2.1.16.jar
- gtnhlib-0.7.10.jar

Then add to your mods folder:
- distanthorizons-alpha18.jar
- angelica-2.1.16.jar
- lwjgl3ify-3.0.15.jar
- gtnhlib-0.9.47.jar

Then unzip `lwjgl3ify-3.0.15-multimc.zip`, copy the contents into your Prism Launcher instance. You know you copy into the right folder when you overwrite your `mmc-pack.json`.


# Development

## Structure
```
gtnh-dh/
├── settings.gradle      # includeBuild('dh'), pins -PmcVer=1.7.10
├── build.gradle         # runs the DH build, copies jars to build/libs
├── .github/workflows/   # GTNH Actions
└── dh/                  # subtree ← upstream DH
    └── coreSubProjects/ # subtree ← upstream DH core
```

## Build

```bash
./gradlew build          # jars land in build/libs
```

## Working on a fix

One commit touches exactly one area — the wrapper, `dh/` (excluding core), or
`dh/coreSubProjects/`. Never mix them, and never `--squash`: subtree push and `format-patch`
both depend on the split being clean.

## Sync from upstream

```bash
git subtree pull --prefix=dh dh <branch>
git subtree pull --prefix=dh/coreSubProjects dh-core <sha>
```

If upstream DH bumped its core submodule, the DH pull conflicts at `dh/coreSubProjects`
(their gitlink vs. our tree):

```bash
git ls-files -s dh/coreSubProjects                  # note the new core SHA
git update-index --force-remove dh/coreSubProjects  # drop their gitlink, keep our tree
git commit
git subtree pull --prefix=dh/coreSubProjects dh-core <that sha>
```

## Licensing

The wrapper adds no code. `dh/` and `dh/coreSubProjects/` keep their upstream licenses (LGPL);
see the `LICENSE*` files inside those directories.

[dh]: https://gitlab.com/distant-horizons-team/distant-horizons
[core]: https://gitlab.com/jeseibel/distant-horizons-core
