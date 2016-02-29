# RestComm SMSC Gateway
 RestComm SMSC Gateway to send/receive SMS from/to Mobile Operator Networks (GSM, SS7 MAP) , SMS aggregators (SMPP) and Internet Telephony Service Providers (SIP, SMPP). 

## Introduction 

RestComm SMSC Gateway is built on [RestComm SS7](https://github.com/RestComm/jss7) and RestComm JSLEE Server

When a user sends a text message (SMS message) to another user, the message gets stored in the SMSC (short message service center) which delivers it to the destination user when they are available. This is a store and forward option.

An SMS center (SMSC) is responsible for handling the SMS operations of a wireless network.

When an SMS message is sent from a mobile phone, it will reach an SMS center first.

2) The SMS center then forwards the SMS message towards the destination.

3) The main duty of an SMSC is to route SMS messages and regulate the process. If the recipient is unavailable (for example, when the mobile phone is switched off), the SMSC will store the SMS message.

4) It will forward the SMS message when the recipient is available.

## Testing
Configuration for SMSC GW with RestComm jSS7 is [here](https://github.com/RestComm/smscgateway/wiki/Testing-SMSC-GW-with-jSS7-Simulator) 

## License

RestComm SMSC is licensed under dual license policy. The default license is the Free Open Source [GNU Affero GPL v3.0](http://www.gnu.org/licenses/agpl-3.0.html). Alternatively a commercial license can be obtained from Telestax ([contact form](http://www.telestax.com/contactus/#InquiryForm))

RestComm SMSC Gateway is lead by [TeleStax, Inc.](www.telestax.com) and developed collaboratively by a [community of individual and enterprise contributors](http://www.telestax.com/open-source-2/acknowledgments/).


## Downloads

Download binary from [here](https://github.com/RestComm/smscgateway/releases) or Continuous Delivery builds from [CloudBees](https://mobicents.ci.cloudbees.com/job/Restcomm-SMSC/)

## Maven Repository

Artifacts are available at [Sonatype Maven Repo](https://oss.sonatype.org/content/repositories/releases/org/mobicents) which are also synched to central

## Build From Source

SMSC GW refers now for cloud hopper SMPP project version (with support for submit_multi operation) that is not yet merged to https://github.com/fizzed/cloudhopper-smpp repo.
Because of it if you need to compile SMSC GW locally, you previously need to compile the updated version of cloud hopper SMPP from repo https://github.com/RestComm/cloudhopper-smpp, branch "master\_submit\_multi-2".

You can use these commands:

```
git clone https://github.com/RestComm/cloudhopper-smpp
git checkout master\_submit\_multi-2
mvn clean install
```

## Wiki

Read our [RestComm SMSC wiki](https://github.com/RestComm/smscgateway/wiki) 

## All Open Source RestComm Projects

Open Source http://telestax.com/open-source/
