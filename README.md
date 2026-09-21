# GTNH Distant Horizons wrapper

Staging repo for quick fixes to [Distant Horizons][dh] and [DH core][core] until they land
upstream. It carries no code of its own: DH sits in `dh/` as a git subtree, DH core sits in
`dh/coreSubProjects/` as a second subtree — exactly where DH's own build expects its submodule —
and a thin Gradle wrapper drives that build so GTNH Actions can build it like any other repo.

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

Everything else is DH's normal build (`./gradlew -PmcVer=1.7.10 :forge17:build` under the hood).
To build another Minecraft version, pass `-PmcVer=<ver> -PdhLoaders=<loader>` — see
`dh/versionProperties/<ver>.properties` for the loaders a version builds for.

GTNH Actions drives the same tasks it drives everywhere (`setupCIWorkspace`, `assemble`,
`build`, `publish`). DH's build has no equivalent for some of them, so the wrapper registers
them as no-ops — see the bottom of `build.gradle`. `runServer` and the spotless auto-PR are
switched off in `.github/workflows/build-and-test.yml`: `dh/` is upstream code and must not be
reformatted. Jars are collected into `build/libs`, which is what the workflows upload.

Never run `git submodule` here. The core submodule is deliberately gone; the subtree replaces it.

## Remotes

| name        | points to           | use               |
|-------------|---------------------|-------------------|
| `dh`        | upstream DH         | pull              |
| `dh-core`   | upstream DH core    | pull              |
| `dh-fork`   | our fork of DH      | push fix branches |
| `core-fork` | our fork of DH core | push fix branches |

The two upstream remotes are configured. Add the forks once they exist:

```bash
git remote add dh-fork   <url>
git remote add core-fork <url>
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

## Upstreaming

Core, straight from the subtree:

```bash
git subtree push --prefix=dh/coreSubProjects core-fork fix/<name>
```

then open a PR from `core-fork`.

DH itself — `subtree push` would drag core along, so send patches instead:

```bash
git format-patch --relative=dh <base>..HEAD -o p/ -- dh ':!dh/coreSubProjects'
# in a clone of dh-fork:
git am p/*.patch && git push origin fix/<name>
```

Once upstream merges, the next `subtree pull` absorbs the fix and the local commit drops out.

## Licensing

The wrapper adds no code. `dh/` and `dh/coreSubProjects/` keep their upstream licenses (LGPL);
see the `LICENSE*` files inside those directories.

[dh]: https://gitlab.com/distant-horizons-team/distant-horizons
[core]: https://gitlab.com/jeseibel/distant-horizons-core
