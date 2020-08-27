A      B

A -> B : [rsa_encrypted_sym]-OpenConnection(yourKeyReceived = false)
repeat every 200ms until
B <- A : [rsa_encrypted_sym]-OpenConnection(yourKeyReceived = true)
