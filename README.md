# RestComm SMS Gateway (SMSC)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fsmscgateway.svg?type=shield)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fsmscgateway?ref=badge_shield)

 RestComm SMS Gateway (SMSC) to send/receive SMS from/to Mobile Operator Networks (GSM, SS7 MAP) , SMS aggregators (SMPP) and Internet Telephony Service Providers (SIP, SMPP). 

## Introduction 

RestComm SMS Gateway (SMSC) is built on [RestComm SS7](https://github.com/RestComm/jss7), SMPP and RestComm JSLEE Server.

When a user sends a text message (SMS message) to another user, the message gets stored in the SMSC (short message service center) which delivers it to the destination user when they are available. This is a store and forward option.

An SMS center (SMSC) is responsible for handling the SMS operations of a wireless network.

When an SMS message is sent from a mobile phone, it will reach an SMS center first.

2) The SMS center then forwards the SMS message towards the destination.

3) The main duty of an SMSC is to route SMS messages and regulate the process. If the recipient is unavailable (for example, when the mobile phone is switched off), the SMSC will store the SMS message.

4) It will forward the SMS message when the recipient is available.

## Testing
Configuration for SMSC GW with RestComm jSS7 is [here](https://github.com/RestComm/smscgateway/wiki/Testing-SMSC-GW-with-jSS7-Simulator) 

## License

RestComm SMSC is licensed under dual license policy. The default license is the Free Open Source [GNU Affero GPL v3.0](http://www.gnu.org/licenses/agpl-3.0.html). Alternatively a commercial license can be obtained from Telestax ([contact form](https://www.restcomm.com/contact/))

RestComm SMSC Gateway is lead by [TeleStax, Inc.](www.telestax.com) and developed collaboratively by a [community of individual and enterprise contributors](https://www.restcomm.com/acknowledgements/).



[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fsmscgateway.svg?type=large)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fsmscgateway?ref=badge_large)

## Downloads

Download your binaries from [the Restcomm site Downloads section](https://www.restcomm.com/downloads/).

## Maven Repository

Artifacts are available at [Sonatype Maven Repo](https://oss.sonatype.org/content/repositories/releases/org/mobicents) which are also synched to central

## Build From Source

SMSC GW refers now to cloud hopper SMPP project version (with support for submit_multi operation) that is not yet merged to https://github.com/fizzed/cloudhopper-smpp repo.
Therefore if you need to compile SMSC GW locally, you need to compile the updated version of cloud hopper SMPP from repo https://github.com/RestComm/cloudhopper-smpp, branch "master\_submit\_multi-2" previously.

You can use these commands:
```
git clone https://github.com/RestComm/cloudhopper-smpp
git checkout master_submit_multi-2
mvn clean install
```
Then you can download and compile SMSC GW code locally.

SMSC GW contains branches with code versions:
* master   - the last actual code (support for smpp, ss7 and sip connectors and diameter OCS server)
* legacy-2 - code with support only for smpp and sip
* legacy   - very old poor code version

## Wiki

Read our [RestComm SMSC wiki](https://github.com/RestComm/smscgateway/wiki) 

## All Open Source RestComm Projects

Open Source https://restcomm.com/open-source/


## Acknowledgements

* [![JProfiler](https://www.ej-technologies.com/images/product_banners/jprofiler_large.png)](https://www.ej-technologies.com/products/jprofiler/overview.html) JProfiler Open Source License

---
We also maintain a list of [all Restcomm contributors](http://www.telestax.com/opensource/acknowledgments/) on the Restcomm website, to acknowledge contributions by the broad open source community.
