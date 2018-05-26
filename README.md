


[Try Restcomm Cloud NOW for FREE!](https://www.restcomm.com/sign-up/) Zero download and install required.


All Restcomm [docs](https://www.restcomm.com/docs/) and [downloads](https://www.restcomm.com/downloads/) are now available at [Restcomm.com](https://www.restcomm.com).




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
