### Business model

* Coinbase + Patreon + decentralized twitter

Pay to permit inbound messages, effectively sender provides a deposit, could be done centrally - donation would go to
project, receiver, or other charity, at receiver's option.  Sender can opt to make it unilateral, or at receiver's option.

Signals/Messages/Posts/Contracts can specify requirements about new posts including an accurate summary of previous posts,
so the summary will remain available and should be accurate even as old posts fall out.

Example would be the balance on an account, it would be required that it be the previous balance +/- the transaction amount.
Other examples could include summaries of WoT relationships.

Trust can be parameterised, different types of trust - eg. "is_spammer" might be separate from "recommended", etc.

WoT, use PAV to correlate user preferences with recommendations from WoT, which can then be used to weight them accordingly.
The PAVs could be shared as part of an endorsement.

Question: Do we want to allow inverse correlations between edges in the WoT?  Could have implications for algorithm.

WoT can be used to identify messages that should be retransmitted for the firehose, hopefully resulting in a meritocratic "firehose"/
zeitgeist.

Anyone can create an "account", but only locutus-signed accounts come with a starting balance with a guaranteed repayment
of the balance at a time of your choosing, this valid transactions may only occur through locutus-signed accounts.

Accounts could optionally be "recordless", maintaining only a *validated* summary of the balance - but would

Threat:
Network could be disconnected, resulting in possible double-spend.
FairShare could be required to validate each transaction in realtime


* Trust as profit model, use FairShare idea to reward early "trusters".
* Trust can be labeled with arbitrary data, but with recommended labels
  * Recommended labels to use can themselves be based on trust
* Contracts are specified as content restricted HTML, but with custom
  additions to do things like:
    * Specify that a <textarea data-locutus-pubkey> contains a public key
    * For a textarea containing a public key, specify what to do with it, eg. encrypt
    * For a button specify whether to increase or decrease trust and the type of trust
    
