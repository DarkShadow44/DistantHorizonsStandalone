# This is my backport of Distant Horizons to 1.7.10 - see the [official Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons).

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
