# Assimilation

## Participants

* Joiner - A new peer wishing to join the peer ring
* Gateway - An open peer through which the joiner wishes to join
* Seed - 

`Joiner -> AssimilationRequest -> Gateway`

`Gateway -> AssimilationResponse -> Joiner`

`Gateway -> Sponsor -> AssimilationRequest`

`Seed 1 -> Gateway -> AssimilationAccept(salt1)`

`Gateway -> Joiner -> AssimilationAccept(salt1)`

`Joiner -> Gateway -> AssimilationAuth(hash(salt1, salt2, ...), location)`

Gateway verifies that location is indeed a hash of the salts, to inhibit Joiner picking their own location


