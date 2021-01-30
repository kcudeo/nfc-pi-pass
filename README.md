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
- Java 11

---
## Project Setup and Installation

The assumption being made in this document is that you're using Windows as your desktop os. If this is not the case,
simply adjust the steps for your operating system.

### Inventory:
- uFR Nano / uFR Nano Online
- Raspberry Pi 4 + MicroSD Card
- Network attached handheld device like a tablet.

### Setup

#### Getting the Raspberry Pi Setup

This step assumes that you are starting from scratch with an empty MicroSD card. If you already have your Pi setup, you
can skip this step.

Download and install [Raspberry Pi Imager](https://www.raspberrypi.org/software/). Use the application to install
Raspbian OS Lite with No Desktop. `Operating System Button` -> `Raspberry Pi OS (other)` -> `Raspberry Pi OS Lite (32 bit)`.

Once the image is written and verified, open the drive labeled `boot` and add the file named `ssh` to the root directory. 
For me, the drive that is labeled `boot` is drive `K`. In PowerShell, I would enter the following commands:
- `k:`
- `echo $null >> ssh`

If you have WiFi and plan to have the Pi connect to the network that way, follow [these instructions](https://www.raspberrypi.org/documentation/configuration/wireless/headless.md)
to get that setup. Then, properly eject the MicroSD card and put it in the Pi and power on.

Once the Pi has had a chance to boot, take a look at the connected devices list on your router admin page to find its IP
address. Once you have that, modify the next command with your Pi's IP address.
- `ssh pi@10.1.3.88` ... _password is `raspberry`_

Once connected, configure your Pi as needed.
- `sudo raspi-config`

Update and Upgrade all default packages. Then install some needed dependencies.
- `sudo apt-get update`
- `sudo apt-get upgrade`
- `sudo apt-get install zip git mongodb build-essential openjdk-11-jdk`
- `sudo reboot`

#### Installing the Drivers

Usually in the beginning a reboot is needed due to firmware upgrades. Connect again via ssh and get your system type.
- `uname -a`

The output of my Pi provides the following and yours will likely be similar.
> ```
> Linux pi-pass 5.4.83-v7l+ #1379 SMP Mon Dec 14 13:11:54 GMT 2020 armv7l GNU/Linux
> ```

Now, download the appropriate [FTDI D2XX Drivers](https://ftdichip.com/drivers/d2xx-drivers/) for your OS. Since mine has
`armv7l`, I'll download the [1.4.22 ARMv7 hard-float ***](https://www.ftdichip.com/Drivers/D2XX/Linux/libftd2xx-arm-v7-hf-1.4.22.gz)
package using the following command.
- `wget https://www.ftdichip.com/Drivers/D2XX/Linux/libftd2xx-arm-v7-hf-1.4.22.gz`
- `tar xfvz libftd2xx-arm-v7-hf-1.4.22.gz`

Please read through the manufacturer instructions for the driver installation. It may be different from what I have here.
However, here's what I did to install the drivers.

- `cd release/build/`
- `sudo cp libftd2xx.* /usr/local/lib`
- `sudo chmod 0755 /usr/local/lib/libftd2xx.so.1.4.22`
- `sudo ln -sf /usr/local/lib/libftd2xx.so.1.4.22 /usr/local/lib/libftd2xx.so`

Now, you need to prevent a certain number of kernel modules from loading at startup that conflict with the new drivers 
you just installed.
- `sudo nano /etc/modprobe.d/blacklist-ftdi-defaults.conf`

Then add the following to the file:
>```text
>blacklist ftdi_sio
>blacklist usbserial
>```

Be sure to save the file and then reboot.
- `sudo reboot`

Now, let's validate our install and confirm that the drivers are correctly setup. SSH back into your Pi and go back to the
driver's example directory.
- `cd release/examples/`
- `make -B`
- `cd EEPROM/read/`

At this point, your `uFR Nano or uFR Nano Online` should be connected to the Pi by USB. Once connected issue the command
to check if the device responds correctly.
- `sudo ./read`

The output of this command should look something similar to the follow:

>```text
>Library version = 0x10422
>Opening port 0
>FT_Open succeeded.  Handle is 0xc48860
>FT_GetDeviceInfo succeeded.  Device is type 5.
>FT_EE_Read succeeded.
>
>Signature1 = 0
>Signature2 = -1
>Version = 2
>VendorId = 0x0403
>ProductId = 0x6001
>Manufacturer = D-Logic
>ManufacturerId = A6
>Description = uFR Nano Plus
>SerialNumber = XXXXXXXX
>MaxPower = 100
>PnP = 1
>SelfPowered = 0
>RemoteWakeup = 1
>232R:
>-----
>        UseExtOsc = 0x0
>        HighDriveIOs = 0x0
>        EndpointSize = 0x40
>        PullDownEnableR = 0x0
>        SerNumEnableR = 0x1
>        InvertTXD = 0x0
>        InvertRXD = 0x0
>        InvertRTS = 0x1
>        InvertCTS = 0x0
>        InvertDTR = 0x0
>        InvertDSR = 0x0
>        InvertDCD = 0x0
>        InvertRI = 0x0
>        Cbus0 = 0x8
>        Cbus1 = 0x2
>        Cbus2 = 0x0
>        Cbus3 = 0x1
>        Cbus4 = 0x5
>        RIsD2XX = 0x1
>Returning 0
>```

If you don't get a success message, go back through the steps and see what went wrong.

#### Installing the Service


---

## Usage

### Requirements

#### Java 11
Developed using `Java.net 11.0.2-open` [SDKMAN](https://sdkman.io/)

#### Environment Variables
I always use [Splunk](https://hub.docker.com/r/splunk/splunk/) to keep track of my logs. It's free, easy to use, and if 
provided with properly formatted data, very easy to search with. If you have no desire to use Splunk for logging, please 
edit `log4j2-spring.xml` and remove the HTTP & Async Appenders. You might also want to change the logging back to 
standard format instead of logging in JSON... in that case, simply delete the `log4j2-spring.xml` file and use the 
default config which is very easy to read.

- **SPLUNK_HOST**: The ip/host of the Splunk server.
- **SPLUNK_PORT**: The port number used for the configured HTTP Event Collector
- **SPLUNK_TOKEN**: The token used for the configured HTTP Event Collector
