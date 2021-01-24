# NFC Pi Password Project

## Preamble

This project aims to create a hardware based password manager... at least as far as the system receiving the password is
concerned. The problem I'm facing is that my password manager of choice works well in the vast majority of circumstances
but I need it to work for me even in the outlying circumstances. I'm just annoyed is all, lol. My environment consists
of several different systems running Windows, Linux and MacOS and I'd like to have a solution that works with each one. 
In fact, several of my linux systems have encrypted drives and I'd like the password manager to handle that as well. The
only way I can envision this all working is to create a "keyboard" that types the password for me... So, that's what I
aim to create. 

## Hardware Components 

- Raspberry Pi 4
    - Built-In WiFi
- uFR Series Nano / Nano Online
    - USB NFC Reader
- USB 3.0 Hub
    - Must have external power

## Pi Software

- Latest version of Raspbian Lite (no Desktop)
- MongoDB
- Java 14


---

## Usage

### Requirements

#### Java 15 +
Developed using `Java.net 15.0.2-open` [SDKMAN](https://sdkman.io/)

#### Environment Variables
I always use [Splunk](https://hub.docker.com/r/splunk/splunk/) to keep track of my logs. It's free, easy to use, and if 
provided with properly formatted data, very easy to search with. If you have no desire to use Splunk for logging, please 
edit `log4j2-spring.xml` and remove the HTTP & Async Appenders. You might also want to change the logging back to 
standard format instead of logging in JSON... in that case, simply delete the `log4j2-spring.xml` file and use the 
default config which is very easy to read.

- **SPLUNK_HOST**: The ip/host of the Splunk server.
- **SPLUNK_PORT**: The port number used for the configured HTTP Event Collector
- **SPLUNK_TOKEN**: The token used for the configured HTTP Event Collector
