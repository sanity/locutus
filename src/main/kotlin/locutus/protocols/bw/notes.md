BW protocol is through limiting inbound message requests, no additional inbound accepted when over-bw.
Also expain to other peer so that they don't waste time sending messages that have to be rejected.

Need centralized "Peer selector", which will find an available peer based on whatever criteria - but limited
to those that aren't bandwidth limited.

Do we limit upload or download bw?  Upload because almost all traffic will be data requests? It's a maximum for up
and dl?