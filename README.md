# Tindroid: Tinode Messaging Client for Android

Android instant messenger using [Tinode](https://github.com/tinode/chat/) backend. Beta-quality software.
Debug APKs are available in [releases](https://github.com/tinode/tindroid/releases).

<a href="https://play.google.com/store/apps/details?id=co.tinode.tindroidx"><img src="play-store.svg" height=36></a>

## Installing and running

This is NOT a standalone app, this is just a frontend, a client. It requires a [backend](https://github.com/tinode/chat/). See [installation instructions](https://github.com/tinode/chat/blob/master/INSTALL.md).

## Getting support

* Read [client-side](http://tinode.github.io/java-api/) and [server-side](https://github.com/tinode/chat/blob/master/docs/API.md) API documentation.
* For support, general questions, discussions post to [https://groups.google.com/d/forum/tinode](https://groups.google.com/d/forum/tinode).
* For bugs and feature requests [open an issue](https://github.com/tinode/tindroid/issues/new).
* Use https://tinode.co/contact for commercial inquiries.

## What works:

* One-on-one conversations and group chats.
* Channels with unlimited number of read-only subscribers.
* Unread message counters.
* Push notifications and in-app presence notifications.
* Message status notifications: message delivery to server; received and read notifications; typing notifications.
* Markdown-style formatting of text, e.g. \*styled\* &rarr; **styled**. Implemented as spannable.
* Replying and forwarding messages.
* Trusted account badges: verified account, staff, etc.
* Form messages suitable for chatbots.
* Attachments and inline images, voice messages.
* Muting/un-muting conversations and other granular permission management.
* Integration with Android's stock Contacts.
* Invite contacts to the app by SMS or email.
* Transport Level Security - https/wss.
* Offline mode.

## Not done yet:

* End-to-end encryption.
* No video messages. No video or audio calling.
* No support for switching between multiple backends.
* Mentions, hashtags.
* Previews not generated for videos, audio, links or docs.

## Dependencies

### SDK:

* [Jackson](https://github.com/FasterXML/jackson) for JSON serialization.
* [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket/) for websocket support.

SDK contains no Android-specific dependencies.

### Application

* [Google-Services](https://firebase.google.com/docs/cloud-messaging/android/client) for push notifications. See below.
* [Crashlytics](https://firebase.google.com/docs/android/setup) for crash reporting.
* [Picasso](https://square.github.io/picasso/) for image downloading and caching.
* [OSS Licenses Plugin](https://github.com/google/play-services-plugins/tree/master/oss-licenses-plugin) for legal compliance with OSS licenses.
* Standard Android dependencies.

## Other

Demo avatars and some other graphics are from https://www.pexels.com/ under [CC0 license](https://www.pexels.com/photo-license/).

Background patterns from http://subtlepatterns.com/, commercial and non-commercial use allowed with attribution.

The [`contacts.vcf`](./contacts.vcf) contains a list of contacts which can be used for testing. Push it to your emulator using command
```
adb push contacts.vcf /sdcard/contacts.vcf`
```

## Push notifications

If you want to use the app with your own server and want push notification to work you have to set them up:

* Register at https://firebase.google.com/, set up the project if you have not done so already.
* [Download your own](https://developers.google.com/android/guides/google-services-plugin) config file `google-services.json` and place it to the `app/` folder of your copy of Tindroid project. The config file contains keys specific to your Firebase/FCM registration.
* Copy Google-provided server key to `tinode.conf`, see details [here](https://github.com/tinode/chat/blob/master/docs/faq.md#q-how-to-setup-fcm-push-notifications).
* Recompile the app. If you add `google-services.json` incorrectly the app will compile but will later crash with non-obvious exceptions.

## Translations

The app is currently available in the following languages:
* English
* Chinese (simplified)
* Chinese (traditional)
* French
* German
* Korean
* Russian
* Spanish

More translations are welcome. See [instructions](https://github.com/tinode/chat/blob/devel/docs/translations.md#android).


## Screenshots
<img src="android-contacts.png" alt="App screenshot - chat list" width="250" /> <img src="android-chat.png" alt="App screenshot - chat" width="250" /> <img src="android-topic-info.png" alt="App screenshot - chat settings" width="250" />
<img src="android-account.png" alt="App screenshot - account info" width="250" /> <img src="android-find-people.png" alt="App screenshot - searching for contacts" width="250" /> <img src="android-forward.png" alt="App screenshot - forward message" width="250" />
