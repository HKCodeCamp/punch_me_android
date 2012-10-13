Punch Me Android Client
=======================

It uses android linear acceleration to determine which direction is the phone being swing.

Socket is used to connect to the server.
It will send a text to the server to indicate a punch.

##Message format

	PUNCH [DIRECTION] [FORCE]

|DIRECTION|
|---------|
|LEFT|
|RIGHT|
|UP|
|DOWN|


|FORCE|
|-----|
|0-10|

