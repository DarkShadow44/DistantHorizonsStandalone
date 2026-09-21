# <img src="https://gitlab.com/distant-horizons-team/distant-horizons-core/-/raw/main/_Misc%20Files/logo%20files/new/SVG/Distant-Horizons.svg" height="128px"> 
_See farther without turning your game into a slide show._

<br>

# What is Distant Horizons?

Distant Horizons is a mod which implements a [Level of Detail](https://en.wikipedia.org/wiki/Level_of_detail_(computer_graphics)) system to Minecraft.\
This allows for far greater render distances without harming performance by gradually lowering the quality of distant terrain.

Below is a video demonstrating the system:

<a href="https://youtu.be/SxQdbtjGEsc" target="_blank">![Distant Horizons - Alpha 2.0](https://i.ytimg.com/vi/SxQdbtjGEsc/hqdefault.jpg)</a>

## Translations

[![Crowdin](https://badges.crowdin.net/distant-horizons/localized.svg)](https://crowdin.com/project/distant-horizons)\
Crowdin Project: [Distant Horizons](https://crowdin.com/project/distant-horizons)\
Guidelines: [translations.md](translations.md)
<br>

## Source Code Installation

### Prerequisites

* A Java Development Kit (JDK) for Java 25 (recommended) or newer. <br>
  Visit https://www.oracle.com/java/technologies/downloads/ for installers.
* Git or someway to clone git projects. <br> 
  Visit https://git-scm.com/ for installers.
* (Not required) Any Java IDE with plugins that support Manifold, for example IntelliJ IDEA.

**If using IntelliJ:**
1. Install the Manifold plugin
   - https://plugins.jetbrains.com/plugin/10057-manifold-ij
2. Open IDEA and import the build.gradle
3. Refresh the Gradle project in IDEA if required

<br>

## Switching Versions

To switch between different Minecraft versions, change `mcVer=1.?` in the `gradle.properties` file.

If running in an IDE, to ensure the IDE noticed the version change, run any gradle command to refresh gradle.\
In IntelliJ, you will also need to do a gradle sync if it didn't happen automatically.

<br>

## Compiling

Prerequisites:
- JDK 25 or newer

From the command line:
1. `git clone --recurse-submodules https://gitlab.com/distant-horizons-team/distant-horizons.git`
2. `cd distant-horizons`
3. `./gradlew assemble`
5. The compiled jar file will be in the folder `\build\libs`

From the File Explorer:
1. Download and extract the project zip
2. Download the core from https://gitlab.com/distant-horizons-team/distant-horizons-core and extract into a folder called `coreSubProjects`
3. Open command prompt/terminal in the project folder
4. Run the commands: `./gradlew assemble`
6. The compiled jar file will be in the folder `\build\libs`

>Note: You can add the argument `-PmcVer=?` to tell gradle to build a selected MC version instead of having to modify the `gradle.properties` file.\
> For example: `./gradlew assemble -PmcVer=1.18.2`

<br>

## Other commands

Run the standalone jar: `./gradlew run` <br>
Build the standalone jar: `./gradlew core:build` <br>
Only build Fabric: `./gradlew fabric:assemble` or `./gradlew fabric:build` <br>
Only build Forge: `./gradlew forge:assemble` or `./gradlew forge:build` <br>
Run the Fabric client (for debugging): `./gradlew fabric:runClient` <br>
Run the Forge client (for debugging): `./gradlew forge:runClient` <br>
Delete all compiled code: `./gradlew clean` <br>
Refresh local dependencies: `./gradlew --refresh-dependencies`

To build all versions: `./buildAll`

<br>

## Open Source Libraries

Forgix (To merge multiple mod versions into one jar) [_Formerly_ [_DHJarMerger_](https://github.com/Ran-helo/DHJarMerger)]\
https://github.com/PacifistMC/Forgix

LZ4 for Java (data compression)\
https://github.com/lz4/lz4-java

NightConfig for JSON & TOML (config handling)\
https://github.com/TheElectronWill/night-config

SVG Salamander for SVG support (not being used atm)\
https://github.com/blackears/svgSalamander

sqlite-jdbc\
https://github.com/xerial/sqlite-jdbc


## Acknowledgements

Distant Horizons has been graciously provided an open source license for <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>.  

> <img src="https://www.yourkit.com/images/yklogo.png">
>
> YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.


