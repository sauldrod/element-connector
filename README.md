element-connector
=================

[![Build Status](https://api.travis-ci.org/mkovatsc/element-connector.png?branch=master)](https://travis-ci.org/mkovatsc/element-connector)

Java socket abstraction for UDP, DTLS, TCP, etc.
It is used to modularize Californium and add DTLS support through the
standalone Scandium (Sc) project. Further projets can add so add different
transports independently (e.g., TCP, SMS, or special sockets when running in
an optimized VM such as Virtenio's PreonVM).
