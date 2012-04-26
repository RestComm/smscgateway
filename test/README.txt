//Mobicents SMSC Server

sctp association create <assoc-name> <CLIENT | SERVER> <server-name> <peer-ip> <peer-port> <host-ip> <host-port>
sctp association create SCTPAssoc1 CLIENT 127.0.0.1 2775 127.0.0.1 2776

m3ua as create <as-name> <AS | SGW | IPSP> mode <SE | DE> ipspType <client | server> rc <routing-context> traffic-mode <traffic mode> 
m3ua as create AS1 IPSP mode SE ipspType client rc 100

m3ua asp create ASP1 SCTPAssoc1

m3ua as add AS1 ASP1

m3ua route add AS1 2 -1 -1

//Rule for incoming SCCP message
sccp primary_add create <id> <address-indicator> <point-code> <subsystem-number> <translation-type> <numbering-plan>  
<nature-of-address-indicator> <digits>

sccp primary_add create 1 19 1 8 0 1 4 923330053058 

sccp rule create <id> <mask> <address-indicator> <point-code> <subsystem-number> <translation-type> <numbering-plan>  
<nature-of-address-indicator> <digits> <ruleType> <primary-address-id> <backup-address-id> <loadsharing-algorithm>

sccp rule create 1 K 18 0 8 0 1 4 923330053058 solitary 1 

//Rule for all out going
sccp primary_add create 2 19 2 0 0 1 4 - 
sccp rule create 2 K 18 0 0 0 1 4 * solitary 2 

//sccp rsp create <id> <remote-spc> <rspc-flag> <mask> 
sccp rsp create 1 2 0 0

//sccp rss create <id> <remote-spc> <remote-ssn> <rss-flag> <mark-prohibited-when-spc-resuming>
sccp rss create 1 2 8 0
sccp rss create 2 2 6 0


sccp sap create <id> <mtp3-id> <opc> <ni>
sccp sap create 1 1 1 2


sccp dest create <sap-id> <id> <first-dpc> <last-dpc> <first-sls> <last-sls> <sls-mask>
sccp dest create 1 1 2 2 0 255 0 255 255

m3ua asp start ASP1

//Test SMSC
sctp server create SCTPServer1 127.0.0.1 2775
sctp server start SCTPServer1
sctp association create SCTPAssoc1 SERVER SCTPServer1 127.0.0.1 2776

m3ua as create AS1 IPSP mode SE ipspType server rc 100

m3ua asp create ASP1 SCTPAssoc1

m3ua as add AS1 ASP1

m3ua route add AS1 1 -1 -1

m3ua asp start

m3ua asp start ASP1

//Rule for outgoing SCCP message
sccp primary_add create 1 19 1 8 0 1 4 923330053058
sccp rule create 1 K 18 0 8 0 1 4 923330053058 solitary 1 

//Rule for all incoming
sccp primary_add create 2 19 2 0 0 1 4 - 
sccp rule create 2 K 18 0 0 0 1 4 * solitary 2

sccp rsp create 1 1 0 0

sccp rss create 1 1 8 0

sccp sap create 1 1 2 2

sccp dest create 1 1 1 1 0 255 255
