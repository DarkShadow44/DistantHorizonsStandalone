# This is my backport of Distant Horizons to 1.7.10 - see the [official Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons).

# What is Distant Horizons?

Distant Horizons is a mod which implements a [Level of Detail](https://en.wikipedia.org/wiki/Level_of_detail_(computer_graphics)) system to Minecraft.\
This allows for far greater render distances without harming performance by gradually lowering the quality of distant terrain.

Below is a video demonstrating the system:

<a href="https://youtu.be/SxQdbtjGEsc" target="_blank">![Distant Horizons - Alpha 2.0](https://i.ytimg.com/vi/SxQdbtjGEsc/hqdefault.jpg)</a>

# Installation

- Download the latest version from [DistantHorizonsStandalone Releases](https://github.com/DarkShadow44/DistantHorizonsStandalone/releases) and put it into the mods folder

Make sure the latest versions of each of the dependencies are installed:

- [lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify)
- [GTNHLib](https://github.com/GTNewHorizons/GTNHLib)
- [UniMixins](https://github.com/LegacyModdingMC/UniMixins)

# Known Issues

- If you use Angelica, you might need to check "Disable Terrain Fog" to prevent rendering issues.
- Currently no shader support — using a shader mod will break rendering.
