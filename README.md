# NeterraProxy

![NeterraProxy](https://raw.githubusercontent.com/sgloutnikov/NeterraProxy/master/app/src/main/res/mipmap-xxhdpi/ic_launcher.png)

## What?
NeterraProxy is an on-demand m3u8 playlist/playback daemon for Neterra.tv, running on Android. (4.0+).

## Why?
Have the freedom to watch on your desired IPTV player or TV (Perfect Player, Kodi IPTV Simple, Android Live Channels, GSE Smart IPTV Player, etc). Play links issued by Neterra expire after 12 hours to prevent abuse. Traditional playlist generators need to be run again in order to generate new links. This is not the case for NeterraProxy.

## How?
NeterraProxy generates a specialized playlist that points to itself rather than Neterra. When NeterraProxy receives a playback request it determines the context of the request and responds with a 301 redirect to a valid corresponding Neterra play link. It automatically re-authenticates if the session has expired.

---
## Instructions
1) Download the latest apk from the [releases](https://github.com/sgloutnikov/NeterraProxy/releases) section.
2) Install/Side Load and launch NeterraProxy on your Android device. 
3) Enter your Neterra.tv **Username** and **Password** and press **Save**.
4) Back out of NeterraProxy (or press Home) to leave it running. **Exit** will terminate NeterraProxy.
5) Use the following URL to connect NeterraProxy with your favorite IPTV player:
    * Playlist URL: http://localhost:8889/playlist.m3u8
    * EPG URL: http://localhost:8889/epg.xml
---
## Perfect Player Example + Settings
![PerfectPlayer1](https://raw.githubusercontent.com/sgloutnikov/NeterraProxy/master/screenshots/pp1.png)

* Playlist:

![PerfectPlayer2](https://raw.githubusercontent.com/sgloutnikov/NeterraProxy/master/screenshots/pp2.png)

* EPG:

![PerfectPlayer3](https://raw.githubusercontent.com/sgloutnikov/NeterraProxy/master/screenshots/pp3.png)

* Decoder:

![PerfectPlayer4](https://raw.githubusercontent.com/sgloutnikov/NeterraProxy/master/screenshots/pp4.png)

---
## Acknowledgments
* SmoothProxy
* KodiBG.org for providing the EPG source
