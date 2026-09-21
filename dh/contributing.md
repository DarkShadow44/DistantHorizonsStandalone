## Contributing

Thanks for your interest in contributing to Distant Horizons!

Check out the [Core Wiki](https://gitlab.com/jeseibel/distant-horizons-core/-/wikis/home) for a rough overview of Distant Horizon's project structure.

## Translations

See [translations.md](translations.md) for the Crowdin workflow.

## Submitting a merge request

We love merge requests from everyone.

By sending a merge request, you agree to abide by the Distant Horizons [Contributor Code of Conduct](code_of_conduct.md). \
Contributions to this project are under the [lesser GPL v3 license](LICENSE.txt) Copyright James Seibel, so please include the [license header](license_header.txt) at the top of any new code files.

1. Fork, then clone the repo: \
`git clone --recurse-submodules https://gitlab.com/jeseibel/distant-horizons.git`

2. Set up your dev environment: \
`./gradlew build`

3. (Optional) Confirm the tests pass: \
`./gradlew test`

4. (Optional) Confirm the game runs with either Forge or Fabric: \
`./gradlew forge:runClient` \
`./gradlew fabric:runClient`

5. Make your change(s). 
6. Add tests (if appropriate). 

7. Confirm the tests pass \
`./gradlew test`

8. Confirm the game runs with both Forge **and** Fabric: \
`./gradlew forge:runClient` \
`./gradlew fabric:runClient` \
When running the game, load or generate a world to confirm Distant Horizons initializes correctly.

9. Push to your fork, make sure to include the Core submodule, and submit a [new merge request](https://gitlab.com/jeseibel/distant-horizons/-/merge_requests/new).



## General Guidelines

* Check the existing issue list to verify that a given [bug](https://gitlab.com/jeseibel/distant-horizons/-/issues/?sort=created_date&state=opened&label_name%5B%5D=Bug&first_page_size=100), [feature](https://gitlab.com/jeseibel/distant-horizons/-/issues/?sort=created_date&state=opened&label_name%5B%5D=Feature&first_page_size=100), or [improvement](https://gitlab.com/jeseibel/distant-horizons/-/issues/?sort=created_date&state=opened&label_name%5B%5D=Improvement&first_page_size=100) hasn't already been submitted.
* Please open an issue if things aren't working as expected.
* Open a merge request to: fix bugs, fix documentations, improve an existing system, or complete a feature.
* When contributing:
  * Put any Minecraft independent code in the [Core](https://gitlab.com/jeseibel/distant-horizons-core) repo when possible.
  * Comment and format your code so other people can easily understand it.

