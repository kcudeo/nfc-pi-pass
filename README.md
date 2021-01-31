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

#### Getting the Raspberry Pi Prepped

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
- `sudo apt-get install zip git mongodb build-essential openjdk-11-jdk maven`
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

#### Enabling USB Gadget Mode

These are modified instructions from this [original post](http://www.isticktoit.net/?p=1383).

Start off by enabling the special device tree and loading the libcomposite kernel module.
- `echo "dtoverlay=dwc2" | sudo tee -a /boot/config.txt`
- `echo "dwc2" | sudo tee -a /etc/modules`
- `echo "libcomposite" | sudo tee -a /etc/modules`

Now, we are only concerned with the keyboard aspect of the functionality that is offered. This will create a system 
config script that runs on boot. 
- `sudo touch /usr/bin/pipass_keyboard`
- `sudo chmod +x /usr/bin/pipass_keyboard`
- `sudo nano /usr/bin/pipass_keyboard`

Enter the following in the file, save and close:
>```text
>#!/bin/bash
>cd /sys/kernel/config/usb_gadget/
>mkdir -p isticktoit
>cd isticktoit
>echo 0x1d6b > idVendor # Linux Foundation
>echo 0x0104 > idProduct # Multifunction Composite Gadget
>echo 0x0100 > bcdDevice # v1.0.0
>echo 0x0200 > bcdUSB # USB2
>mkdir -p strings/0x409
>echo "fedcba9876543210" > strings/0x409/serialnumber
>echo "Tobias Girstmair" > strings/0x409/manufacturer
>echo "iSticktoit.net USB Device" > strings/0x409/product
>mkdir -p configs/c.1/strings/0x409
>echo "Config 1: ECM network" > configs/c.1/strings/0x409/configuration
>echo 250 > configs/c.1/MaxPower
>
># Add functions here
>mkdir -p functions/hid.usb0
>echo 1 > functions/hid.usb0/protocol
>echo 1 > functions/hid.usb0/subclass
>echo 8 > functions/hid.usb0/report_length
>echo -ne \\x05\\x01\\x09\\x06\\xa1\\x01\\x05\\x07\\x19\\xe0\\x29\\xe7\\x15\\x00\\x25\\x01\\x75\\x01\\x95\\x08\\x81\\x02\\x95\\x01\\x75\\x08\\x81\\x03\\x95\\x05\\x75\\x01\\x05\\x08\\x19\\x01\\x29\\x05\\x91\\x02\\x95\\x01\\x75\\x03\\x91\\x03\\x95\\x06\\x75\\x08\\x15\\x00\\x25\\x65\\x05\\x07\\x19\\x00\\x29\\x65\\x81\\x00\\xc0 > functions/hid.usb0/report_desc
>ln -s functions/hid.usb0 configs/c.1/
># End functions
>
>ls /sys/class/udc > UDC
>```

Then to get things started at boot:
- `sudo nano /etc/rc.local`

Add the following before the `exit 0` line in that file:
> `/usr/bin/pipass_keyboard`

Finally, reboot to initialize.
- `sudo reboot`

#### Installing the Service

It is to be expected that some modifications will be made to the code in order to customize the experience. This is 
particularly true when it comes to logging. The file `src/main/resources/log4j2-spring.xml` should be updated with the
appenders that make sense for you. Feel free to fork the project and make modifications as you see fit. 

To get started, clone this repo to a location of your choosing.
- `mkdir ~/projects`
- `cd ~/projects`
- `git clone https://github.com/kcudeo/nfc-pi-pass.git`
- `cd nfc-pi-pass/`

If you're comfortable with Splunk, [have an instance installed](https://hub.docker.com/r/splunk/splunk/), and decide to take that route, then you'll need to set a
few environment variables. For this project I have setup a generic index with an `HTTP Event Collector` that expects a
`Source Type` of `Structured` -> `_json`. I have specifically disabled HTTPS in the settings for the event collector 
because all data flows through an [SSH tunnel](https://www.ssh.com/ssh/tunneling/example) to the remote Splunk server. 
- `nano ~/.bashrc`

Add the following to the very end of the file. Of course, your values will be different.
>```text
>export SPLUNK_HOST=localhost
>export SPLUNK_PORT=48088
>export SPLUNK_TOKEN=019f60ac-71e1-4871-950f-eb85a360a843
>```

After saving, be sure to source the changes to apply them.
- `source ~/.bashrc`

Now, run the application. Dependencies will download and the server should start.
- `mvn spring-boot:run`



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
