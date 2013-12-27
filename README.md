RemoteSMS
=========
Remote SMS enables you to receive and send SMS via an Android device from any browser in the same local network. The Android device must be connected to a WiFi network which is the same local network the browsers are at. 

The workflow is:
The app launches, and create a web server and a websocket server on the Android device. The web server listens at port 8888 by default. When a browser hits the web server, an HTML document along with a js document is served. The js creates a websocket connecting to the websocket server, and all the communication afterwards is down via websocket.

This application uses undocumented feature in Android related to SMS queries. It uses a content provide content://sms which is undocumented. It is a proof-of-concept and sketchy prototype.
