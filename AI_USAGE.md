# Declaration of AI Tool Usage

**Module:** OPSC6312 Open Source Coding (Intermediate)
**Group:** Vukosi Rikhotso (ST10439408) & Theo Golele (ST10439863)

This declaration explains how we used AI tools while building the Kin+ Portfolio of Evidence.

## Research and learning

We used AI in a similar way to Google Search, YouTube tutorials and official documentation. When we did not know how to do something, such as adding Google Maps, the Credential Manager Google sign in flow, WorkManager with a Hilt worker factory or MongoDB `2dsphere` queries, we asked for explanations and examples, then adapted them to our project and tested them ourselves. We also used AI to understand compiler errors.

## Part 3: moving from Jetpack Compose to XML views

For Part 3 we rebuilt the interface with XML layouts and Fragments. We used AI to help convert the Compose screens into XML layouts and Fragment code, to draft the isiZulu and Sepedi strings that were missing, and to explain build errors from GitHub Actions. We kept our existing ViewModels and data layer, and decided the screen structure and navigation ourselves. We reviewed every change, built and ran the app on a physical phone, and tested each screen and Google sign in before including it.

## What remained our own work

The **concept, features, use cases, requirements, original screen designs, API design and data model** are our own work, as defined in the Part 1 Research and Planning and Design documents. The **architecture decisions**, including the offline first repositories, the WorkManager sync queue with idempotency keys, Firebase authentication and server side token validation, were decided by the team.

## Verification

We did not include anything we did not understand. GitHub Actions builds the app and runs the unit tests on every push, which gives an independent check that the code compiles and the tests pass.


