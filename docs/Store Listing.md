# Kin+ Google Play Store Listing

Everything needed to fill in the Play Console store listing. Character limits are Google Play's.

## App details

| Field | Value |
| ----- | ----- |
| App name (30 max) | Kin+ Family Safety |
| Category | Lifestyle |
| Tags | Safety, Family, Location sharing |
| Contact email | st10439408@rcconnect.edu.za |
| Privacy policy | https://github.com/ST10439408-VRIKHOTSO/KinPlusApp/blob/master/docs/Privacy%20Policy.md |
| Price | Free, no ads, no in app purchases |
| Countries | South Africa |

## Short description (80 max)

Share location with family, send an SOS, and get home safe. In 3 SA languages.

## Full description (4000 max)

Kin+ keeps you connected to the people you trust and gets help to you quickly when something goes wrong. It was designed for South African families and communities, and works in English, isiZulu and Sepedi.

KIN+ CIRCLES
Create a circle for your family or friends and invite them with a code. Everyone in the circle can see each other on the map, and you choose what you share with each circle.

EMERGENCY SOS
Press and hold the SOS button for three seconds to alert all of your circles with your location. The hold stops accidental alerts, and you can cancel before it sends.

FAKE CALL, SIREN AND STROBE
Need a reason to walk away? Schedule a fake incoming call from any name you choose. Need attention? Sound a loud siren and flash the torch.

SAFE ZONES
Mark places like home, school or work. Your circle is told when you arrive or leave, so nobody has to ask "are you home yet?".

SAFE JOURNEY
Set a destination, an arrival time and who should watch over you. If you do not arrive in time, they are alerted. The "I'm OK" timer raises an SOS if you do not check in.

COMMUNITY ALERTS
See reports of crime, hazards, fires and medical emergencies near you, filtered by distance. Report what you see anonymously, and confirm other reports with "I see it too".

NEARBY HELP
One tap finds the closest police station, hospital, clinic or pharmacy.

WORKS OFFLINE
Your circles and Safe Zones are saved on your phone. Anything you do without signal is sent when you reconnect.

PRIVACY FIRST
Pause sharing for every circle with one switch, or one circle at a time. Your location is only shared with circles you join, and you can delete your account at any time.

Kin+ does not replace emergency services. In an emergency, call 10111 (police) or 112.

## Graphics

| Asset | Size | Status |
| ----- | ---- | ------ |
| App icon | 512 x 512 PNG | Done, `docs/store/Icon 512.png` |
| Feature graphic | 1024 x 500 PNG or JPG | Done, `docs/store/Feature Graphic 1024x500.png` |
| Phone screenshots | 2 to 8, in `docs/screenshots` | Done |

## Data safety answers

| Data type | Collected | Shared | Purpose | Optional |
| --------- | --------- | ------ | ------- | -------- |
| Precise location (incl. background) | Yes | No | App functionality | No |
| Name | Yes | No | Account management, app functionality | No |
| Email address | Yes | No | Account management | No |
| User generated content (community reports) | Yes | No | App functionality | Yes |
| Device or other IDs (FCM token) | Yes | No | App functionality (notifications) | No |
| App interactions (Firebase Analytics) | Yes | No | Analytics | No |

* Data is encrypted in transit: **Yes**
* Users can request deletion: **Yes**, from the Profile screen

## Background location declaration

Kin+ uses background location so that Safe Zones can notify a user's circle when they arrive at or leave a saved place, and so Safe Journey and the "I'm OK" timer can raise an alert while the app is closed. These are core safety features the user turns on themselves.

## Content rating

Kin+ shares the user's location with other users they choose and allows users to post community reports. Answer **Yes** to "shares location" and "users can interact or exchange content" in the questionnaire.
